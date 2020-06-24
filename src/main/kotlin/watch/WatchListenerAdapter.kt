package watch

import db.DB
import main.Help
import main.ListenerAdapterCommand
import main.Permissioned
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

@Help("Schaltet Benachrichtungen, wenn jemand bei Turnieren abstimmt ein oder aus")
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

        watchers.find { it.id == event.author.idLong }?.apply {
            watchers.remove(this)
        }

        event.channel.sendMessage("Watching turned off").complete()

    }

}