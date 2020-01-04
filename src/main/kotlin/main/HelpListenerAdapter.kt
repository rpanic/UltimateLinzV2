package main

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class HelpListenerAdapter : ListenerAdapterCommand("help"){

    val padding = 12

    override fun help(event: MessageReceivedEvent, msg: Array<out String>?) {

        val builder = MessageBuilder()

        builder.append("**Alle Kategorien von commands:**")

        val commands = Main.jda.registeredListeners
            .asSequence()
            .filterIsInstance<ListenerAdapterCommand>()
            .filter { it !is HelpListenerAdapter }
            .filter { it.getCommandMethods(it.javaClass, Main.jda.guilds[0], event.author).size > 0 }
            .map {
                val help = it.javaClass.getAnnotation(Help::class.java)
                var helptext = "-${it.cmd.replace(Main.prefix ?: "", "")}"
                if(help != null){
                    helptext = helptext.padRight(padding) + help.value
                }
                helptext
            }.joinToString("\n")

        builder.appendCodeBlock(commands, "")

        send(event.channel, builder.build())
    }

}