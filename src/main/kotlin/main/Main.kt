package main

import db.DB
import db.DBSingleton
import db.TournamentListener
import model.Tournament
import model.TournamentInit
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import tournament.TournamentCreator
import tournament.tournamentDbKey
import java.util.*

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
            if(it is ReadyEvent)
                println("API is ready")
        })

        jda.addEventListener(TournamentListener())

        DB.getList<Tournament>(tournamentDbKey).list().forEach {
            TournamentInit.init(it)
        }

    }

    private fun getToken(): String {

        val fallbacktoken = "NjI3NTgyMTUyMTI4NzI1MDE1.XY-wtQ.l5iLyVPVcD350-xudZHh8ygFG-4"

        val token = DB.getObject("token"){
            Token("none")
        }

        return if(token.token == "none"){
            fallbacktoken
        }else {
            token.token
        }

    }

}

data class Token(val token: String) : DBSingleton()

fun main(args: Array<String>) {

    DB.getList<Token>("Token")

    Main.main(args)
}