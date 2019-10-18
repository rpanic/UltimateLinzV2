package main

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.lang3.event.EventUtils.addEventListener
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.MessageChannel



class Prompt(val prompt: String, val channel: MessageChannel, val promptedUser: User) : ListenerAdapter() {

    private var latch: CountDownLatch? = null

    var callback: Consumer<String>? = null

    var tempMessage: Message? = null

    var deleteDelay = -1L

    var ret: String? = null

//    fun setDelete(millis: Long): Prompt {
//        if (millis != -1) {
//            deleteDelay = millis
//            println("Delete set to $millis")
//        }
//        return this
//    }

    fun promptSync(): String? {

        tempMessage = channel.sendMessage(prompt).complete()
        channel.jda.addEventListener(this)
        latch = CountDownLatch(1)
        try {
            latch!!.await()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        channel.jda.removeEventListener(this)

        println("Returning answer")

        return ret
    }

    fun promptAsync(callback: Consumer<String>) {

        this.callback = callback

        tempMessage = channel.sendMessage(prompt).complete()
        channel.jda.addEventListener(this)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        super.onMessageReceived(event)

        if (channel == event.channel && event.author == promptedUser) {
            ret = event.message.contentRaw
            if (latch != null) {
                latch!!.countDown()
            } else if (callback != null) {
                channel.jda.removeEventListener(this)
                callback!!.accept(ret!!)
            } else {
                println("Impossible case")
                return
            }
//            if (deleteDelay != -1) {
//                MessageTimer.deleteAfter(tempMessage, deleteDelay)
//                MessageTimer.deleteAfter(event.message, deleteDelay)
//            }
            println("Callback")
        }
    }

}