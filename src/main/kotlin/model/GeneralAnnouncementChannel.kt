package model

import observable.Observable

class GeneralAnnouncementChannel()
    : Observable() {

    var channelId: Long by observable(-1)
    var tournamentMessages by observableList<Long>()

}