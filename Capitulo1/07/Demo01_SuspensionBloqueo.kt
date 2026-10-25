import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

suspend fun comparar(bloquear: Boolean) = coroutineScope {
    println("--- ${if (bloquear) "Thread.sleep" else "delay"} ---")
    val tarea = launch {
        println("A: empieza en ${Thread.currentThread().name}")
        if (bloquear) {
            Thread.sleep(200) // Ocupa el hilo; ninguna otra tarea puede usarlo aqui.
        } else {
            delay(200) // Suspende A; el hilo puede ejecutar otra corrutina lista.
        }
        println("A: termina la espera")
    }
    yield() // Dejamos que A comience antes del siguiente mensaje.
    println("B: puede avanzar en ${Thread.currentThread().name}")
    tarea.join()
}

fun main() = runBlocking {
    comparar(bloquear = false)
    comparar(bloquear = true)
}
