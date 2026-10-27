import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class Contador {
    private val _estado = MutableStateFlow(0)
    val estado: StateFlow<Int> = _estado.asStateFlow()

    fun cambiarA(valor: Int) {
        _estado.value = valor
    }
}

fun main() = runBlocking {
    val contador = Contador()
    println("Valor disponible sin recolectores: ${contador.estado.value}")

    val observadorA = launch {
        contador.estado.collect { println("A recibe: $it") }
    }
    yield()

    contador.cambiarA(1)
    yield()
    println("Asignamos otra vez 1")
    contador.cambiarA(1) // Un valor igual al actual no genera otra emision.
    yield()

    contador.cambiarA(2)
    yield()
    val observadorB = launch {
        contador.estado.collect { println("B recibe: $it") }
    }
    yield() // B recibe el estado actual, aunque no vio los cambios anteriores.

    observadorA.cancelAndJoin()
    observadorB.cancelAndJoin()

    println("--- Recolector lento ---")
    val lento = launch {
        contador.estado.collect {
            println("LENTO recibe: $it")
            delay(250)
        }
    }
    yield()
    contador.cambiarA(3)
    contador.cambiarA(4)
    contador.cambiarA(5)
    delay(400) // El recolector lento recibe 5 y omite los estados intermedios.
    lento.cancelAndJoin()
}
