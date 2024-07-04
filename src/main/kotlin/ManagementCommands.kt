package org.bot

import dev.kord.core.entity.Message
import kotlinx.coroutines.runBlocking
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
}