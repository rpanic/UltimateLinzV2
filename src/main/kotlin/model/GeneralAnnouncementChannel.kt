package model

import db.DBSingleton

data class GeneralAnnouncementChannel(val channelId: Long, val tournamentMessages: List<Long>)
    : DBSingleton() {



}