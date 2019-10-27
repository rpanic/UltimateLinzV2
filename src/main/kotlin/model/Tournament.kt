package model

import com.sun.org.apache.xpath.internal.operations.Bool
import db.Observable
import java.text.SimpleDateFormat

class Tournament : Observable(){

    var id: Int                 by observable(0)
    var name: String?           by observable(null)
    var dateFrom: Long          by observable(0)
    var dateTo: Long            by observable(0)
    var location: String?       by observable(null)
    var format: String?         by observable(null)
    var division: String?       by observable(null)
    var teamFee: String?        by observable(null)
    var playersFee: String?     by observable(null)
    var registrationDeadline: Long  by observable(0)
    var paymentDeadline: Long       by observable(0)
    var schedule: String?           by observable(null)
    var playersinfo: String?        by observable(null)
    var ucLink: String?             by observable(null)
    var comment: String?            by observable(null)

    var announcementChannel: Long   by observable(-1)
    var discussionChannel: Long     by observable(-1)

    var infoMessage: Long           by observable(-1)
    var tableMessage: Long          by observable(-1)

    var eatingEnabled: Boolean      by observable(false)
    var eatingMessage: Long         by observable(-1)

    override fun toString(): String {
        return "Tournament(id=$id, name=$name, dateFrom=$dateFrom, dateTo=$dateTo, location=$location, format=$format, division=$division, teamFee=$teamFee, playersFee=$playersFee, registrationDeadline=$registrationDeadline, paymentDeadline=$paymentDeadline, schedule=$schedule, playersinfo=$playersinfo, ucLink=$ucLink, comment=$comment, announcementChannel=$announcementChannel, discussionChannel=$discussionChannel, infoMessage=$infoMessage, tableMessage=$tableMessage, eatingMessage=$eatingMessage, eatingEnabled=$eatingEnabled)"
    }

}