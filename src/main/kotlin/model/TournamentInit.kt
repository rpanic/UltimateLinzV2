package model

import db.DB
import main.AsciiMessageObserver
import main.EmoteLimiter
import main.Main
import main.nickName
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import tournament.tournamentDbKey
import net.dv8tion.jda.api.entities.TextChannel
import watch.linkWatchToReaction


object TournamentInit{

    fun init(t: Tournament){

        println("Initializing tournament ${t.name}")

        //Info & Table
        var newc = Main.jda.getTextChannelById(t.announcementChannel)

        val infoM = newc!!.retrieveMessageById(t.infoMessage).complete()

        var limiter = EmoteLimiter(infoM)
            .setAllowedEmotes(listOf("in", "out"))
            .setDisplayAllowed(true)
            .setLimitEmotes(true)
            .setLimitReactions(true)

        var observer = ReactionStateObserver(infoM, limiter)
        observer.init()

        linkWatchToReaction(observer)

        val tableMessage = newc.retrieveMessageById(t.tableMessage).complete()

        limiter.addEmoteListener(object: EmoteLimiter.EmoteListener{

            val messageObserver = AsciiMessageObserver(tableMessage)
            val observerRef = observer

            init {
               observerRef.getReactions().entries.associateBy ({ it.value }){it.key}
                   .flatMap { it.key.map { x -> Pair(x, it.value) } }
                   .forEach {
                       messageObserver.answerChanged(it.first.nickName(observerRef.message.guild), it.second)
                   }
                messageObserver.editMessage()
            }

            override fun emoteAdd(e: MessageReactionAddEvent?) {
                println("EmoteAdd ${e?.user} ${e?.reactionEmote?.name}")
                messageObserver.answerChanged(e!!.user.nickName(tableMessage.guild), observerRef.getReactionByUser(e.user.idLong))
            }

            override fun emoteRemove(e: MessageReactionRemoveEvent?) {
                println("EmoteRemove ${e?.user} ${e?.reactionEmote?.name}")
                messageObserver.answerChanged(e!!.user.nickName(tableMessage.guild), observerRef.getReactionByUser(e.user.idLong))
            }
        })
        limiter.start(newc)

        initEating(t)

        TournamentChangeObserver(t).all(Tournament::announcementChannel, "") //TODO not the cleanest solution

//        limiter.addEmoteListener(object: EmoteLimiter.EmoteListener{
//
//            val observer = observer
//
//            override fun emoteAdd(e: MessageReactionAddEvent?) {
//                messageObserver.answerChanged(tableMessage.guild.getMemberById(e!!.user.idLong)!!.nickname, observer.getReactionByUser(e.user.idLong))
//            }
//
//            override fun emoteRemove(e: MessageReactionAddEvent?, r: MessageReaction?) {
//                messageObserver.answerChanged(tableMessage.guild.getMemberById(e!!.user.idLong)!!.nickname, observer.getReactionByUser(e.user.idLong))
//            }
//        })

    }

    fun initEating(t: Tournament){

        if(t.eatingEnabled){

            var newc = Main.jda.getTextChannelById(t.announcementChannel)

            val eatingM = newc!!.retrieveMessageById(t.eatingMessage).complete()

            val limiter = EmoteLimiter(eatingM)
                .setAllowedEmotes(listOf("meat", "veggie"))
                .setDisplayAllowed(true)
                .setLimitEmotes(true)
                .setLimitReactions(true)

            val observer = ReactionStateObserver(eatingM, limiter)
            observer.init()

            limiter.start(eatingM.channel)

        }

    }

}