import db.DB
import model.Tournament

fun main(args: Array<String>) {

    val t = Tournament()
    t.id = 2
    t.name = "Test"

    val list = DB.getList<Tournament>("tournamentlist")

    list.add(t)

    println(list[0])

}