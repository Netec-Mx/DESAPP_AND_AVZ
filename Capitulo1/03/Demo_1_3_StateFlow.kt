package tema13.flujo

// Ampliacion OPCIONAL. Este archivo SI usa kotlinx.coroutines.
// Es independiente de Demo_1_3.kt; compilar por separado con -cp.
// Los comandos completos para Windows estan en Guia_1_3.md.

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

private sealed interface ResourceState<out T> {
    object Loading : ResourceState<Nothing>
    data class Success<out T>(val data: T) : ResourceState<T>
    data class Error(val message: String) : ResourceState<Nothing>
}

private fun presentar(estado: ResourceState<List<String>>): String = when (estado) {
    ResourceState.Loading -> "[UI] Cargando"
    is ResourceState.Success -> "[UI] Datos: ${estado.data}"
    is ResourceState.Error -> "[UI] Error: ${estado.message}"
}

fun main() = runBlocking {
    // MutableStateFlow conserva UN estado actual y permite actualizarlo.
    val editable = MutableStateFlow<ResourceState<List<String>>>(ResourceState.Loading)
    // El observador utiliza una vista de solo lectura.
    val observable: StateFlow<ResourceState<List<String>>> = editable.asStateFlow()

    // UNDISPATCHED no crea un hilo: comienza a ejecutar la corrutina aqui,
    // hasta su primera suspension. Asi el observador recibe Loading primero.
    val observador = launch(start = CoroutineStart.UNDISPATCHED) {
        // StateFlow no termina por si mismo. take(3) limita ESTA demostracion
        // a tres estados observados para que el programa pueda finalizar.
        observable.take(3).collect { estado ->
            println(presentar(estado))
        }
    }

    editable.value = ResourceState.Success(listOf("Teclado", "Mouse"))
    // Ceder ejecucion permite al observador mostrar el estado en esta demo
    // controlada, con el event loop de runBlocking en un unico hilo.
    yield()
    editable.value = ResourceState.Error("Sin conexion") // RF1: cambiar el mensaje.
    observador.join()

    println("La demo termino")
    // StateFlow representa estado actual; no es un historial de eventos.
    // Actualizaciones iguales se omiten y un observador lento puede perder
    // estados intermedios. Esta demo cede ejecucion para hacer visibles los tres.
}
