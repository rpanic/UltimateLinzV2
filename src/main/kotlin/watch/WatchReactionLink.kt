package watch

import db.DB
import helper.EmoteListener
import helper.SimpleEmoteLimiter
import main.Main
import main.nickName
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent

fun linkWatchToLimiter(observer: SimpleEmoteLimiter){

    observer.addEmoteListener(object: EmoteListener{
        override fun emoteAdd(e: MessageReactionAddEvent, limiter: SimpleEmoteLimiter) {

            val watchers = DB.getList<Watcher>("watchers")
            watchers.forEach {
                val watcherUser = Main.jda.getUserById(it.id)
                if(watcherUser != null){

                    val tournament = observer.message.channel.name
                    val message = "${e.user.nickName(Main.jda.guilds[0])} changed vote on Tournament $tournament to ${e.reactionEmote.emote.name}"

                    watcherUser.openPrivateChannel().complete().sendMessage(message).complete()
                }
            }

        }

        override fun emoteRemove(e: MessageReactionRemoveEvent, limiter: SimpleEmoteLimiter) {
        }
    })
}