import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.system.measureTimeMillis

enum class Cooperacion { NINGUNA, IS_ACTIVE, ENSURE_ACTIVE, YIELD }

suspend fun comparar(modo: Cooperacion) = coroutineScope {
    println("--- $modo ---")
    val iniciada = CompletableDeferred<Unit>()
    val tarea = launch(Dispatchers.Default) {
        val limite = System.nanoTime() + 600_000_000L
        var resultado = 0L
        iniciada.complete(Unit) // Solo sincroniza el inicio para hacer visible la comparacion.
        try {
            while (System.nanoTime() < limite) {
                when (modo) {
                    Cooperacion.NINGUNA -> Unit
                    Cooperacion.IS_ACTIVE -> if (!isActive) {
                        println("CPU: isActive es false; salimos del bucle")
                        break
                    }
                    Cooperacion.ENSURE_ACTIVE -> ensureActive()
                    Cooperacion.YIELD -> yield()
                }
                resultado = (resultado + 17) % 1_000_003
            }
            println("CPU: llego despues del bucle; resultado=$resultado")
        } catch (e: CancellationException) {
            println("CPU: detecto CancellationException")
            throw e
        } finally {
            println("CPU: ejecuta finally")
        }
    }
    iniciada.await()
    delay(50)
    println("CONTROL: solicita cancelar")
    tarea.cancel()
    val espera = measureTimeMillis { tarea.join() }
    println("CONTROL: espero aproximadamente $espera ms; isCancelled=${tarea.isCancelled}")
}

fun main() = runBlocking {
    Cooperacion.values().forEach { comparar(it) }
}
