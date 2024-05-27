package org.bot

import dev.kord.common.entity.*
import dev.kord.core.*
import dev.kord.core.behavior.reply
import dev.kord.core.entity.*
import dev.kord.core.event.message.*
import dev.kord.gateway.*
import java.io.*
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.*
import java.util.concurrent.*

val dotenv = dotenv()
val botToken: String? = dotenv["TOKEN"]
val owners = Json.decodeFromString<JsonArray>(dotenv["OWNERS"])
val llmUrl: String? = dotenv["LLMURL"]
var kord: Kord? = null
val client = OkHttpClient.Builder().connectTimeout(1, TimeUnit.DAYS).writeTimeout(1, TimeUnit.DAYS).readTimeout(1, TimeUnit.DAYS).callTimeout(1,TimeUnit.DAYS).build()
val llmConfig = Json.decodeFromString<JsonObject>(File("./src/config.json").readText())
val prompt = File("./src/SystemPrompt.LLMD").readText() + "\n" + File("./src/Character/CharacterInfo.LLMD").readText()
val charName = File("./src/Character/CharacterName.LLMD").readText()
val greeting = File("./src/Character/Greeting.LLMD").readText()
var blockList = Json.decodeFromString<JsonArray>("[]")
val commandManager = CommandManager()
val LLM = LLMManager()
var ignoreNext = false
val filter = FilterManager()
val strictFiltering = try { dotenv["STRICT_FILTERING"].toBooleanStrictOrNull()!! } catch (e: NullPointerException) { throw Exception("NonBooleanStrictFilteringException") }
val allowDMs = dotenv["ALLOW_DMS"].toBooleanStrictOrNull() ?: false
val botStatus = getStatus()
var loginAgain = true
var restartBot = false
const val botVersion = "Discord bot LMI by Superbox\nV1.0.1\n"

