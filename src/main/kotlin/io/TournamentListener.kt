package io

import db.DB
import main.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import tournament.TournamentCreator
import main.Permissioned
import tournament.tournamentDbKey
import main.Prompt
import java.util.*
import model.Tournament
import model.TournamentStatus
import java.lang.RuntimeException
import kotlin.contracts.contract

@Help("Alle Commands zur Turnierinfo und -verwaltung")
class TournamentListener : ListenerAdapterCommand("${Main.prefix}t") {

    val padding = 12

    @Permissioned("Vorstand", "Moderator")
    @Help("Erstellt ein neues Turnier")
    fun create(event: MessageReceivedEvent, args: Array<String>){

        val channel: PrivateChannel

        if (event.channel.type !== ChannelType.PRIVATE) {

            val response = "Der Erstellungsprozess wird als Privatnachricht fortgesetzt"

            event.channel.sendMessage(response).complete()

            channel = event.author.openPrivateChannel().complete()

        } else {
            channel = event.privateChannel
        }

        TournamentCreator(channel).create()
    }

    @Permissioned("Vorstand", "Moderator")
    @Blocking
    @Help("Ändert die Infos eines Turniers")
    fun edit(event: MessageReceivedEvent, msg: Array<String>) {

        var prompt = "Folgende Felder stehen zur Auswahl: \n"
        for (field in TournamentCreator.translations) {

            val arr = field.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            prompt += String.format(" - %s: %s\n", arr[0], arr[1])

        }
        prompt += "Welches Infofeld willst du ändern?"


        val definition = tournamentCommandDefinition("edit", prompt)
        val args = getInputData(event.message, definition, msg)

        if(args.error != null){
            send(event.channel, args.error)
            return
        }

        var parameter = args.args!!.parameters[0]

        val existsCheck = {param: String ->
            TournamentCreator.translations
            .map { x -> x.split("-").dropLastWhile { it.isEmpty() }.first() }
            .any { x -> x.startsWith(param, true) }
        }

        if (!existsCheck(parameter)) {

            parameter = Prompt(
                "Feld existiert nicht - du kannst nochmal probieren",
                event.channel,
                event.author
            ).promptSync() ?: throw RuntimeException("Something went wrong while waiting for user input")

            if (!existsCheck(parameter)) {
                send(event.channel, "Abbruch")
                return
            }

        }

        val value = Prompt("Neuer Wert?", event.channel, event.author).promptSync()

        TournamentCreator.parseFieldInto(args.args.tournament, value, TournamentCreator.getFieldFromLabel(parameter)!!)

        sendSync(event.channel, "Feld $parameter gesetzt auf $value")

    }

    @Permissioned("Vorstand", "Moderator")
    @Blocking
    @Help("Ändert den Status eines Turniers")
    fun status(event: MessageReceivedEvent, msg: Array<String>){

        val statuses = TournamentStatus.values().map { it.displayName }.joinToString(", ")
        val definition = tournamentCommandDefinition("status", "Was ist der neue Status? $statuses")
        val args = getInputData(event.message, definition, msg)

        if(args.error != null){
            send(event.channel, args.error)
        }else{
            args.args!!.apply {
                val status = TournamentStatus.values().maxBy { it.displayName.similarity(parameters[0]) }
                if(status != null) {
                    tournament.status = status
                    send(event.channel, "Status von ${tournament.name} zu ${tournament.status.displayName} gesetzt")
                }else{
                    send(event.channel, "Status nicht gefunden")
                }
            }
        }

    }

