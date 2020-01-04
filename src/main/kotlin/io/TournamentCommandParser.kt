package io

import db.DB
import main.Prompt
import model.Tournament
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import tournament.tournamentDbKey
import java.lang.RuntimeException

class TournamentCommandParser(val channel: MessageChannel, val user: User){

    fun getArgs(definition: TournamentCommandDefinition, args2: List<String>) : TournamentCommandResult{

        val args = args2.toMutableList()

        if(args[0].matches("[!?]t".toRegex())){
            args.removeAt(0)
        }

        check(args[0].equals(definition.command, true)) { "Not possible" }
        args.removeAt(0)

        val findTournamentByName = {name: String ->
            DB.getList<Tournament>(tournamentDbKey).list()
                .firstOrNull { x -> x.name.toLowerCase().startsWith(name.toLowerCase()) }
        }

        val promptTournament = { prompt: String -> Prompt(prompt, channel, user)
                .promptSync()?.let { findTournamentByName(it) }
        }

        val t = if(args.size > 0){
            var t = findTournamentByName(args[0])
            if(t == null){
                t = promptTournament("Turnier ${args[0]} nicht gefunden - try again") ?: return TournamentCommandResult(null, "Turnier nicht gefunden!")
            }
            args.removeAt(0)
            t
        }else{
            promptTournament("Welches Turnier?") ?: return TournamentCommandResult(null, "Turnier nicht gefunden!")
        }

        val results = mutableListOf<String>()
        for((i, param) in definition.parameterQuestions.withIndex()){
            if(args.size > i){
                results += args[i]
            }else{
                results += Prompt(param, channel, user)
                    .promptSync() ?: throw RuntimeException("Error while waiting for answer")
            }
        }

        return TournamentCommandResult(TournamentCommandArgs(definition.command, t, results), null)
    }

}

fun String.similarity(s: String) : Int {

    val s1 = this.toLowerCase()
    val s2 = s.toLowerCase()

    return s2.length * if(s1.contains(s2)) 1 else 0

}

data class TournamentCommandResult(val args: TournamentCommandArgs?, val error: String?)

data class TournamentCommandArgs(val command: String, val tournament: Tournament, val parameters: List<String>)

data class TournamentCommandDefinition(val command: String, val parameterQuestions: List<String>)

fun tournamentCommandDefinition(command: String, vararg parameterQuestions: String) = TournamentCommandDefinition(command, parameterQuestions.toList())