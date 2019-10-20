package model

import db.ChangeObserver
import main.Main
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color
import java.text.SimpleDateFormat
import java.time.Instant
import kotlin.reflect.KProperty

class TournamentChangeObserver(t: Tournament) : ChangeObserver<Tournament>(t){

    fun all(new: Any){

        //Update Info Message
        println("Tournament ${t.name} changed")

        val channel = Main.jda.getTextChannelById(t.announcementChannel)

        if (channel != null) {
            val message = channel.retrieveMessageById(t.infoMessage).complete()

            if (message != null) {

                message.editMessage(buildInfoMessage()).complete()
//                message.editMessage(buildEmbed()).complete()

            } else {
                System.err.println("TournamentChangeListenere - Message not found")
                Thread.dumpStack()
            }

        } else {
            System.err.println("TournamentChangeListenere - Channel not found")
        }

    }

    private fun buildInfoMessage(): String {

        val dateformat = SimpleDateFormat("dd.MM.yyyy")

        return t.run {

            val fields = mutableMapOf(
                "Datum" to dateformat.format(dateFrom) + if(dateFrom != dateTo) "-"+dateformat.format(dateTo) else "",
                "Ort" to location,
                "Format" to "$format $division",
                "Teamfee" to teamFee,
                "Playersfee" to playersFee,
                "Deadline Anmeldung" to dateformat.format(registrationDeadline),
                "Deadline Zahlung" to dateformat.format(paymentDeadline),
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