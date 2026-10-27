import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

sealed interface EstadoProductos {
    object Cargando : EstadoProductos
    data class Contenido(val productos: List<String>) : EstadoProductos
    data class Error(val mensaje: String) : EstadoProductos
}

class Catalogo {
    private val _estado = MutableStateFlow<EstadoProductos>(EstadoProductos.Cargando)
    val estado: StateFlow<EstadoProductos> = _estado.asStateFlow()

    private val _avisos = MutableSharedFlow<String>(replay = 0)
    val avisos: SharedFlow<String> = _avisos.asSharedFlow()

    suspend fun esperarObservadorDeAvisos() {
        _avisos.subscriptionCount.first { it > 0 }
    }

    suspend fun cargar(simularError: Boolean = false) {
        _estado.value = EstadoProductos.Cargando
        try {
            delay(300)
            if (simularError) throw IllegalStateException("Conexion simulada no disponible")
            _estado.value = EstadoProductos.Contenido(listOf("Teclado", "Monitor"))
            _avisos.emit("Productos actualizados")
        } catch (e: CancellationException) {
            throw e // Una cancelacion debe conservar su significado.
        } catch (e: IllegalStateException) {
            _estado.value = EstadoProductos.Error(e.message ?: "Error de carga")
            _avisos.emit("No fue posible actualizar los productos")
        }
    }
}

fun describir(estado: EstadoProductos): String = when (estado) {
    EstadoProductos.Cargando -> "Cargando..."
    is EstadoProductos.Contenido -> "Productos: ${estado.productos}"
    is EstadoProductos.Error -> "Error: ${estado.mensaje}"
}

fun main() = runBlocking {
    val catalogo = Catalogo()

    fun conectarPantalla() = launch {
        launch { catalogo.estado.collect { println("PANTALLA: ${describir(it)}") } }
        launch { catalogo.avisos.collect { println("AVISO: $it") } }
    }

    println("--- Pantalla conectada ---")
    var pantalla = conectarPantalla()
    catalogo.esperarObservadorDeAvisos()
    catalogo.cargar()
    yield()

    println("--- Pantalla desconectada ---")
    pantalla.cancelAndJoin()
    catalogo.cargar(simularError = true)
    println("La carga termino sin observadores")

    println("--- Pantalla conectada otra vez ---")
    pantalla = conectarPantalla()
    catalogo.esperarObservadorDeAvisos()
    yield() // Regresa el estado Error, pero no se repite el aviso pasado.
    pantalla.cancelAndJoin()
}
