import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

suspend fun confirmarCierre() {
    println("LIMPIEZA: inicia confirmacion suspendible")
    delay(100)
    println("LIMPIEZA: confirmacion terminada")
}

suspend fun demostrar(proteger: Boolean) = coroutineScope {
    println("--- ${if (proteger) "Con NonCancellable" else "Sin NonCancellable"} ---")
    val tarea = launch {
        try {
            println("TAREA: utiliza el recurso simulado")
            delay(5000)
        } finally {
            println("FINALLY: libera el recurso sin suspender")
            if (proteger) {
                // Protegemos solo el tramo de limpieza que necesita suspenderse.
                withContext(NonCancellable) { confirmarCierre() }
            } else {
                try {
                    confirmarCierre()
                } catch (e: CancellationException) {
                    println("LIMPIEZA: delay detecto que la tarea ya estaba cancelada")
                    throw e
                }
            }
        }
    }
    yield()
    println("CONTROL: cancela y espera")
    tarea.cancelAndJoin()
    println("CONTROL: tarea terminada; isCancelled=${tarea.isCancelled}")
}

fun main() = runBlocking {
    demostrar(proteger = false)
    demostrar(proteger = true)
}
