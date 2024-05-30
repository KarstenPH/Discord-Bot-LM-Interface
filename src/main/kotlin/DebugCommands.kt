package org.bot

import dev.kord.core.entity.Message
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class DebugCommands {
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