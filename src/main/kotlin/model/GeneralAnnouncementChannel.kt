package model

import db.Observable

class GeneralAnnouncementChannel()
    : Observable() {

    var channelId: Long by observable(-1)
    var tournamentMessages: MutableList<Long> by observableList()

}