    @Permissioned("Vorstand", "Moderator")
    @Blocking
    @Help("Archiviert ein Turnier")
    fun archive(event: MessageReceivedEvent, msg: Array<String>) {

        val tournament = getTournament(msg, 2, event.message)

        Thread {

            if (msg.size >= 2) {

                if (tournament == null) {
                    println("Tournament null!")
                    return@Thread
                }

                var response = Prompt(
                    "Do you really want to archive " + tournament.name + "?",
                    event.channel,
                    event.author
                ).promptSync()

                response = response!!.toLowerCase()

                if (response.yesNo() == true) {

                    //Removal operations

                    val channel = event.jda.getTextChannelById(tournament.announcementChannel)

                    val guild = event.jda.guilds[0]

                    val c = guild.getCategoriesByName("Turnierarchiv", true).stream().findFirst().orElse(null)

                    if (c == null) {
                        println("Category Archive is not created!!")
                        return@Thread
                    }

                    val channels = c.channels

                    if(channel != null) {

                        println("${channel.positionRaw},${channel.position}")

                        val position =
                            if (channels.size > 0) channels[channels.size - 1].positionRaw + 1 else c.positionRaw
                        channel.manager.setParent(c)
                        channel.manager.setPosition(position)
                        //guild.getController().modifyTextChannelPositions().selectPosition(channel).moveTo(position).complete();

                        //JsonModel.getInstance().tournaments().remove(tournament);
                        val cal = Calendar.getInstance()
                        cal.time = Date(tournament.dateFrom)

                        tournament.name = tournament.name + "-" + cal.get(Calendar.YEAR)
                        channel.manager.setName(tournament.name.replace(" ", "-")).complete()

                        tournament.status = TournamentStatus.ARCHIVED

                        send(event.channel, "Tournament " + tournament.name + " archived!")
                    }

                } else {
                    send(event.channel, "Archivation aborted")
                }

                System.out.println("Archived $tournament")

            }

        }.start()

    }

    @Help("Listet alle erstellten Turniere")
    fun list(event: MessageReceivedEvent, msg: Array<String>) {

        val tournaments = DB.getList<Tournament>(tournamentDbKey).list()

        System.out.println(tournaments.size)

        val s = "Created Tournaments: \n" + tournaments
            .sortedBy { it.dateFrom }
            .map(Tournament::name)
            .has( {size > 0} , "Keine Turniere zurzeit verfuegbar"){ reduce { x, y -> x + "\n" + y } }

        event.channel.sendMessage(MessageBuilder().appendCodeBlock(s, "").build()).complete()

    }

    @Help("Füge einen nicht registriertern Spieler zu einem Turnier hinzu")
    fun player(event: MessageReceivedEvent, msg: Array<String>) {

        val definition = tournamentCommandDefinition("player", "Wie ist der Name des Spielers?")
        val args = getInputData(event.message, definition, msg)

        if(args.error != null){
            send(event.channel, args.error)
        }else{

            

        }

    }

    //TODO
//    @Permissioned("Vorstand", "Moderator")
//    fun activateEating(event: MessageReceivedEvent?, msg: Array<out String>?){
//
//    }

    /*private fun parseDataWithTournament(args: Array<String>, channel: PrivateChannel): Pair<Tournament, Array<String>>{

        val tournaments = DB.getList<Tournament>(tournamentDbKey).list()

        var arg = args.maxBy { s -> tournaments.map { it.nameSimilarity(s) }.max() ?: 0 } ?: return Optional.empty()

        var t = tournaments.maxBy { it.nameSimilarity(arg) } ?: null

        if(t?.nameSimilarity(arg) ?: 0 < 4){
            val tournamentName = Prompt("Welches Turnier?", channel, channel.user).promptSync()
        }

        val args2 = args.toMutableList()
        args2.remove(arg)

        return Pair(t, args2.toTypedArray())

    }

    private fun Tournament.nameSimilarity(s: String) : Int {

        val s1 = this.name.toLowerCase()
        val s2 = s.toLowerCase()

        return s2.length * if(s1.contains(s2)) 1 else 0

    }*/

    private fun getInputData(message: Message, tournamentCommandDefinition: TournamentCommandDefinition, args: Array<String>) =
        TournamentCommandParser(message.channel, message.author)
            .getArgs(tournamentCommandDefinition, args.toList())


    private fun getTournament(args: Array<String>, index: Int, msg: Message): Tournament? {

        val findByName = {name: String ->
            DB.getList<Tournament>(tournamentDbKey).list()
                .firstOrNull { x -> x.name.toLowerCase().startsWith(name.toLowerCase()) }
        }

        if (args.size > index) {

            val name = args[index]
            return findByName(name)

        }

        return Prompt("Welches Turnier?", msg.channel, msg.author)
            .promptSync()?.let { findByName(it) }

    }

    override fun help(event: MessageReceivedEvent, msg: Array<out String>?) {

        val info = ("""|
            Mit \"${this.cmd}\" erstellst und verwaltest du Turniere
            Standardschema:
            `${this.cmd} command <turnier> <argumente>`
            Alle verfügbaren Optionen:""".trimIndent())

        val commands = getHelp(event.jda.guilds[0], event.author)

        val messageBuilder = MessageBuilder()
        messageBuilder.append(info)
        messageBuilder.appendCodeBlock(commands, "")

        send(event.channel, messageBuilder.build())

        println("help")
    }

}

