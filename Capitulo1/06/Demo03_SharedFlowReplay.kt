import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun demostrarReplay(replay: Int) = coroutineScope {
    println("--- SharedFlow con replay = $replay ---")
    val avisos = MutableSharedFlow<String>(replay = replay)

    println("Emitimos PREVIO sin suscriptores")
    avisos.emit("PREVIO")

    val a = launch {
        avisos.collect { println("A recibe: $it") }
    }
    // Esperamos una suscripcion real antes de emitir el siguiente aviso.
    avisos.subscriptionCount.first { it == 1 }
    yield()
    avisos.emit("ACTUAL")
    yield()

    val b = launch {
        avisos.collect { println("B recibe: $it") }
    }
    avisos.subscriptionCount.first { it == 2 }
    yield()

    avisos.emit("NUEVO")
    avisos.emit("NUEVO") // SharedFlow tambien entrega valores consecutivos iguales.
    yield()

    a.cancelAndJoin()
    b.cancelAndJoin()
}

fun main() = runBlocking {
    demostrarReplay(0)
    demostrarReplay(1)
}
