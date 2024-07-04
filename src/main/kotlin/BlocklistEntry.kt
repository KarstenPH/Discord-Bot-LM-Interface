package org.bot

import kotlinx.serialization.Serializable

@Serializable
data class BlocklistEntry(val uID: String, val blockedBy: String = "not set", val blockedTimestamp: String = "not set")
