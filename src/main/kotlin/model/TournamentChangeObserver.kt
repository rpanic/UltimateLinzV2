package model

import db.ChangeObserver
import helper.EmoteListener
import helper.SimpleEmoteLimiter
import main.AsciiMessageObserver
import main.Main
import main.nickName
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import watch.linkWatchToLimiter
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.reflect.KProperty

class TournamentChangeObserver(t: Tournament) : ChangeObserver<Tournament>(t){

    var participationEmoteLimiter: SimpleEmoteLimiter

    var announcementChannel: TextChannel

    var eatingEmoteLimiter: SimpleEmoteLimiter? = null

    init {

        if(t.generalAnnouncementChannelMessage == -1L) {
            val msg = Main.jda.getTextChannelById(Main.generalAnnouncementChannel.channelId)!!.sendMessage(buildGeneralInfoMessage())?.complete()
            if (msg != null) {
                t.generalAnnouncementChannelMessage = msg.idLong
                Main.generalAnnouncementChannel.tournamentMessages.add(msg.idLong)
            }
        }

        //Init Emotelimiter
        announcementChannel = Main.jda.getTextChannelById(t.announcementChannel)!!

        val infoM = announcementChannel.retrieveMessageById(t.infoMessage).complete()

        participationEmoteLimiter = SimpleEmoteLimiter(infoM)

        val emotes = Main.jda.emotes.map { Pair(it.name, it) }.toMap()
        participationEmoteLimiter.emotes = listOfNotNull(emotes["in"], emotes["out"])
        participationEmoteLimiter.start(infoM.guild)

        initParticipationTable(participationEmoteLimiter, announcementChannel)

        if(t.eatingEnabled){
            initEating()
        }

        this.all(Tournament::announcementChannel, "")

    }

    private fun initParticipationTable(limiter: SimpleEmoteLimiter, newc: TextChannel) {

        val tableMessage = newc.retrieveMessageById(t.tableMessage).complete()

        limiter.addEmoteListener(object: EmoteListener {

            val messageObserver = AsciiMessageObserver(tableMessage)

            init {

                limiter.getReactionsByUser().forEach { (u, e) ->
                    messageObserver.answerChanged(u.nickName(newc.guild), e.name)
                }
                messageObserver.editMessage()
            }

            override fun emoteAdd(e: MessageReactionAddEvent, limiter: SimpleEmoteLimiter) {
                println("EmoteAdd ${e.user} ${e.reactionEmote.name}")
                messageObserver.answerChanged(e.user.nickName(tableMessage.guild), limiter.getReactionsByUser()[e.user]?.name ?: "none")
            }

            override fun emoteRemove(e: MessageReactionRemoveEvent, limiter: SimpleEmoteLimiter) {
                println("EmoteRemove ${e.user} ${e.reactionEmote.name}")
                messageObserver.answerChanged(e.user.nickName(tableMessage.guild), limiter.getReactionsByUser()[e.user]?.name ?: "none")
            }
        })

        linkWatchToLimiter(limiter)
    }

    fun eatingEnabled(new: Any){

        if(new is Boolean){

            val channel = Main.jda.getTextChannelById(t.announcementChannel)!!

            if(new){

                val eatingM = channel.sendMessage("Fleisch / Veggie").complete()
                eatingM.pin().complete()
                t.eatingMessage = eatingM.idLong

                println("Init eating")

                initEating()

            }else{

                var message: Message? = null
                if(t.eatingMessage > 0 && channel.retrieveMessageById(t.eatingMessage).complete().apply { message = this } != null){

                    message!!.delete().complete()

                }

            }
        }
    }

    fun status(new: Any){

        //Alle Teilnehmer benachrichtigen

        val status = new as? TournamentStatus
        if(status == TournamentStatus.SIGNED_UP) {

        }
        

    }

