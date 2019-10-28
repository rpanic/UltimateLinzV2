package model

import com.beust.klaxon.Json
import db.Observable

class GeneralAnnouncementChannel()
    : Observable() {

    var channelId: Long by observable(-1)
    var tournamentMessages: MutableList<Long> by observable(mutableListOf())

    @Json(ignored = true)
    var change: Int by observable(0)

    fun addMessage(id: Long){
        tournamentMessages.add(id)
        change++
    }

}