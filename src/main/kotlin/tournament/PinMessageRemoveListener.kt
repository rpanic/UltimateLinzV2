package tournament

import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class PinMessageRemoveListener(internal var channel: TextChannel) : ListenerAdapter() {

    var count = 0
    var runnable: () -> Unit = {}

    override fun onMessageReceived(event: MessageReceivedEvent) {

        if (event.channelType == ChannelType.TEXT && event.textChannel.idLong == channel.idLong) {

            if (event.message.type.id == MessageType.CHANNEL_PINNED_ADD.getId()) {
                event.message.delete().queue()
                println(event.message.idLong.toString() + " message deleted from pinremover ")

                if (count > 0) {
                    count--
                    if (count == 0) {
                        runnable()
                    }
                }

            }
        }

        super.onMessageReceived(event)
    }

    fun afterCount(count: Int, runnable: () -> Unit) {
        this.count = count
        this.runnable = runnable
    }
}