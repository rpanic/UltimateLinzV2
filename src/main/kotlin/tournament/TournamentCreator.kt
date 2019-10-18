package tournament

import db.DB
import main.parseDateLazy
import model.Tournament
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

import java.lang.reflect.InvocationTargetException
import java.text.ParseException
import java.text.SimpleDateFormat

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
        "Ultimate Central Link?"
    )
    internal var values: Array<String?>

    internal var position = 0

    internal var consumer: (Tournament) -> Unit = {}

    init {
        this.values = arrayOfNulls(arr.size)
    }

    fun create(consumer: (Tournament) -> Unit) {
        this.consumer = consumer
        channel.jda.addEventListener(this)
        nextQuestion()
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

            consumer(t)

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
            "uclink"
        )
        val nonCreationFields = arrayOf("schedule", "playersinfo")
        val translations = arrayOf(
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
            "schedule-Schedule",
            "playersinfo-Playersinfo"
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