import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val temperaturas = flow {
        println("PRODUCTOR: comienza una lectura")
        for (celsius in listOf(20, 25, 30)) {
            delay(200)
            println("PRODUCTOR: emite $celsius C")
            emit(celsius)
        }
    }

    // Crear el flujo no ejecuta su bloque: hace falta recolectarlo.
    println("Flujo creado; todavia no hay lecturas")

    val mensajes = temperaturas.map { celsius ->
        "Temperatura: $celsius C = ${celsius * 9 / 5 + 32} F"
    }

    println("--- Recolector A ---")
    mensajes.collect { mensaje -> println("A recibe: $mensaje") }

    // El segundo collect vuelve a ejecutar el productor completo.
    println("--- Recolector B ---")
    mensajes.collect { mensaje -> println("B recibe: $mensaje") }
}
