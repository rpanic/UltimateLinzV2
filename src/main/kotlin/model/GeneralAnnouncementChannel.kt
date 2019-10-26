package model

import db.Observable

class GeneralAnnouncementChannel()
    : Observable() {

    var channelId: Long by observable(-1)
    val tournamentMessages: List<Long> by observable(mutableListOf())

}