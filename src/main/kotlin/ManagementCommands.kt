package org.bot

import dev.kord.core.entity.Message
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class ManagementCommands {
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
                    it.println("${LLM.charName}: ${LLM.greeting}")
                }
                message.channel.createMessage("Message log reset successfully, <@${message.author!!.id}>")
                message.channel.createMessage(LLM.greeting)
                println("${message.author!!.username} reset the chatlogs for channel ${message.channel.id}")
                println("${LLM.charName}: ${LLM.greeting}")
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
}