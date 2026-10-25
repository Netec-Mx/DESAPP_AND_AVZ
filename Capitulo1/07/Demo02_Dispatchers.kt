import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

fun mostrarHilo(mensaje: String) {
    println("$mensaje | hilo=${Thread.currentThread().name}")
}

fun main() = runBlocking {
    mostrarHilo("Inicio en consola")
    launch {
        mostrarHilo("launch hereda el contexto de runBlocking")
    }.join()

    val numeros = withContext(Dispatchers.IO) {
        mostrarHilo("IO: espera bloqueante simulada")
        Thread.sleep(100) // Simula una API bloqueante; sigue ocupando un hilo de IO.
        listOf(1, 2, 3, 4)
    }
    mostrarHilo("Regreso a la coordinacion")

    val suma = withContext(Dispatchers.Default) {
        mostrarHilo("Default: calculo")
        numeros.sumOf { it * it }
    }
    mostrarHilo("Resultado: $suma")

    launch(Dispatchers.Unconfined) {
        mostrarHilo("Unconfined: antes de delay")
        delay(100)
        mostrarHilo("Unconfined: despues de delay")
    }.join()

    mostrarHilo("Fin en consola")
    // Dispatchers.Main requiere una integracion de UI; no se utiliza en esta consola.
}
