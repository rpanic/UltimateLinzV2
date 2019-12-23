package watch

import db.DB
import main.ListenerAdapterCommand
import main.Permissioned
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class WatchListenerAdapter : ListenerAdapterCommand("watch"){

    var watchers = DB.getList<Watcher>("watchers")

    @Permissioned("Vorstand", "Moderator")
    fun on(event: MessageReceivedEvent, msg: Array<String>){

        watchers.add(Watcher(event.author.idLong))

        event.channel.sendMessage("Watching turned on").complete()

    }

    @Permissioned("Vorstand", "Moderator")
    fun off(event: MessageReceivedEvent, msg: Array<String>){

        watchers.remove(watchers.find { it.id == event.author.idLong })

        event.channel.sendMessage("Watching turned off").complete()

    }

    override fun help(event: MessageReceivedEvent?, msg: Array<out String>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}