package helper

import kotlin.experimental.or

class StateObserverBuilder<T>{

    lateinit var trigger: ((T) -> Unit) -> Unit
    lateinit var then: T.() -> Unit

    var set = 0.toByte()

    fun trigger(f: ((T) -> Unit) -> Unit){
        trigger = f
        set = set or 1
    }

    fun then(f: T.() -> Unit){
        then = f
        set = set or 2
    }

    fun build() : StateObserver<T> {

        val observer = object : StateObserver<T>(){
            val t = trigger

            override fun trigger(f: (T) -> Unit) {
                t(f)
            }
        }
        observer.then{then(it)}
        observer.init()
        return observer

    }

}

abstract class StateObserver<T>() {

    val thens: MutableList<(T) -> Unit>

    init {
        thens = mutableListOf()
    }

    fun init(){
        trigger{
            thens.forEach { f -> f(it) }
        }
    }

    abstract fun trigger(f: (T) -> Unit);

    fun then(f: (T) -> Unit){
        thens.add(f)
    }

}
