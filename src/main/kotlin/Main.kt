package org.bot

import dev.kord.common.entity.*
import dev.kord.core.*
import dev.kord.core.behavior.reply
import dev.kord.core.entity.*
import dev.kord.core.event.message.*
import dev.kord.gateway.*
import java.io.*
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

val dotenv = dotenv()
val botToken: String? = dotenv["TOKEN"]
val owners = Json.decodeFromString<JsonArray>(dotenv["OWNERS"])
val llmUrl: String? = dotenv["LLMURL"]
var kord: Kord? = null
var blockList = Json.decodeFromString<JsonArray>("[]")
val callCommand = CallCommand()
val LLM = LLMManager()
var ignoreNext = false
val filter = FilterManager()
val strictFiltering = try { dotenv["STRICT_FILTERING"].toBooleanStrictOrNull()!! } catch (e: NullPointerException) { throw Exception("NonBooleanStrictFilteringException") }
val allowDMs = dotenv["ALLOW_DMS"].toBooleanStrictOrNull() ?: false
val botStatus = getStatus()
var loginAgain = true
var restartBot = false
val funCommands = FunCommands()
val debugCommands = DebugCommands()
val managementCommands = ManagementCommands()
val commandIdentifier = if (dotenv["COMMAND_IDENTIFIER"] != null) { dotenv["COMMAND_IDENTIFIER"].lowercase() } else "!llm"
const val botVersion = "Discord bot LMI by Superbox\nV1.2.0\n"

@OptIn(ExperimentalCoroutinesApi::class)
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
    val nonParallelDispatcher = Dispatchers.Default.limitedParallelism(1)
    kord = Kord(botToken)
    kord!!.on<ReactionAddEvent>(CoroutineScope(nonParallelDispatcher)) {
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
    kord!!.on<MessageCreateEvent>(CoroutineScope(nonParallelDispatcher)) {
        if (message.channel.asChannel().type == ChannelType.DM && !allowDMs)
            return@on
        if (message.author?.id == kord.selfId)
            return@on
        val messageContent = message.content.split(" ")
        if (messageContent.isEmpty())
            return@on
        try {
            when (messageContent[0].lowercase()) {
                "!ping" -> debugCommands.ping(message)
                "!debug" -> debugCommands.debug(message)
                "bonk$commandIdentifier" -> funCommands.bonk(message, messageContent)
                "reset$commandIdentifier" -> managementCommands.reset(message)
                "stop$commandIdentifier" -> debugCommands.stop(message)
                "continue$commandIdentifier" -> LLM.continueCmd(message)
                "echo$commandIdentifier" -> {
                    if (messageContent.size >= 2) {
                        funCommands.echo(message, messageContent)
                    } else {
                        reply(message, "You must specify a message to echo")
                    }
                }

                commandIdentifier -> {
                    if (messageContent.size == 3 && messageContent[1].lowercase() == "call") {
                        callCommand.call(message)
                    } else if (messageContent.size == 2) {
                        when (messageContent[1].lowercase()) {
                            "reset" -> managementCommands.reset(message)
                            "stop" -> debugCommands.stop(message)
                            "continue" -> LLM.continueCmd(message)
                            "relog" -> debugCommands.relog(message)
                            "restart" -> debugCommands.restart(message)
                            else -> LLM.onCommand(message, messageContent)
                        }
                    } else LLM.onCommand(message, messageContent)
                }

                "blocklist$commandIdentifier" -> {
                    if (messageContent.size == 3) {
                        try {
                            if (checkPermissions(message)) {
                                when (messageContent[1].lowercase()) {
                                    "add" -> managementCommands.blocklistAdd(message, messageContent[2])
                                    "remove" -> managementCommands.blocklistRemove(message, messageContent[2])
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
                        message.channel.createMessage("Command has an incorrect amount of parameters, expecting 'blocklist$commandIdentifier add/remove USER'")
                    }
                }

                "filter$commandIdentifier" -> {
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
                        LLM.onPing(message)
                    } else if (message.referencedMessage?.author?.id == kord.selfId) {
                        if (ignoreNext) {
                            ignoreNext = false
                            return@on
                        }
                        LLM.onPing(message)
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