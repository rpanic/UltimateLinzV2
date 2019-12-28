package helper

import main.EmoteLimiter
import main.EmoteLimiter.EmoteListener
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.events.emote.EmoteAddedEvent
import net.dv8tion.jda.api.events.emote.EmoteRemovedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class SimpleEmoteLimiter(val message: Message) : ListenerAdapter() {

    var emotes = listOf<Emote>()

    var locked = false

    fun start(guild: Guild){
//        message.reactions.filter { it.reactionEmote.emote.name in emotes }

        guild.jda.addEventListener(this)

    }

    private val listeners = mutableListOf<EmoteListener>()

    fun addEmoteListener(listener: EmoteListener){

        listeners.add(listener)

    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {

        if(locked){
            event.reaction.removeReaction(event.user).complete()
            return
        }

        if(event.reaction.reactionEmote.emote in emotes){

            listeners.forEach { it.emoteAdd(event) }

            //Remove all others
            message.reactions.filter { it.reactionEmote.emote in emotes }
                .filter { event.user in it.retrieveUsers().complete() }
                .forEach { it.removeReaction(event.user).complete() }

        }

        super.onMessageReactionAdd(event)
    }

}

//fun List<MessageReaction>.toUserMap(){
//
//    this.map { it.retrieveUsers().complete() }
//
//}