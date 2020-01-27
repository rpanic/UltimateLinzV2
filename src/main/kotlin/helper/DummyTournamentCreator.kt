package helper

import main.*
import model.Tournament
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.lang3.RandomStringUtils
import tournament.TournamentCreator

class DummyTournamentCreator : ListenerAdapterCommand("dummy"){

    @Permissioned("Moderator")
    @Blocking
    fun createDummy(event: MessageReceivedEvent, args: Array<String>){

        if(args.size < 3){
            send(event.channel, "1 Argument benötigt (Datum)")
        }else{

            val date = parseDateLazy(args[2]).time

            val t = Tournament().apply {
                name = "Dummy${RandomStringUtils.randomAlphanumeric(6)}"
                dateFrom = date
                dateTo = date + (24*60*60*1000)
                location = "Linz"
                format = "5v5"
                division = "Mixed"
                teamFee = "251247427"
                playersFee = "12525125"
                registrationDeadline = date
                paymentDeadline = date
                ucLink = "asd"
                eatingEnabled = true
            }

            TournamentCreator(event.privateChannel).createAction(Main.jda.guilds[0], t) //TODO evntl createAction rausziehen in eine Init Klasse. Würde auch Kontextmäßig besser reinpassen

        }
    }

}