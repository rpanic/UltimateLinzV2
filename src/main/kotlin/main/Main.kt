package main

import db.DB
import db.Observable
import helper.DummyTournamentCreator
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
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.RuntimeException

object Main{

    lateinit var jda: JDA

    var prefix: String? = null
    @JvmField
    var isInDeveloperMode = false

    lateinit var generalAnnouncementChannel: GeneralAnnouncementChannel

    fun main(args: Array<String>) {

        if(args.size >= 2){
            if(args[0].startsWith("-dev")){
                isInDeveloperMode = args[1].toBoolean()
            }
        }

        DB.primaryBackend = JsonBackend()

        prefix = if (isInDeveloperMode) "?" else "!"

        jda = JDABuilder(getToken())
            .build()

        jda.presence.setPresence(Activity.playing("${prefix}help"), false)

        jda.addEventListener(EventListener { e ->
            if(e is ReadyEvent) {

                println("API is ready")

                generalAnnouncementChannel = DB.getObject("generalAnnouncementChannel"){
                    println("General Announcement channel not existing, creating a new one")

                    val guild = jda.guilds[0]

                    //todo Check if the channel already exists

                    val channel = guild.createTextChannel("Turniere").complete()

                    channel.manager.setParent(guild.getCategoriesByName("Verein", true)[0]).complete()

                    GeneralAnnouncementChannel().apply { channelId = channel.idLong }
                }

                GeneralAnnouncementChannelImposer(generalAnnouncementChannel)

                //INIT
                val tournaments = DB.getList<Tournament>(tournamentDbKey)

                val invalidTournaments = DB.getList<Tournament>("invalidTournaments")

                //Check integrity of the data and throw away invalid records
                tournaments.list().forEach {
                    if(jda.getTextChannelById(it.announcementChannel) == null){
                        invalidTournaments += it
                        tournaments.remove(it)
                        postToDevNotifications("AnnouncementChannel for Tournament ${it.name} got deleted, removing it from active tournaments")
                    }
                }

                tournaments.list().forEach {
                    try {
                        TournamentChangeObserver(it)
                    }catch(e: Exception){
                        println("An exception occured while initializing Tournament ${it.name}")
                        e.printStackTrace()

                        postToDevNotifications(".\nAn exception occured while initializing Tournament ${it.name}\n" +
                                StringWriter().apply { e.printStackTrace(PrintWriter(this)) }
                                    .toString().split("\n").take(4).joinToString("\n"))
                    }
                }

                tournaments.addListener(TournamentListSortObserver())

                startCheckTournamentOverJob()
            }

        })

        jda.addEventListener(TournamentListener())
        jda.addEventListener(WatchListenerAdapter())
        jda.addEventListener(LogonStatisticsListenerAdapter())
        jda.addEventListener(HelpListenerAdapter())
        jda.addEventListener(DummyTournamentCreator())

    }

    private fun getToken(): String {

        val token = DB.getObject<Token>("token"){
            throw FileNotFoundException("token.json not supplied in directory data/. Make sure you set a Bot-Token before you start the Bot")
        }
        return token.token
    }

}

class Token : Observable(){
    var token: String by observable("")
}

fun main(args: Array<String>) {

    Main.main(args)
}