import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

fun lecturas(): Flow<Int> = flow {
    println("PRODUCTOR: inicia una ejecucion")
    try {
        var numero = 1
        while (true) {
            delay(100)
            emit(numero++)
        }
    } finally {
        println("PRODUCTOR: termina esta ejecucion")
    }
}

suspend fun demostrarConversion(usarStateIn: Boolean) = coroutineScope {
    println("--- ${if (usarStateIn) "stateIn" else "shareIn"} ---")
    // Un Job hijo permite terminar la produccion compartida al cerrar la demo.
    val trabajo = Job(coroutineContext[Job])
    val ambito = CoroutineScope(coroutineContext + trabajo)
    val estrategia = SharingStarted.WhileSubscribed(stopTimeoutMillis = 500)
    val origen = lecturas()

    val compartido: Flow<Int> = if (usarStateIn) {
        val estado = origen.stateIn(ambito, estrategia, initialValue = 0)
        println("Valor inicial de StateFlow: ${estado.value}")
        estado
    } else {
        origen.shareIn(ambito, estrategia, replay = 1)
    }

    fun observar(nombre: String) = launch {
        compartido.collect { println("$nombre recibe: $it") }
    }

    try {
        println("Sin recolectores: todavia no comienza el productor")
        yield()
        val a = observar("A")
        delay(250)
        val b = observar("B")
        delay(200)
        a.cancelAndJoin()
        b.cancelAndJoin()

        println("Sin suscriptores durante 200 ms: volvemos antes del limite")
        delay(200)
        val c = observar("C")
        delay(200)
        c.cancelAndJoin()

        println("Ahora esperamos mas de 500 ms sin suscriptores")
        delay(700)
        println("D se conecta: recibe lo conservado y reinicia el origen")
        val d = observar("D")
        delay(250)
        d.cancelAndJoin()
    } finally {
        trabajo.cancelAndJoin()
    }
}

fun main() = runBlocking {
    demostrarConversion(usarStateIn = true)
    demostrarConversion(usarStateIn = false)
}