suspend fun main() {
    println("Starting $botVersion")
    if (dotenv["TRUNCATION_LENGTH"].toIntOrNull() == null) throw Exception("InvalidTruncationLengthException")
    if (llmUrl == null || llmUrl == "") throw Exception("NoLLLMURLException")
    if (owners.isEmpty()) throw Exception("NoOwnersException")
    println("Owners: $owners")
    println("LLMUrl: $llmUrl")
    if (!File("./src/Logs").exists()) File("./src/Logs").mkdir()
    if (File("./src/Blocklist.json").exists()) {
        blockList = Json.decodeFromString<JsonArray>(File("./src/Blocklist.json").readText())
    } else {
        runBlocking {
            File("./src/Blocklist.json").createNewFile()
            File("./src/Blocklist.json").printWriter().use {
                it.print("[]")
            }
            println("Created new blocklist file")
        }
    }
    if (!File("./src/Usernames.json").exists()) {
        runBlocking {
            File("./src/Usernames.json").createNewFile()
            File("./src/Usernames.json").printWriter().use {
                it.print("[]")
            }
            println("Created new usernames file")
        }
    }
    println("Blocked users: $blockList")
    filter.buildFilterMap()
    if (botToken == null || botToken == "") {
        throw Exception("NoBotTokenException")
    }
    kord = Kord(botToken)
    kord!!.on<ReactionAddEvent> {
        if (messageAuthorId.toString() == kord.selfId.toString()) {
            if (emoji == ReactionEmoji.Unicode("❌")) {
                for (i in message.getReactors(emoji).toList()) {
                    var moderator = false
                    for (k in i.asMember(guildId!!).roleBehaviors) {
                        if (k.asRole().permissions.contains(Permission.ModerateMembers)) moderator = true
                    }
                    if (owners.contains<Any?>(Json.encodeToJsonElement(i.id.toString())) || moderator) {
                        message.delete()
                    }
                }
            }
            if (emoji == ReactionEmoji.Unicode("⛔")) {
                for (i in message.getReactors(emoji).toList()) {
                    var moderator = false
                    for (k in i.asMember(guildId!!).roleBehaviors) {
                        if (k.asRole().permissions.contains(Permission.ModerateMembers)) moderator = true
                    }
                    if (owners.contains<Any?>(Json.encodeToJsonElement(i.id.toString())) || moderator) {
                        ignoreNext = true
                    }
                }
            }
        }
    }
    kord!!.on<MessageCreateEvent> {
        if (message.channel.asChannel().type == ChannelType.DM && !allowDMs)
            return@on
        if (message.author?.id == kord.selfId)
            return@on
        val messageContent = message.content.split(" ")
        if (messageContent.isEmpty())
            return@on
        try {
            when (messageContent[0].lowercase()) {
                "!ping" -> commandManager.ping(message)
                "!debug" -> commandManager.debug(message)
                "!bonk" -> commandManager.bonk(message, messageContent)
                "!reset" -> commandManager.reset(message)
                "!stop" -> commandManager.stop(message)
                "!continue" -> commandManager.continueCmd(message)
                "!echo" -> {
                    if (messageContent.size >= 2) {
                        commandManager.echo(message, messageContent)
                    } else {
                        reply(message, "You must specify a message to echo")
                    }
                }

                "!llm" -> {
                    if (messageContent.size == 2) {
                        when (messageContent[1].lowercase()) {
                            "reset" -> commandManager.reset(message)
                            "stop" -> commandManager.stop(message)
                            "continue" -> commandManager.continueCmd(message)
                            "relog" -> commandManager.relog(message)
                            "restart" -> commandManager.restart(message)
                            else -> LLM.onCommand(message, messageContent)
                        }
                    } else LLM.onCommand(message, messageContent)
                }

                "!blocklist" -> {
                    if (messageContent.size == 3) {
                        try {
                            if (checkPermissions(message)) {
                                when (messageContent[1].lowercase()) {
                                    "add" -> commandManager.blocklistAdd(message, messageContent[2])
                                    "remove" -> commandManager.blocklistRemove(message, messageContent[2])
                                }
                            } else {
                                message.channel.createMessage("Sorry, but you do not have the correct permission to do so.")
                                println("${message.author?.username} tried to tamper with the blocklist, but they lack the permission to do so! Skill issue.")
                            }
                        } catch (e: NullPointerException) {
                            println("Caught NullPointerException in blocklist")
                            message.channel.createMessage("Checking permissions failed: NullPointerException")
                        }
                    } else {
                        message.channel.createMessage("Command has an incorrect amount of parameters, expecting '!blocklist add/remove USER'")
                    }
                }

                "!filter" -> {
                    if (messageContent.size >= 3) {
                        if (messageContent[1].lowercase() == "toggle") {
                            filter.toggleFilter(messageContent.drop(2).joinToString(" "), message)
                        }
                    }
                }

                else -> {
                    if (message.mentionedUserIds.contains(kord.selfId)) {
                        if (ignoreNext) {
                            ignoreNext = false
                            return@on
                        }
                        val botResponse = LLM.onPing(message)
                        println("$charName: $botResponse")
                        reply(message, botResponse)
                    } else if (message.referencedMessage?.author?.id == kord.selfId) {
                        if (ignoreNext) {
                            ignoreNext = false
                            return@on
                        }
                        val botResponse = LLM.onPing(message)
                        println("$charName: $botResponse")
                        reply(message, botResponse)
                    }
                }
            }
        } catch (e: Exception) {
            println(e.toString())
            for (i in e.stackTrace) println(i.toString())
            }
        }
    println("ready")
    while (loginAgain) {
        println("Logging in as ${kord!!.getSelf().username}")
        kord!!.login {
            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent
            presence {
                status = botStatus.first
                playing(botStatus.second)
            }
        }
    }
    println("logged out")
    if (restartBot) {
        println("restarting...")
        runBlocking {
            ProcessBuilder("java", "-jar", "LLMKotlin.jar").redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT).start().waitFor()
        }
        println("exit hosting (probably broken) layer")
    }
}

suspend fun checkPermissions(message: Message): Boolean {
    var moderator = false
    for (i in message.author?.asMember(message.getGuild().id)!!.roleBehaviors) {
        if (i.asRole().permissions.contains(Permission.ModerateMembers)) moderator = true
    }
    return owners.contains<Any?>(Json.encodeToJsonElement(message.author?.id.toString())) || moderator
}

suspend fun reply(message: Message, input: String) {
    message.reply {
        content = input
    }
}

fun getStatus(): Pair<PresenceStatus, String> {
    val rawStatus = dotenv["STATUS"] ?: "away"
    val status = if (rawStatus.lowercase() == "online") {
        PresenceStatus.Online
    } else if (rawStatus.lowercase() == "away") {
        PresenceStatus.Idle
    } else if (rawStatus.lowercase() == "dnd") {
        PresenceStatus.DoNotDisturb
    } else {
        PresenceStatus.Invisible
    }
    val presence = dotenv["PRESENCE"] ?: "Chat with !LLM"
    return Pair(status, presence)
}