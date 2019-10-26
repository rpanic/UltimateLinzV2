package model

import db.ChangeObserver
import main.Main

class GeneralAnnouncementChannelImposer(val channel : GeneralAnnouncementChannel) : ChangeObserver<GeneralAnnouncementChannel>(channel) {

    

}