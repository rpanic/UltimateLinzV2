package model

import main.EmoteLimiter
import main.Main
import main.emoteOrEmojiName
import main.isNull
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.EventListener
import java.util.*

class ReactionStateObserver(val message: Message, val limiter: EmoteLimiter) : StateObserver<GenericEvent>() {

    private val reactions = mutableMapOf<String, MutableList<User>>()

    fun getReactionByUser(id: Long) =
        reactions.entries.firstOrNull { it.value.any { it.idLong == id } }?.key

    fun getReactions() = reactions

    override fun trigger(f: (GenericEvent) -> Unit) {

        //Init
        message.reactions
            .forEach {
                val users = it.retrieveUsers().complete()
                reactions[it.reactionEmote.emoteOrEmojiName()] = users.filterNotNull().toMutableList()
            }


        //Listener
        Main.jda.addEventListener(EventListener {
            println("ReactionStateObserver: ${it.javaClass.name}")
            if (it is MessageReactionAddEvent && it.messageIdLong == message.idLong) {

                val reactionEmoji = it.reactionEmote.emoteOrEmojiName()

                if(reactions.containsKey(reactionEmoji)){
                    reactions[reactionEmoji]!!.add(it.user)
                }else{
                    reactions[reactionEmoji] = mutableListOf(it.user)
                }

            } else if (it is MessageReactionRemoveEvent && it.messageIdLong == message.idLong) {

                val reactionEmoji = it.reactionEmote.emoteOrEmojiName()

                if(reactions.containsKey(reactionEmoji)) {
                    reactions[reactionEmoji]!!.remove(it.user)
                    if (reactions[reactionEmoji]!!.isEmpty()) {
                        reactions.remove(reactionEmoji)
                    }
                }

            } else if (it is MessageReactionRemoveAllEvent && it.messageIdLong == message.idLong) {

                reactions.clear()

            }

            f(it)

        })

        then {

            if (it is MessageReactionAddEvent) {
                limiter.onMessageReactionAdd(it)
            } else if (it is MessageReactionRemoveEvent) {
                limiter.onMessageReactionRemove(it)
            }

        }
    }

}