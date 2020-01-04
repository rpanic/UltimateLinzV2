package main

import db.DB
import db.Observable
import io.TournamentListener
import json.JsonBackend
import model.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.hooks.EventListener
import tournament.tournamentDbKey
import watch.LogonStatisticsListenerAdapter
import watch.WatchListenerAdapter
import java.io.FileNotFoundException

object Main{

    lateinit var jda: JDA

    lateinit var guild: Guild

    var prefix: String? = null
    @JvmField
    var isInDeveloperMode = true

    lateinit var generalAnnouncementChannel: GeneralAnnouncementChannel

    fun main(args: Array<String>) {

        DB.primaryBackend = JsonBackend()

        prefix = if (isInDeveloperMode) "?" else "!"

        jda = JDABuilder(getToken())
            .build()

        jda.presence.setPresence(Activity.playing("${prefix}help"), false)

        jda.addEventListener(EventListener { e ->
            if(e is ReadyEvent) {

                println("API is ready")

                generalAnnouncementChannel = DB.getObject("generalAnnouncementChannel"){
                    val guild = jda.guilds[0]

                    //todo Check if the channel already exists

                    val channel = guild.createTextChannel("Turniere").complete()

                    channel.manager.setParent(guild.getCategoriesByName("Verein", true)[0]).complete()

                    GeneralAnnouncementChannel().apply { channelId = channel.idLong }
                }

                GeneralAnnouncementChannelImposer(generalAnnouncementChannel)

                //INIT
                DB.getList<Tournament>(tournamentDbKey).list().forEach {
                    TournamentChangeObserver(it)
                }
            }

        })

        jda.addEventListener(TournamentListener())
        jda.addEventListener(WatchListenerAdapter())
        jda.addEventListener(LogonStatisticsListenerAdapter())

    }

    private fun getToken(): String {

        val token = DB.getObject<Token>("token"){
            throw FileNotFoundException("token.json not supplied in directory data/. Make sure you set a Bot-Token before you start the Bot")
        }
        return token.token
    }

}

class Token() : Observable(){

    constructor(inital: String) : this(){
        token = inital
    }

    var token: String by observable("")
}

fun main(args: Array<String>) {

    Main.main(args)
}