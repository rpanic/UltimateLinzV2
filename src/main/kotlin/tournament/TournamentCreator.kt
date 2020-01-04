package tournament

import db.DB
import main.parseDateLazy
import model.Tournament
import model.TournamentChangeObserver
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

import java.lang.reflect.InvocationTargetException
import java.text.ParseException
import java.util.ArrayList

const val tournamentDbKey = "tournaments"

class TournamentCreator(internal var channel: PrivateChannel) : ListenerAdapter() {

    private val arr = arrayOf(
        "Wie heißt das Turnier?",
        "Wann ist das Turnier? (Start)",
        "Wann ist das Turnier? (Ende)",
        "Wo ist das Turnier?",
        "In welchem Format?",
        "Mixed, Open, Women?",
        "Teamfee?",
        "Playersfee?",
        "Registration Deadline?",
        "Payment Deadline?",
        "Ultimate Central Link?",
        "Soll die Essensabstimmung sofort freigeschaltet werden?"
    )
    internal var values: Array<String?>

    internal var position = 0

    init {
        this.values = arrayOfNulls(arr.size)
    }

    fun create() {
        channel.jda.addEventListener(this)
        nextQuestion()
    }

    fun createAction(guild: Guild, t: Tournament) {

        val c = guild.getCategoriesByName("Turniere", true).get(0)
        val channels = c.getChannels()
        val last = if (channels.size > 0) channels.get(channels.size - 1).getPosition() else 0

        val newc = c.createTextChannel(t.name!!).complete() as TextChannel

        //Permissions

        Thread {

            try {
                Thread.sleep(12000L)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            val roles = guild.getRolesByName("Vereinsmitglied", true)
            println(roles.toString())
            if (roles.size > 0) {

                val permissions = listOf(
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.VIEW_CHANNEL,
                        Permission.MESSAGE_WRITE,
                        Permission.MESSAGE_READ,
                        Permission.MESSAGE_TTS,
                        Permission.MESSAGE_EMBED_LINKS,
                        Permission.MESSAGE_ATTACH_FILES,
                        Permission.MESSAGE_EXT_EMOJI
                    )

                val raw = Permission.getRaw(permissions)

                roles.forEach { y ->

                    newc.manager.putPermissionOverride(y, raw, 0).complete()

                }
            }
        }.start()

        //Give the bot the power he deserves
        val bot = guild.getRolesByName("Bot", true).stream().findFirst().orElse(null)
        if (bot != null) {
            newc.manager.putPermissionOverride(
                bot,
                Permission.ALL_CHANNEL_PERMISSIONS or Permission.ALL_TEXT_PERMISSIONS,
                0
            ).complete()
        }

        val pinremover = PinMessageRemoveListener(newc)

        guild.jda.addEventListener(pinremover)

        //Create Info post
        var m = newc.sendMessage("Placeholder").complete()
        t.infoMessage = m.idLong

        val tableMessage = newc.sendMessage("AttendanceTable").complete()
        t.tableMessage = tableMessage.idLong

        //Create eating message
        if (t.eatingEnabled) {
            val eatingM = newc.sendMessage("Fleisch / Veggie").complete()
            t.eatingMessage = eatingM.idLong
            eatingM.pin().complete()
        }

        m.pin().complete() //Pin here, so the Info Message appears on the top

        t.announcementChannel = newc.idLong

        newc.guild.modifyTextChannelPositions().selectPosition(newc).moveTo(last + 1)

        TournamentChangeObserver(t)

    }

    fun nextQuestion() {

        if (position >= arr.size) {

            //Creation
            val t = Tournament()

            for (i in fields.indices) {

                parseFieldInto(
                    t,
                    values[i],
                    fields[i]
                )

            }

            channel.sendMessage("Das Turnier wurde erstellt!").complete()
            channel.sendMessage(t.toString()).complete()
            channel.jda.removeEventListener(this)

            t.id = DB.getList<Tournament>(tournamentDbKey).list().map{ x -> x.id}.max() ?: -1 + 1;

            createAction(channel.jda.guilds[0], t)

            DB.getList<Tournament>(tournamentDbKey).add(t)

        } else {

            channel.sendMessage(arr[position]).complete()

        }
    }

    override fun onPrivateMessageReceived(event: PrivateMessageReceivedEvent) {
        super.onPrivateMessageReceived(event)

        if (event.author != event.jda.selfUser) {

            val content = event.message.contentRaw
            values[position] = content

            position++

            nextQuestion()

        }
    }

    companion object {
        val fields = arrayOf(
            "name",
            "dateFrom",
            "dateTo",
            "location",
            "format",
            "division",
            "teamFee",
            "playersFee",
            "registrationDeadline",
            "paymentDeadline",
            "uclink",
            "eatingEnabled"
        )
        val nonCreationFields = arrayOf("schedule", "playersinfo", "comment")
        val translations = arrayOf(  //Have to be "creationfields" first
            "name-Name des Turniers",
            "start-Start des Turniers",
            "end-Ende des Turniers",
            "ort-Ort des Turniers",
            "format-Format (z.B. 5v5 Continous)",
            "division-Division (Mixed, Women, Open, Master)",
            "teamfee-Teamfee",
            "playersfee-Playersfee",
            "registrationdeadline-Deadline zur Registrierung",
            "paymentdeadline-Deadline zur Teamfeezahlung",
            "link-Ultimate Central Link",
            "eating-Essensabstimmung freigeschaltet",
            "schedule-Schedule",
            "playersinfo-Playersinfo",
            "comment-Freitext für zusätzliche Infos"
        )

        fun getFieldFromLabel(label: String): String? {

            for (i in translations.indices) {

                if (translations[i] == label || translations[i].split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] == label) {

                    return if (fields.size <= i)
                        nonCreationFields[i - fields.size]
                    else
                        fields[i]

                }
            }
            return null

        }

        fun parseFieldInto(t: Tournament, value: String?, field: String): Boolean {

            for (m in Tournament::class.java.methods) {

                if (m.name.equals("set$field", ignoreCase = true)) {

                    println(field)

                    try {

                        if (m.parameters[0].type.name == Long::class.javaPrimitiveType!!.name) {

                            if(value != null){
                                m.invoke(t, parseDateLazy(value).time)
                            }
                        } else if (m.parameters[0].type.name == Boolean::class.javaPrimitiveType!!.name) {

                            if(value != null){
                                val booleaned = value.run {
                                    when {
                                        startsWith("y") -> true
                                        startsWith("j") -> true
                                        startsWith("t") -> true
                                        startsWith("n") -> false
                                        startsWith("f") -> false
                                        else -> null
                                    }
                                }
                                if(booleaned != null)
                                    m.invoke(t, booleaned)
                                else
                                    throw java.lang.IllegalArgumentException("$value ist kein valider Ja / Nein Ausdruck")
                            }

                        } else {
                            m.invoke(t, value)
                        }
                        return true

                    } catch (e: IllegalAccessException) {
                        e.printStackTrace()
                    } catch (e: IllegalArgumentException) {
                        e.printStackTrace()
                    } catch (e: InvocationTargetException) {
                        e.printStackTrace()
                    } catch (e: ParseException) {
                        e.printStackTrace()
                    }

                }
            }
            return false
        }
    }
}