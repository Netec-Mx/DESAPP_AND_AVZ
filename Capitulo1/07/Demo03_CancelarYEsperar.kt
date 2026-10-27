import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun main() = runBlocking {
    val tarea = launch {
        launch {
            try {
                println("HIJA: comienza")
                delay(5000)
                println("HIJA: completo su trabajo")
            } finally {
                println("HIJA: ejecuta finally")
            }
        }
        try {
            println("TAREA: comienza")
            repeat(10) { paso ->
                delay(100)
                println("TAREA: paso ${paso + 1}")
            }
        } catch (e: CancellationException) {
            println("TAREA: detecto la cancelacion")
            throw e // Registrar la cancelacion no debe convertirla en un exito.
        } finally {
            println("TAREA: ejecuta finally")
        }
    }

    yield()
    delay(250)
    println("CONTROL: solicita cancelar")
    tarea.cancel() // Solicita cancelar la tarea y sus hijas; no espera su cierre.
    println("CONTROL: isCancelled=${tarea.isCancelled}, isCompleted=${tarea.isCompleted}")
    tarea.join()
    println("CONTROL: finalizaron la tarea y su hija; isCompleted=${tarea.isCompleted}")
}
