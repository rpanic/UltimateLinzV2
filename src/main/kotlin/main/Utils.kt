package main

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageReaction
import net.dv8tion.jda.api.entities.User
import org.apache.commons.lang3.time.DateUtils
import java.io.File
import java.util.*

fun <R> (() -> R).then(f: () -> R): () -> R {
    return {
        this()
        f()
    }
}

fun <T : Number> T.trim(min: T, max: T) : T {

    if(this.toDouble() > max.toDouble()){
        return max
    }

    if(this.toDouble() < min.toDouble()){
        return min
    }

    return this

}

fun userdir() = File(System.getProperty("user.dir"))

fun File.child(s: String) = File(this.absolutePath + "${File.separator}$s").apply { if(name.lastIndexOf('.') == -1){ mkdir() } }

fun String.padRight(n: Int): String {
    return String.format("%1$-" + n + "s", this)
}

fun String.padLeft(n: Int): String {
    return String.format("%1$" + n + "s", this)
}

fun isNull(obj: Any?) : Boolean{
    return obj == null
}

fun <T, V> T.has(pred: T.() -> Boolean, default: V, function: T.() -> V): V {
    return if(pred(this)){
        function(this)
    }else{
        default
    }
}


// ### Needs Apache Commons

fun parseDateLazy(date: String) : Date{
    return DateUtils.parseDate(date, "dd.MM.yyyy", "dd.MM.yy", "MM-DD-YYYY", "MM-DD-YY", "DD-MM-YYYY", "DD-MM-YY")
}

// #### Discord Sepcific
fun MessageReaction.ReactionEmote.emoteOrEmojiName() = if(isEmoji) emoji else emote.name

fun User.getMember(g: Guild) = g.getMemberById(idLong)

fun User.nickName(g: Guild) = getMember(g)?.effectiveName