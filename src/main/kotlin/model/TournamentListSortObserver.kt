package model

import db.DB
import db.ElementChangeType
import db.ElementChangedListener
import main.Main
import tournament.tournamentDbKey

class TournamentListSortObserver : ElementChangedListener<Tournament> {

    override fun invoke(changeType: ElementChangeType, t: Tournament) {

        println("ListChanged $changeType ${t.name}")
        sort()

    }

    fun sort(){

        val category = Main.jda.getCategoriesByName("Turniere", true).firstOrNull()

        if(category == null){
            println("Channelcategory is null - check that the name is `Turniere`")
            return
        }

        val tournaments = DB.getList<Tournament>(tournamentDbKey)

        val map = category.textChannels
            .filter { it.parent == category }
            .map { it to tournaments.firstOrNull { t -> t.announcementChannel == it.idLong } }
            .filter {
                if(it.second == null) {
                    println("Orphan Channel ${it.first.name} in Category `Turniere` found")
                    false
                }else true
            }
            .map { it.first to it.second!! }

        map.sortedBy { it.second.dateFrom }.forEachIndexed { index, pair ->
            if(index != pair.first.position) {
                pair.first.manager.setPosition(index).complete()
            }
        }

    }

}