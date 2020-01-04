package watch

import db.DB
import main.ListenerAdapterCommand
import main.Permissioned
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class WatchListenerAdapter : ListenerAdapterCommand("watch"){

    var watchers = DB.getList<Watcher>("watchers")

    @Permissioned("Vorstand", "Moderator")
    fun on(event: MessageReceivedEvent, msg: Array<String>){

        val userId = event.author.idLong

        val response = if(!watchers.any { it.id == userId }){
            watchers.add(Watcher(userId))
            "Watching turned on"
        }else{
            "Watching already turned on"
        }

        event.channel.sendMessage(response).complete()
    }

    @Permissioned("Vorstand", "Moderator")
    fun off(event: MessageReceivedEvent, msg: Array<String>){

        watchers.remove(watchers.find { it.id == event.author.idLong })

        event.channel.sendMessage("Watching turned off").complete()

    }

//    override fun help(event: MessageReceivedEvent, msg: Array<out String>?) {
//
//        event.channel.sendMessage("Options:\n-on\n-off").complete()
//
//    }

}