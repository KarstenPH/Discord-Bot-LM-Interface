package org.bot

import dev.kord.core.entity.Message
import kotlinx.serialization.json.*
import java.io.File

class FilterManager {
    private val filterLists = Json.decodeFromString<JsonArray>(dotenv["FILTERS"])
    private val filterListsEnabled = mutableMapOf<String, Boolean>()
    private val regexPattern = Regex("""(^(?<=[\W])*\W+|\W*(?<=\W)*$)""") //RegEx pattern by Edelweiss

    fun buildFilterMap() {
        for (i in filterLists) {
            filterListsEnabled[i.jsonPrimitive.content] = true
        }
    }

    suspend fun toggleFilter(filter: String, message: Message) {
        if (checkPermissions(message)) {
            if (filterListsEnabled.containsKey(filter)) {
                filterListsEnabled[filter] = !filterListsEnabled[filter]!!
                message.channel.createMessage(
                    "The filter $filter was changed to ${
                        if (filterListsEnabled[filter]!!) {
                            "enabled"
                        } else {
                            "disabled"
                        }
                    }"
                )
                println("${message.author!!.username} toggled filter $filter")
            } else {
                message.channel.createMessage("The filter $filter does not exist")
                println("${message.author!!.username} tried to toggle the filter $filter, but it does not exist")
            }
        } else {
            message.channel.createMessage("Sorry, but you do not have the correct permissions to do so.")
            println("${message.author!!.username} tried to toggle the filter $filter, but they lack the permissions to do so")
        }
    }

    private fun loadFilters(): JsonArray {
        return buildJsonArray{
            for (i in filterListsEnabled) {
                if (i.value) {
                    for (word in Json.decodeFromString<JsonArray>(File("./src/Filters/${i.key}.json").readText())) {
                        add(word.jsonPrimitive.content)
                    }
                }
            }
        }
    }

    fun filter(unfiltered: String): String {
        val filters = loadFilters()
        val words = unfiltered.split(" ")
        val filtered = mutableListOf<String>()
        for (word in words) {
            filtered.add(if (!filters.contains(Json.encodeToJsonElement(clean(word.lowercase())))) {
                word
            } else {
                println("Filtered word \"$word\"")
                "[Filtered]"
            })
        }
        return filtered.joinToString(" ")
    }

    fun filterStrict(unfiltered: String): String {
        val filters = loadFilters()
        val words = unfiltered.split(" ")
        val filtered = mutableListOf<String>()
        var wordIndex = 1
        for (word in words) {
            if (wordIndex == words.size) {
                filtered.add(if (!filters.contains(Json.encodeToJsonElement(clean(word.lowercase())))) {
                    word
                } else {
                    println("Strict filtered word or phrase \"$word\"")
                    "[Filtered]"
                })
            } else {
                val wordSubset = words.subList(wordIndex + 1, words.size)
                var toCheck = clean(word.lowercase())
                if (filters.contains(Json.encodeToJsonElement(toCheck))) {
                    filtered.add(word)
                } else {
                    var needsFiltering = false
                    for (i in wordSubset) {
                        toCheck += " " + clean(i.lowercase())
                        if (filters.contains(Json.encodeToJsonElement(toCheck))) {
                            needsFiltering = true
                            break
                        }
                    }
                    filtered.add(if (needsFiltering) {
                        println("Strict filtered word or phrase \"$word\"")
                        "[Filtered]"
                    } else {
                        word
                    })
                }
                wordIndex++
            }
        }
        return filtered.joinToString(" ")
    }
    private fun clean(input: String) = input.replace(regexPattern, "")
}