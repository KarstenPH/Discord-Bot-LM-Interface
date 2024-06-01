package org.bot

import dev.kord.core.entity.Message
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import java.util.concurrent.TimeUnit

class LLMManager {
    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.DAYS)
        .writeTimeout(1, TimeUnit.DAYS)
        .readTimeout(1, TimeUnit.DAYS)
        .callTimeout(1, TimeUnit.DAYS)
        .build()
    private val llmConfig = Json.decodeFromString<JsonObject>(File("./src/config.json").readText())
    private val prompt = File("./src/SystemPrompt.LLMD").readText() + "\n" + File("./src/Character/CharacterInfo.LLMD").readText()

    val charName = File("./src/Character/CharacterName.LLMD").readText()
    val greeting = File("./src/Character/Greeting.LLMD").readText()

    suspend fun onCommand(message: Message, messageContents: List<String>) {
        if (blockList.contains<Any?>(Json.encodeToJsonElement(message.author?.id.toString()))) {
            println("Blocked user ${message.author!!.username} tried to talk to the bot")
            message.channel.createMessage("You are blocked from using that")
            return
        }
        val userMessage = if (messageContents.size >= 2) { messageContents.toMutableList().drop(1).joinToString(" ") } else { "" }
        println("${message.author!!.username}: \"$userMessage\"")
        val botResponse = try {generation(userMessage, message.author!!.username, message)} catch (e: IOException) {e.toString()}
        println("$charName: $botResponse")
        reply(message, botResponse)
    }

    suspend fun onPing(message: Message) {
        if (blockList.contains<Any?>(Json.encodeToJsonElement(message.author?.id.toString()))) {
            println("Blocked user ${message.author!!.username} tried to talk to the bot")
            message.channel.createMessage("You are blocked from using that")
            return
        }
        println("${message.author!!.username}: \"${message.content.removePrefix("<@${kord?.selfId}>").trim()}\"")
        val botResponse = try {generation(message.content.removePrefix("<@${kord?.selfId}>").trim(), message.author!!.username, message)} catch (e: IOException) {e.toString()}
        println("$charName: $botResponse")
        reply(message, botResponse)
    }

    suspend fun continueCmd(message: Message) {
        if (blockList.contains<Any?>(Json.encodeToJsonElement(message.author?.id.toString()))) {
            println("Blocked user ${message.author!!.username} tried to continue generation")
            message.channel.createMessage("You are blocked from using that")
            return
        }
        println("${message.author!!.username} continued the generation")
        val botResponse = LLM.generationContinue(message)
        println("$charName: $botResponse")
        reply(message, botResponse)
    }

    private suspend fun generation(userMessage: String, user: String, message: Message): String {
        val ctxTruncation = dotenv["CTX_TRUNCATION"].toIntOrNull() ?: -1
        val truncationLength = dotenv["TRUNCATION_LENGTH"].toIntOrNull() ?: -1
        val maxStreak = dotenv["MAX_NEWLINE_STREAK"].toIntOrNull() ?: -1
        val doStreak = maxStreak >= 0
        var chatLog = readChatLog(message)
         val rawResponse = sendLLMRequest("${if (ctxTruncation < 0) {chatLog} else {if (chatLog.length < ctxTruncation) {chatLog} else {chatLog.drop(chatLog.length-ctxTruncation)} } }\n$user: $userMessage\n$charName:", user, message)
            var unfilteredResponse = ""
            var streakLength = 0
            if (truncationLength < 0) {
                unfilteredResponse = rawResponse
            } else {
                var newlines = 0
                for (i in rawResponse) {
                    if (i.toString() == " ") {
                        streakLength++
                    } else if (i.toString() == "\n") {
                        newlines++
                        streakLength++
                        if (newlines > truncationLength) {
                            println("Truncated reply to $truncationLength newlines")
                            break
                        }
                        unfilteredResponse += " "
                    } else {
                        streakLength = 0
                    }
                    if (streakLength > maxStreak && doStreak) {
                        println("Maximum streak length of $maxStreak reached, reply cut off")
                        break
                    }
                    unfilteredResponse += i.toString()
                }
            }
            if (unfilteredResponse == "") unfilteredResponse = "..."
            val botResponse = if (!strictFiltering) {
                filter.filter(unfilteredResponse)
            } else {
                filter.filterStrict(unfilteredResponse)
            }
            chatLog = "$chatLog\n$user: $userMessage\n$charName: $botResponse".trim()
            File("src/Logs/Logs${message.channel.id}/characterLogs.LLMD").printWriter().use {
                it.print(chatLog)
            }
            return botResponse
    }
    private fun readChatLog(message: Message): String {
        if (!File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").exists()) {
            println("Creating log file for channel ${message.channel.id}")
            if (!File("./src/Logs/Logs${message.channel.id}").exists()) {
                File("./src/Logs/Logs${message.channel.id}").mkdir()
                println("Log directory for the channel was not found, so it was created automatically")
            }
            File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").createNewFile()
            File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").printWriter().use {
                it.print("$charName: $greeting".trim())
            }
        }
        val logPath = "./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD"
        return File(logPath).readText()
    }

    private suspend fun sendLLMRequest(input: String, user: String, message: Message): String {
        try {
            return runBlocking {
                    val typing = launch(Dispatchers.Default) {
                        while (true) {
                            message.channel.type()
                            delay(1000L)
                        }
                    }
                val response = async {
                    val userStop = Json.decodeFromString<JsonArray>(File("./src/Stop.json").readText())
                    val usernames =
                        Json.decodeFromString<JsonArray>(File("./src/Usernames.json").readText()).toMutableList()
                    if (!usernames.contains<Any?>(Json.encodeToJsonElement("$user:"))) {
                        println("added $user: as a stop token, because $usernames did not contain it")
                        usernames.add(Json.encodeToJsonElement("$user:"))
                        File("src/Usernames.json").printWriter().use {
                            it.print(Json.encodeToString(usernames).trim())
                        }
                    }
                    val stop = buildJsonArray {
                        for (i in userStop) {
                            add(i.jsonPrimitive.content)
                        }
                        for (i in usernames) {
                            add(i.jsonPrimitive.content)
                        }
                        add("\n\n\n")
                        add("$charName:")
                        add("${message.author!!.username}:")
                    }
                    val llmRequest = buildJsonObject {
                        for (i in llmConfig) {
                            put(i.key, i.value)
                        }
                        put("prompt", input)
                        put("character", prompt)
                        put("stop", stop)
                    }
                    val requestBody = llmRequest.toString().toRequestBody()
                    val request =
                        Request.Builder().url(llmUrl!!).header("Content-Type", "application/json").post(requestBody)
                            .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val responseJson = Json.decodeFromString<JsonObject>(response.body!!.string())
                        return@async responseJson.jsonObject["choices"]!!.jsonArray[0].jsonObject["text"]!!.jsonPrimitive.content.trim()
                    }
                }.await()
                typing.cancelAndJoin()
                return@runBlocking response
            }
        } catch(e: Exception) {
            println(e.toString())
            for (i in e.stackTrace) println(i.toString())
            return ""
        }
    }

    private suspend fun generationContinue(message: Message): String {
        val ctxTruncation = dotenv["CTX_TRUNCATION"].toIntOrNull() ?: -1
        val truncationLength = dotenv["TRUNCATION_LENGTH"].toIntOrNull() ?: -1
        val maxStreak = dotenv["MAX_NEWLINE_STREAK"].toIntOrNull() ?: -1
        val doStreak = maxStreak >= 0
        var chatLog = readChatLog(message)
        val rawResponse = sendLLMRequest(if (ctxTruncation < 0) {chatLog} else {if (chatLog.length < ctxTruncation) {chatLog} else {chatLog.drop(chatLog.length-ctxTruncation)} }, message.author!!.username, message)
        var unfilteredResponse = ""
        var streakLength = 0
        if (truncationLength < 0) {
            unfilteredResponse = rawResponse
        } else {
            var newlines = 0
            for (i in rawResponse) {
                if (i.toString() == " ") {
                    streakLength++
                } else if (i.toString() == "\n") {
                    newlines++
                    streakLength++
                    if (newlines > truncationLength) {
                        println("Truncated reply to $truncationLength newlines")
                        break
                    }
                    unfilteredResponse += " "
                } else {
                    streakLength = 0
                }
                if (streakLength > maxStreak && doStreak) {
                    println("Maximum streak length of $maxStreak reached, reply cut off")
                    break
                }
                unfilteredResponse += i.toString()
            }
        }
        if (unfilteredResponse == "") unfilteredResponse = "..."
        val botResponse = if (!strictFiltering) {
            filter.filter(unfilteredResponse)
        } else {
            filter.filterStrict(unfilteredResponse)
        }
        chatLog = "$chatLog$botResponse".trim()
        File("src/Logs/Logs${message.channel.id}/characterLogs.LLMD").printWriter().use {
            it.print(chatLog)
        }
        return botResponse
    }
}