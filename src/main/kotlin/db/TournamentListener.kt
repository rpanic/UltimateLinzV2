package db

import main.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import model.TournamentInit
import net.dv8tion.jda.api.Permission
import tournament.PinMessageRemoveListener
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.*
import tournament.TournamentCreator
import main.Permissioned
import tournament.tournamentDbKey
import net.dv8tion.jda.api.entities.TextChannel
import main.Prompt
import java.util.*
import model.Tournament

class TournamentListener : ListenerAdapterCommand("${Main.prefix}t") {

    @Permissioned("Vorstand", "Moderator")
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
    fun edit(event: MessageReceivedEvent, msg: Array<String>) = editinfo(event, msg)

    @Permissioned("Vorstand", "Moderator")
    @Blocking
    fun editinfo(event: MessageReceivedEvent, msg: Array<String>) {

        //TODO Argument 2 und 3 funktionieren nicht richtig
        val tournament = getTournament(msg, 3, event.message)

        if (tournament == null) {
            send(event.channel, "Turnier nicht gefunden!")
            return
        }

        var parameter: String? = null

//        deleteCommandAfter(30000)

        if (msg.size == 2) {

            var prompt = "Folgende Felder stehen zur Auswahl: \n"
            for (field in TournamentCreator.translations) {

                val arr = field.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

                prompt += String.format(" - %s: %s\n", arr[0], arr[1])

            }
            prompt += "Welches Infofeld willst du ändern?"

            parameter = Prompt(prompt, event.channel, event.author).promptSync() //setDelete(30000).

            println("Param: $parameter")

            var exists = TournamentCreator.translations
                .map { x -> x.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                .any { x -> x.startsWith(parameter!!.toLowerCase()) }

            if (!exists) {

                parameter = Prompt(
                    "Feld existiert nicht - du kannst nochmal probieren",
                    event.channel,
                    event.author
                ).promptSync() //.setDelete(30000)

                val finalParameter2 = parameter

                exists = Arrays.stream(TournamentCreator.translations)
                    .map { x -> x.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] }
                    .anyMatch { x -> x.startsWith(finalParameter2!!.toLowerCase()) }

                if (!exists) {
                    val m = event.channel.sendMessage("Abbruch").complete()
//                    MessageTimer.deleteAfter(m, 20000)
                    return
                }

            }

        } else if (msg.size > 2) {

            parameter = Arrays.stream(Arrays.copyOfRange(msg, 2, msg.size)).reduce { x, y -> "$x $y" }.orElse(null)

        }

        val value = Prompt("Neuer Wert?", event.channel, event.author).promptSync() //.setDelete(30000)

        TournamentCreator.parseFieldInto(tournament, value, TournamentCreator.getFieldFromLabel(parameter!!)!!)

        /*MessageTimer.deleteAfter(*/sendSync(event.channel, "Feld $parameter gesetzt auf $value")//, 30000);

    }

    @Permissioned("Vorstand", "Moderator")
    @Blocking
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

                if (response.startsWith("y") || response.startsWith("j")) {


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
                        channel.manager.setName(tournament.name?.replace(" ", "-") ?: "").complete()

                        send(event.channel, "Tournament " + tournament.name + " archived!")
                    }

                } else {
                    send(event.channel, "Archivation aborted")
                }

                System.out.println("Archived $tournament")

            }

        }.start()

    }

    fun list(event: MessageReceivedEvent, msg: Array<String>) {

        val tournaments = DB.getList<Tournament>(tournamentDbKey).list()

        System.out.println(tournaments.size)

        val s = "Created Tournaments: \n" + tournaments
            .sortedBy { it.dateFrom }
            .map(Tournament::name)
            .has( {size > 0} , "Keine Turniere zurzeit verfuegbar"){ reduce { x, y -> x + "\n" + y } }

        event.channel.sendMessage(MessageBuilder().appendCodeBlock(s, "").build()).complete()

    }

    //TODO
//    @Permissioned("Vorstand", "Moderator")
//    fun activateEating(event: MessageReceivedEvent?, msg: Array<out String>?){
//
//    }

    private fun getTournament(args: Array<String>, index: Int, msg: Message): Tournament? {

        val findByName = {name: String ->
            DB.getList<Tournament>(tournamentDbKey).list()
                .filter{ x -> x.name?.toLowerCase()?.startsWith(name.toLowerCase()) ?: false }
                .firstOrNull()
        }

        if (args.size > index) {

            val name = args[index]
            return findByName(name)

        }

        return Prompt("Welches Turnier?", msg.channel, msg.author)
            .promptSync()?.let { findByName(it) }

    }

    override fun help(event: MessageReceivedEvent?, msg: Array<out String>?) {
        val padding = 12

        val info = ("Mit \"${this.cmd}\" erstellst und verwaltest du Turniere\n"
                + "Alle verfügbaren Optionen:\n")

        val commands = ("help".padRight(padding) + "Ruft die Hilfe auf\n"
                + "create".padRight(padding) + "Erstellt ein neues Turnier\n"
                + "info [x]".padRight(padding) + "Infos zu aktuellen Turnier [x = Turniername]\n"
                + "editinfo".padRight(padding) + "Ändert die Infos eines Turniers\n"
                + "list".padRight(padding) + "Listet alle erstellten Turniere\n"
                + "archive".padRight(padding) + "Archiviert ein Turnier\n")

        val messageBuilder = MessageBuilder()
        messageBuilder.append(info)
        messageBuilder.appendCodeBlock(commands, "")

        send(event!!.channel, messageBuilder.build())

        println("help")
    }

}

