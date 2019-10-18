package model

import db.Observable
import java.text.SimpleDateFormat

data class Tournament(

    var id: Int,
    var name: String?,
    var dateFrom: Long,
    var dateTo: Long,
    var location: String?,
    var format: String?,
    var division: String?,
    var teamFee: String?,
    var playersFee: String?,
    var registrationDeadline: Long = 0,
    var paymentDeadline: Long = 0,
    var schedule: String? = null,
    var playersinfo: String? = null,
    var ucLink: String? = null,
    var comment: String? = null,

    var announcementChannel: Long = 0,
    var discussionChannel: Long = 0,

    var infoMessage: Long = 0,
    var tableMessage: Long = 0,
    var eatingMessage: Long = 0
){

    constructor() : this(0, null, 0, 0, null, null, null, null, null)

    //%8$td.%8$tm.%8$tY
    fun getInfoMarkup(): String {

        val dateformat = SimpleDateFormat("dd.MM.yyyy")

        var s = String.format(
            "\n**%s**\n"
                    + "**Datum**: ${dateformat.format(dateFrom)}${if(dateFrom != dateTo) "-"+dateformat.format(dateTo) else ""}"
                    + "\n**Ort**: %s\n"
                    + "**Format**: %s %s\n"
                    + "**Teamfee**: %s\n"
                    + "**Playersfee**: %s\n"
                    + "**Deadline Anmeldung**: " + dateformat.format(registrationDeadline) + "\n"
                    + "**Deadline Zahlung**: " + dateformat.format(paymentDeadline) + "\n\n",
            name,
            location,
            format,
            division,
            teamFee,
            playersFee
        )

        s += if (schedule != null) String.format("**Schedule:** %s\n\n", schedule) else ""
        s += if (playersinfo != null) String.format("**Playersinfo** %s\n\n", playersinfo) else ""
        s += if (ucLink != null) String.format("**Ultimate Central:** %s\n\n", ucLink) else ""
        s += if (comment != null) String.format("**Kommentar:** %s", comment) else ""

        return s

    }

    override fun toString(): String {
        return "Tournament(id=$id, name=$name, dateFrom=$dateFrom, dateTo=$dateTo, location=$location, format=$format, division=$division, teamFee=$teamFee, playersFee=$playersFee, registrationDeadline=$registrationDeadline, paymentDeadline=$paymentDeadline, schedule=$schedule, playersinfo=$playersinfo, ucLink=$ucLink, comment=$comment, announcementChannel=$announcementChannel, discussionChannel=$discussionChannel, infoMessage=$infoMessage, tableMessage=$tableMessage, eatingMessage=$eatingMessage)"
    }

}