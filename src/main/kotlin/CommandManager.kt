package org.bot

import dev.kord.core.behavior.reply
import dev.kord.core.entity.Message
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.nio.file.*
import java.time.*
import java.time.format.DateTimeFormatter

class CommandManager {
    suspend fun ping(message: Message) {
        message.channel.createMessage("pong!\nAverage gateway ping: ${kord!!.gateway.averagePing}")
    }

    suspend fun debug(message: Message) {
        if (owners.contains<Any?>(Json.encodeToJsonElement(message.author?.id.toString()))) {
            println("User ${message.author?.username} is in owners")
            message.channel.createMessage("User is in owners")
        } else {
            println("User ${message.author?.username} is not in owners")
            message.channel.createMessage("User is not in owners")
        }
    }

    suspend fun stop(message: Message) {
        if (checkPermissions(message)) {
            message.channel.createMessage("The bot is shutting down. Please ping the host if you wish to restart it.")
            loginAgain = false
            kord!!.logout()
        }  else {
            message.channel.createMessage("Sorry, but you do not have the correct permission to do so.")
            println("${message.author?.username} tried to stop the LLM, but they lack the permission to do so!\nThe UserID need to be in the `.env` file in the `OWNERS` variable for them to gain the right permissions. Skill issue.")
        }
    }

    suspend fun restart(message: Message) {
        if (checkPermissions(message)) {
            message.channel.createMessage("The bot is restarting...")
            loginAgain = false
            restartBot = true
            kord!!.logout()
        }  else {
            message.channel.createMessage("Sorry, but you do not have the correct permission to do so.")
            println("${message.author?.username} tried to restart the LLM, but they lack the permission to do so!\nThe UserID need to be in the `.env` file in the `OWNERS` variable for them to gain the right permissions. Skill issue.")
        }
    }

    suspend fun reset(message: Message) = runBlocking {
        if (checkPermissions(message)) {
            if (File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").exists()) {
                val newPath = Paths.get(
                    "./src/Logs/Logs${message.channel.id}/CharacterLogs ${
                        DateTimeFormatter.ofPattern("yyyy MM dd HH mm ss SSSSSS").withZone(ZoneOffset.UTC)
                            .format(Instant.now())
                    }.LLMD"
                )
                Files.move(Paths.get("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD"), newPath, StandardCopyOption.REPLACE_EXISTING)
                File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").createNewFile()
                File("./src/Logs/Logs${message.channel.id}/CharacterLogs.LLMD").printWriter().use {
                    it.println("$charName: $greeting")
                }
                message.channel.createMessage("Message log reset successfully, <@${message.author!!.id}>")
                message.channel.createMessage(greeting)
                println("${message.author!!.username} reset the chatlogs for channel ${message.channel.id}")
                println("$charName: $greeting")
            } else {
                message.channel.createMessage("Error: log file has not been created yet, start a conversation to create a log file.")
            }
        } else {
            message.channel.createMessage("Sorry, but you do not have the correct permission to do so.")
            println("${message.author?.username} tried to reset the LLM, but they lack the permission to do so!\\nThe UserID need to be in the `.env` file in the `OWNERS` variable for them to gain the right permissions. Skill issue.")
        }
    }

    suspend fun blocklistAdd(message: Message, uID: String) {
        val userToBlock = uID.removePrefix("<@").removeSuffix(">")
        if (userToBlock.toDoubleOrNull() == null) {
            message.channel.createMessage("<@${message.author!!.id}> Invalid uID provided")
            return
        }
        val blockListMutable = blockList.toMutableList()
        if (blockList.contains<Any?>(Json.encodeToJsonElement(userToBlock))) {
            message.channel.createMessage("User with userID $userToBlock (@silent <@uID>) is already part of the blocklist")
            println("${message.author!!.username} tried to add $userToBlock to the blocklist again. He must hate that guy.")
        } else {
            blockListMutable.addLast(Json.encodeToJsonElement(userToBlock))
            message.channel.createMessage("User with userID $userToBlock (@silent <@uID>) added to the blocklist")
            println("${message.author!!.username} successfully added $userToBlock to the blocklist.")
        }
        blockList = Json.encodeToJsonElement(blockListMutable).jsonArray
        File("./src/Blocklist.json").printWriter().use {
            it.println(blockList)
        }
    }

    suspend fun blocklistRemove(message: Message, uID: String) {
        val userToRemove = uID.removePrefix("<@").removeSuffix(">")
        if (userToRemove.toDoubleOrNull() == null) {
            message.channel.createMessage("<@${message.author!!.id}> Invalid uID provided")
            return
        }
        if (!blockList.contains<Any?>(Json.encodeToJsonElement(userToRemove))) {
            message.channel.createMessage("$userToRemove is not in the blocklist")
            println("${message.author!!.username} tried to remove $userToRemove from the blocklist when he's not in it.")
        } else {
            val blockListMutable = blockList.toMutableList()
            blockListMutable.remove(Json.encodeToJsonElement(userToRemove))
            message.channel.createMessage("User with userID $userToRemove (@silent <@uID>) removed from the blocklist")
            println("${message.author!!.username} successfully removed $userToRemove from the blocklist.")
            blockList = Json.encodeToJsonElement(blockListMutable).jsonArray
        }
        File("./src/Blocklist.json").printWriter().use {
            it.println(blockList)
        }
    }

    suspend fun bonk(message: Message, messageContents: List<String>) {
        if (messageContents.size == 1) {
            message.channel.createMessage("${message.author?.username} bonked the air! <:SCHIZO:1215082198164701294>")
        } else if (message.author?.username == messageContents[1] || message.author?.id.toString() == messageContents[1].removePrefix("<@").removeSuffix(">")) {
            message.channel.createMessage("${message.author?.username} bonked themself!")
        } else {
            message.channel.createMessage("${message.author?.username} bonked ${messageContents[1]}")
        }
    }

    suspend fun echo(message: Message, messageContents: List<String>) {
        val userMessage = messageContents.toMutableList().drop(1).joinToString(" ")
        println("${message.author?.username}: $userMessage")
        reply(message, userMessage)
        if (!File("./src/Logs/EchoLogs.txt").exists()) {
            if (!File("./src/Logs").exists()) {
                File("./src/Logs").mkdir()
                println("Log directory was not found so it was created automatically")
            }
            File("./src/Logs/EchoLogs.txt").createNewFile()
            println("Successfully created echo log file")
        }
        val logs = File("./src/Logs/EchoLogs.txt").readText().trim()
        File("./src/Logs/EchoLogs.txt").printWriter().use {
            it.print("$logs\n${message.author!!.username}: $userMessage".trim())
        }
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
        message.reply {
            content = botResponse
        }
    }

    suspend fun relog(message: Message) {
        if (checkPermissions(message)){
            println("The bot was commanded to relog by ${message.author!!.username}")
            kord!!.logout()
        } else {
            println("${message.author!!.username} tried to make the bot relogin, but they lack the permissions to do so")
            reply(message, "Sorry, but you do not have the correct permissions to do so")
        }
    }
}