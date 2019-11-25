package watch

import db.DB
import main.Main
import main.nickName
import model.ReactionStateObserver

fun linkWatchToReaction(observer: ReactionStateObserver){

    observer.listeners.add { type, choice, user ->

        if(type){

            val watchers = DB.getList<Watcher>("watchers")
            watchers.forEach {
                val watcherUser = Main.jda.getUserById(it.id)
                if(watcherUser != null){

                    val tournament = observer.message.channel.name
                    val message = "${user.nickName(Main.jda.guilds[0])} changed vote on Tournament $tournament to $choice"

                    watcherUser.openPrivateChannel().complete().sendMessage(message).complete()
                }
            }

        }

    }




}