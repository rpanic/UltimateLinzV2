package watch

import db.ChangeObserver
import db.DB
import main.*
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateOnlineStatusEvent
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*
import observable.Observable

class Login(val user: Long, val time: Long, val status: String) : Observable()

@Help("A static of last logins of users")
class LogonStatisticsListenerAdapter : ListenerAdapterCommand("login"){

    override fun onUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {

        val logins = DB.getList<Login>("logins")

        logins.add(Login(event.user.idLong, System.currentTimeMillis(), event.newOnlineStatus.name))

        if(logins.size > 1000){
            logins.removeAt(0)
        }

    }

    @Permissioned("Vorstand", "Moderator")
    @Help("Lists all last logins")
    fun list(event: MessageReceivedEvent?, msg: Array<out String>?){

        if(event?.channelType == ChannelType.PRIVATE) {
            if (msg?.size == 2) {

                val logins = DB.getList<Login>("logins").list()

                val data = logins.groupBy { it.user }
                    .map {
                        val list = it.value.sortedByDescending { x -> x.time }.toMutableList()
                        if (list[0].status == OnlineStatus.OFFLINE.name) {
                            list.removeAt(0)
                        }

                        val index = list.indexOfFirst { x -> x.status == OnlineStatus.OFFLINE.name }
                        if (index > 0) {
                            Pair(it.key, list[index - 1])
                        } else {
                            null
                        }
                    }
                    .filterNotNull()
                    .sortedByDescending { it.second.time }

                val columns = AsciiTable.ColumnDefinition<String>()
                    .addPrimaryColumn("Name")
                    .addInfoColumn("last login")

                val table = AsciiTable<String>(null, columns)

                val dateFormat = SimpleDateFormat("dd.MM.YYYY hh:mm")

                data.forEach {
                    val user = Main.jda.getUserById(it.first)
                    val time = dateFormat.format(Date.from(Instant.ofEpochMilli(it.second.time)))
                    table.data(listOf(user!!.nickName(Main.jda.guilds[0]), time), listOf())
                }

                event.channel.sendMessage(MessageBuilder().appendCodeBlock(table.renderAscii(), "").build()).complete()

            }
        }

    }

}