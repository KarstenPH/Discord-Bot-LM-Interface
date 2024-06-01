package org.bot

import dev.kord.core.entity.Message

class CallCommand {
    suspend fun call(message: Message) {
        if (checkPermissions(message)) {
            val messageParts = message.content.split(" ")
            if (messageParts.size == 3) {
                if (messageParts[2].toLongOrNull() != null) {
                    message.channel.createMessage("<@${messageParts[2]}> Hi there!")
                } else {
                    message.channel.createMessage("Invalid uID, uID should be numeric.")
                }
            } else {
                message.channel.createMessage("Invalid command arguments, expected '$commandIdentifier call [uID]'")
            }
        } else {
            message.channel.createMessage("Sorry, but you do not have the correct permissions to use that.")
        }
    }
}