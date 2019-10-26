package main

import db.DB
import db.Observable
import db.TournamentListener
import model.GeneralAnnouncementChannel
import model.GeneralAnnouncementChannelImposer
import model.Tournament
import model.TournamentInit
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import tournament.tournamentDbKey

object Main{

    lateinit var jda: JDA

    var prefix: String? = null
    @JvmField
    var isInDeveloperMode = true

    fun main(args: Array<String>) {

        prefix = if (isInDeveloperMode) "?" else "!"

        jda = JDABuilder(getToken())
            .build()

        jda.presence.setPresence(Activity.playing("${prefix}help"), false)

        jda.addEventListener(EventListener {
            if(it is ReadyEvent) {
                println("API is ready")

                val generalAnnouncementChannel = DB.getObject<GeneralAnnouncementChannel>("generalAnnouncementChannel"){
                    val guild = jda.guilds[0]

                    //todo Check if the channel already exists

                    val channel = guild.createTextChannel("Turniere").complete()

                    channel.manager.setParent(guild.getCategoriesByName("Verein", true)[0]).complete()

                    GeneralAnnouncementChannel().apply { channelId = channel.idLong }
                }

                GeneralAnnouncementChannelImposer(generalAnnouncementChannel)

                //INIT
                DB.getList<Tournament>(tournamentDbKey).list().forEach {
                    TournamentInit.init(it)
                }
            }

        })

        jda.addEventListener(TournamentListener())

    }

    private fun getToken(): String {

        val fallbacktoken = "NjI3NTgyMTUyMTI4NzI1MDE1.XY-wtQ.l5iLyVPVcD350-xudZHh8ygFG-4"

        val token = DB.getObject("token"){
            Token(fallbacktoken)
        }
        return token.token
    }

}

class Token(inital: String) : Observable(){
    var token: String by observable(inital)
}

fun main(args: Array<String>) {

    DB.getList<Token>("Token")

    Main.main(args)
}