    fun all(prop: KProperty<*>, new: Any){

        //Update Info Message
        println("Tournament ${t.name} changed ${prop.name} -> $new")

        val channel = Main.jda.getTextChannelById(t.announcementChannel)

        if (channel != null) {
            val message = channel.retrieveMessageById(t.infoMessage).complete()

            if (message != null) {

                message.editMessage(buildInfoMessage()).complete()
//                message.editMessage(buildEmbed()).complete()

            } else {
                System.err.println("TournamentChangeListener - Message not found")
                Thread.dumpStack()
            }

        } else {
            System.err.println("TournamentChangeListener - Channel not found")
        }



    }

    fun initEating(){

        if(t.eatingEnabled){

            var newc = Main.jda.getTextChannelById(t.announcementChannel)

            val eatingM = newc!!.retrieveMessageById(t.eatingMessage).complete()

            eatingEmoteLimiter = SimpleEmoteLimiter(eatingM)
            val emotes = Main.jda.emotes.map { Pair(it.name, it) }.toMap()
            eatingEmoteLimiter!!.emotes = listOfNotNull(emotes["meat"], emotes["veggie"])

            eatingEmoteLimiter!!.start(newc.guild)

        }
    }

    private fun buildInfoMessage(): String {

        val dateformat = SimpleDateFormat("dd.MM.yyyy")

        return t.run {

            val fields = mutableMapOf(
                "Status" to status.displayName,
                "Datum" to dateformat.format(dateFrom) + if(dateFrom != dateTo) "-"+dateformat.format(dateTo) else "",
                "Ort" to location,
                "Format" to "$format $division",
                "Playersfee" to playersFee,
                "Deadline Anmeldung" to dateformat.format(registrationDeadline),
                "" to ""
            )

            if(schedule != null) fields += "Schedule" to schedule
            if(playersinfo != null) fields += "Playersinfo" to playersinfo
            if(ucLink != null) fields += "Ultimate Central" to ucLink
            if(comment != null) fields += "Kommentar" to comment

            val builder = StringBuilder()

            fields.entries.forEach { entry ->
                if(entry.key != ""){
                    builder.append("**${entry.key}**: ${entry.value}\n")
                }else{
                    builder.append("\n")
                }
            }
            builder.toString()

        }

        //TODO Include Image from UltimateCentral automatically
    }

    private fun buildGeneralInfoMessage(): Message {

        return MessageBuilder()
            .append(".\n")
            .append(Main.jda.getTextChannelById(t.announcementChannel) as IMentionable)
            .append("\n" + buildInfoMessage() + "\n")
            .build()

    }

    private fun buildEmbed(): MessageEmbed {

        val dateformat = SimpleDateFormat("dd.MM.yyyy")

        return t.run {
            val builder = EmbedBuilder()
                .setColor(Color.BLUE) //TODO Change that depending on the season of the tournament
                .setTitle(t.name)
                .also {

                    val fields = mutableMapOf(
                        "Datum" to dateformat.format(dateFrom) + if(dateFrom != dateTo) "-"+dateformat.format(dateTo) else "",
                        "Ort" to location,
                        "Format" to "$format $division",
                        "Teamfee" to teamFee,
                        "Playersfee" to playersFee,
                        "Deadline Anmeldung" to dateformat.format(registrationDeadline),
                        "Deadline Zahlung" to dateformat.format(paymentDeadline)
                    )

                    if(schedule != null) fields += "Schedule" to schedule
                    if(playersinfo != null) fields += "Playersinfo" to playersinfo
                    if(ucLink != null) fields += "Ultimate Central" to ucLink
                    if(comment != null) fields += "Kommentar" to comment

                    fields.entries.forEach { entry ->
                        it.addField(entry.key, entry.value, false)
                    }

                }
                .setTimestamp(Instant.now())
            builder.build()
        }

        //TODO Include Image from UltimateCentral automatically
    }

    fun name(new: Any){

        if(new is String){
            Main.jda.getTextChannelById(t.announcementChannel)?.manager?.setName(new.replace(' ' , '-'))?.complete()
        }

    }

}