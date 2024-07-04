package org.bot

import dev.kord.core.entity.Message
import java.io.File
import kotlinx.serialization.json.*

class BlocklistManager {
    suspend fun blocklistAdd(message: Message, uID: String) {
        val userToBlock = uID.removePrefix("<@").removeSuffix(">")
        if (userToBlock.toDoubleOrNull() == null) {
            message.channel.createMessage("<@${message.author!!.id}> Invalid uID provided")
            return
        }
        val currentTime = getBlocklistTimestamp()
        val blockingUser = message.author!!.username
        val blockListMutable = blocklistUIDs.toMutableList()
        if (blocklistUIDs.contains<Any?>(Json.encodeToJsonElement(userToBlock))) {
            message.channel.createMessage("User with userID $userToBlock (@silent <@uID>) is already part of the blocklist")
            println("$blockingUser tried to add $userToBlock to the blocklist again. He must hate that guy.")
        } else {
            blockListMutable.addLast(Json.encodeToJsonElement(userToBlock))
            message.channel.createMessage("User with userID $userToBlock (@silent <@uID>) added to the blocklist")
            println("$blockingUser successfully added $userToBlock to the blocklist.")
            blocklistUIDs = Json.encodeToJsonElement(blockListMutable).jsonArray
            blockList = buildJsonObject {
                put("blocklistEntries", buildJsonArray {
                    for (i in blockList["blocklistEntries"]!!.jsonArray) {
                        val entry = Json.decodeFromString<BlocklistEntry>(i.toString())
                        add(
                            Json.encodeToJsonElement(
                                BlocklistEntry(
                                    entry.uID,
                                    entry.blockedBy,
                                    entry.blockedTimestamp
                                )
                            )
                        )
                    }
                    add(Json.encodeToJsonElement(BlocklistEntry(userToBlock, blockingUser, "<t:$currentTime>")))
                })
            }
        }
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
        if (!blocklistUIDs.contains<Any?>(Json.encodeToJsonElement(userToRemove))) {
            message.channel.createMessage("$userToRemove is not in the blocklist")
            println("${message.author!!.username} tried to remove $userToRemove from the blocklist when he's not in it.")
        } else {
            val blockListMutable = blocklistUIDs.jsonArray.toMutableList()
            blockListMutable.remove(Json.encodeToJsonElement(userToRemove))
            message.channel.createMessage("User with userID $userToRemove (@silent <@uID>) removed from the blocklist")
            println("${message.author!!.username} successfully removed $userToRemove from the blocklist.")
            blocklistUIDs = Json.encodeToJsonElement(blockListMutable).jsonArray
            blockList = buildJsonObject {
                put("blocklistEntries", buildJsonArray {
                    for (i in blockList["blocklistEntries"]!!.jsonArray) {
                        val entry = Json.decodeFromString<BlocklistEntry>(i.toString())
                        if (blockListMutable.contains(Json.encodeToJsonElement(entry.uID))) {
                            add(
                                Json.encodeToJsonElement(
                                    BlocklistEntry(
                                        entry.uID,
                                        entry.blockedBy,
                                        entry.blockedTimestamp
                                    )
                                )
                            )
                        }
                    }
                })
            }
        }
        File("./src/Blocklist.json").printWriter().use {
            it.println(blockList)
        }
    }

    suspend fun blocklistInfo(message: Message, uID: String) {
        val userToCheck = uID.removePrefix("<@").removeSuffix(">")
        if (userToCheck.toDoubleOrNull() == null) {
            message.channel.createMessage("<@${message.author!!.id}> Invalid uID provided")
            return
        }
        println("${message.author!!.username} is fetching blocklist info for $userToCheck")
        if (!blocklistUIDs.contains<Any?>(Json.encodeToJsonElement(userToCheck))) {
            message.channel.createMessage("$userToCheck is not in the blocklist")
            return
        }
        for (i in blockList["blocklistEntries"]!!.jsonArray) {
            val entry = Json.decodeFromString<BlocklistEntry>(i.toString())
            if (entry.uID == userToCheck) {
                message.channel.createMessage("""User: $userToCheck
                    |blocked by: ${entry.blockedBy}
                    |time blocked: ${entry.blockedTimestamp}
                """.trimMargin())
            }
        }
    }

    private fun getBlocklistTimestamp() = System.currentTimeMillis() / 1000
}