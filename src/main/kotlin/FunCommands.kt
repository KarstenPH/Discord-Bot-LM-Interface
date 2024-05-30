package org.bot

import dev.kord.core.entity.Message
import java.io.File

class FunCommands {
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

    suspend fun bonk(message: Message, messageContents: List<String>) {
        if (messageContents.size == 1) {
            message.channel.createMessage("${message.author?.username} bonked the air! <:SCHIZO:1215082198164701294>")
        } else if (message.author?.username == messageContents[1] || message.author?.id.toString() == messageContents[1].removePrefix("<@").removeSuffix(">")) {
            message.channel.createMessage("${message.author?.username} bonked themself!")
        } else {
            message.channel.createMessage("${message.author?.username} bonked ${messageContents[1]}")
        }
    }
}