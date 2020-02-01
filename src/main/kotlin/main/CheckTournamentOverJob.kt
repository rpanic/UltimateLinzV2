package main

import db.DB
import model.Tournament
import model.TournamentStatus
import tournament.tournamentDbKey
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun startCheckTournamentOverJob(){
    val scheduler = Executors.newScheduledThreadPool(1)
    scheduler.scheduleAtFixedRate({

        DB.getList<Tournament>(tournamentDbKey).forEach {
            if(it.dateTo < System.currentTimeMillis() &&
                    it.status !in listOf(TournamentStatus.NOT_SIGNED_UP, TournamentStatus.NO_SPOT, TournamentStatus.OVER)){
                it.status = TournamentStatus.OVER
            }
        }

    }, 0, 24, TimeUnit.HOURS)
}