package helper

import main.emoteOrEmojiName
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class SimpleEmoteLimiter(var message: Message) : ListenerAdapter() {

    var emotes = listOf<Emote>()

    var locked = false

    lateinit var reactionsMap: MutableMap<User, Emote>

    fun start(guild: Guild){

        message = message.channel.retrieveMessageById(message.idLong).complete()

        emotes.forEach { message.addReaction(it).queue() }

        reactionsMap = message.reactions
            .map { it.retrieveUsers().complete()
                .filter { u -> !u.isBot }
                .map { u -> Pair(u, it.reactionEmote.emote) } }
            .flatten()
            .toMap()
            .toMutableMap()

        guild.jda.addEventListener(this)

    }

    private val listeners = mutableListOf<EmoteListener>()

    fun addEmoteListener(listener: EmoteListener){

        listeners.add(listener)

    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {

//        println("reactionAdd: ${event.messageIdLong} | ${message.idLong} | ${event.user.name}")

        if(event.messageIdLong == message.idLong && !event.user.isBot) {

            if (locked) {
                event.reaction.removeReaction(event.user).complete()
                return
            }

            if (event.reaction.reactionEmote.emote in emotes) {

                reactionsMap[event.user] = event.reactionEmote.emote

                listeners.forEach { it.emoteAdd(event, this) }

                //Remove all others
                if(event.reaction.retrieveUsers().complete().contains(event.user)) {  //TODO Eventuell durch ein async repair ersetzen

                    message = event.channel.retrieveMessageById(message.idLong).complete()

                    message.reactions.filter { it.reactionEmote.emote in emotes }
                        .filter { event.user in it.retrieveUsers().complete() }
                        .filter { it.reactionEmote.emoteOrEmojiName() != event.reactionEmote.emoteOrEmojiName() }
                        .forEach { it.removeReaction(event.user).queue() }

                }
            }
        }
        super.onMessageReactionAdd(event)
    }

    fun getReactionsByUser() : Map<User, Emote>{
        //TODO Replace with event-based map
        return reactionsMap
    }

}

interface EmoteListener {
    fun emoteAdd(e: MessageReactionAddEvent, limiter: SimpleEmoteLimiter)
    fun emoteRemove(e: MessageReactionRemoveEvent, limiter: SimpleEmoteLimiter)
}