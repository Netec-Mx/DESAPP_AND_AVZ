package tema13.base

// Tema 1.3: enum, sealed class, sealed interface y flujo de estados.
// Archivo completo de Kotlin/JVM. No copiar los delimitadores de Markdown.
// Compilar SOLO este archivo: kotlinc Demo_1_3.kt -include-runtime -d demo13.jar
// Ejecutar todo: java -jar demo13.jar
// Ejecutar un bloque: java -jar demo13.jar 5
// Los retos R1-R7 estan comentados: el archivo inicial compila sin resolverlos.

private fun cabecera(numero: Int, titulo: String) {
    println("\n=== DEMO $numero: $titulo ===")
}

// Este primer modelo permite cualquier combinacion de sus tres propiedades.
private data class BanderasCarga(
    val cargando: Boolean,
    val datos: List<String>?,
    val error: String?
)

private fun validarBanderas(estado: BanderasCarga): Boolean {
    // Contrato de ESTA demo: tras iniciar una solicitud queremos exactamente
    // una alternativa: cargando, datos disponibles O error.
    // Otra aplicacion podria permitir mostrar datos previos mientras actualiza.
    val alternativas = listOf(
        estado.cargando,
        estado.datos != null,
        estado.error != null
    )
    return alternativas.count { it } == 1
}

private fun demo1() {
    cabecera(1, "El problema de las banderas independientes")
    val estado = BanderasCarga(
        cargando = true, // R1: cambiar a false.
        datos = listOf("Teclado"),
        error = "Sin conexion" // R1: cambiar a null.
    )
    println("Cargando: ${estado.cargando}")
    println("Datos: ${estado.datos}")
    println("Error: ${estado.error}")
    println("Valido segun nuestro contrato: ${validarBanderas(estado)}")
    // Compila incluso cuando los datos contradicen nuestro contrato.
    // R1: dejar SOLO la alternativa de datos y anticipar la validacion.
}

private enum class Prioridad(val etiqueta: String) {
    BAJA("Baja"),
    NORMAL("Normal"),
    ALTA("Alta")
}

private fun horasDeRespuesta(prioridad: Prioridad): Int = when (prioridad) {
    Prioridad.BAJA -> 48
    Prioridad.NORMAL -> 24
    Prioridad.ALTA -> 4 // R2: cambiar a 2.
    // No hace falta else: cubrimos todas las constantes.
}

private fun demo2() {
    cabecera(2, "enum: un conjunto fijo de valores")
    for (prioridad in Prioridad.entries) {
        println("${prioridad.name}: ${prioridad.etiqueta}, ${horasDeRespuesta(prioridad)} h")
    }
    val primera = Prioridad.ALTA
    val segunda = Prioridad.ALTA
    println("Misma constante: ${primera === segunda}")
    // Cada constante es una instancia compartida; puede tener propiedades.
    // Aqui usamos propiedades val para mantener sus metadatos inmutables.
    // R2: reducir a 2 horas el tiempo de ALTA y anticipar que linea cambia.
}

private sealed class ResultadoCarga protected constructor() {
    object Cargando : ResultadoCarga()
    data class Exito(val cantidad: Int) : ResultadoCarga()
    data class Error(val mensaje: String) : ResultadoCarga()
}

private fun describirResultado(resultado: ResultadoCarga): String = when (resultado) {
    // Cargando es un objeto concreto: comparamos con ese valor.
    ResultadoCarga.Cargando -> "Cargando"
    // Exito y Error son clases: comprobamos el TIPO mediante is.
    is ResultadoCarga.Exito -> "Exito con ${resultado.cantidad} elementos"
    is ResultadoCarga.Error -> "Error: ${resultado.mensaje}"
    // El smart cast permite leer cantidad o mensaje en la rama adecuada.
}

private fun demo3() {
    cabecera(3, "sealed class: alternativas con datos distintos")
    val primero = ResultadoCarga.Exito(2)
    val segundo = ResultadoCarga.Exito(5) // R3: cambiar a 9.
    val estados: List<ResultadoCarga> = listOf(
        ResultadoCarga.Cargando,
        primero,
        segundo,
        ResultadoCarga.Error("Archivo no disponible")
    )
    estados.forEach { println(describirResultado(it)) }

    val carga1 = ResultadoCarga.Cargando
    val carga2 = ResultadoCarga.Cargando
    println("Cargando compartido: ${carga1 === carga2}")
    println("Exitos con los mismos datos: ${primero == segundo}")
    // No podemos construir ResultadoCarga() directamente: es sellada/abstracta.
    // Exito admite muchas instancias con distintos valores de cantidad.
    // R3: cambiar solo la cantidad del segundo Exito y observar la salida.
}

private open class EventoConOrigen(val origen: String)

private interface Auditable {
    fun registrar(): String
}

private sealed interface AccionCatalogo

// Hereda de UNA clase y, ademas, implementa DOS interfaces.
private class SolicitarActualizacion(origen: String) :
    EventoConOrigen(origen), AccionCatalogo, Auditable {
    override fun registrar(): String = "solicitud desde $origen"
}

private object CancelarCarga : AccionCatalogo

private fun describirAccion(accion: AccionCatalogo): String = when (accion) {
    is SolicitarActualizacion -> "Actualizar; ${accion.registrar()}"
    CancelarCarga -> "Cancelar carga"
}

private fun demo4() {
    cabecera(4, "sealed interface y una clase base existente")
    val solicitud = SolicitarActualizacion("teclado") // R4: usar "menu".
    println("Origen heredado: ${solicitud.origen}")
    val acciones: List<AccionCatalogo> = listOf(solicitud, CancelarCarga)
    acciones.forEach { println(describirAccion(it)) }
    // La interfaz sellada define las alternativas; no tiene constructor propio.
    // La clase implementadora SI puede guardar datos por instancia.
    // R4: cambiar el origen a menu; anticipar las dos lineas afectadas.
}

// out T permite usar un estado que produce un tipo mas especifico donde
// se espera otro mas general. Aqui T solo se expone como salida de datos.
private sealed interface ResourceState<out T> {
    // Nothing es subtipo de todos los tipos y no tiene valores instanciables.
    // Loading no necesita transportar un T; no estamos creando un Nothing.
    object Loading : ResourceState<Nothing>
    data class Success<out T>(val data: T) : ResourceState<T>
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ResourceState<Nothing>
    // R7, paso 1: agregar object Empty : ResourceState<Nothing>.
}

private data class Product(val id: Int, val name: String, val price: Double)

private fun productosDeEjemplo(): List<Product> = listOf(
    Product(1, "Teclado", 800.0),
    Product(2, "Mouse", 350.0)
    // R6: agregar Product(3, "Monitor", 2500.0). Agregar la coma necesaria.
)

private fun renderizarEstado(estado: ResourceState<List<Product>>): String =
    when (estado) {
        ResourceState.Loading -> "[UI] Cargando productos"
        is ResourceState.Success -> {
            // Smart cast: aqui estado es Success<List<Product>>.
            // data es List<Product>; no usamos as ni conversion forzada.
            val nombres = estado.data.map { it.name }
            "[UI] Datos: ${estado.data.size} productos $nombres"
        }
        is ResourceState.Error -> "[UI] Error: ${estado.message}"
        // R7, paso 2: manejar ResourceState.Empty sin agregar else.
    }

private class DataFlowProcessor {
    fun processIncomingState(estado: ResourceState<List<Product>>) {
        // Aqui la presentacion solo consiste en imprimir una linea de consola.
        println(renderizarEstado(estado))
    }
}

private fun demo5() {
    cabecera(5, "ResourceState generico y smart cast")
    val procesador = DataFlowProcessor()
    val cargando: ResourceState<List<Product>> = ResourceState.Loading
    val exito: ResourceState<List<Product>> = ResourceState.Success(productosDeEjemplo())
    val fallo = ResourceState.Error(
        "Sin conexion", // R5: cambiar a "Servicio en mantenimiento".
        java.io.IOException("Tiempo de espera agotado")
    )
    procesador.processIncomingState(cargando)
    procesador.processIncomingState(exito)
    procesador.processIncomingState(fallo)
    println("Detalle tecnico: ${fallo.exception?.message}")

    // La misma instancia Loading es compatible con otro parametro de tipo.
    val cargaDeTexto: ResourceState<String> = ResourceState.Loading
    println("Loading compartido entre tipos: ${cargando === cargaDeTexto}")
    // R5: modificar SOLO el mensaje para el usuario; conservar la excepcion.
}

private fun solicitarProductos(
    simularError: Boolean,
    publicar: (ResourceState<List<Product>>) -> Unit
) {
    // Flujo didactico SIN corrutinas: se llama al callback sincronicamente.
    publicar(ResourceState.Loading)
    if (simularError) {
        publicar(ResourceState.Error("No se pudo consultar el catalogo"))
    } else {
        publicar(ResourceState.Success(productosDeEjemplo()))
    }
    // No hay red real, StateFlow, hilos nuevos ni espera simulada.
}

private fun demo6() {
    cabecera(6, "Productor, estado y consumidor")
    val procesador = DataFlowProcessor()
    println("Solicitud 1: error simulado")
    solicitarProductos(simularError = true) { estado ->
        procesador.processIncomingState(estado)
    }
    println("Solicitud 2: carga correcta")
    solicitarProductos(simularError = false) { estado ->
        procesador.processIncomingState(estado)
    }
    // El productor decide que publicar; el consumidor decide como presentarlo.
    // Se reutiliza la trailing lambda estudiada en el tema 1.2.
    // R6: agregar un tercer producto en productosDeEjemplo y anticipar
    // que solicitud cambia. La demo5 tambien usa esa misma lista.
}

private fun demo7() {
    cabecera(7, "Exhaustividad al agregar una alternativa")
    val estado: ResourceState<List<Product>> = ResourceState.Success(emptyList())
    println(renderizarEstado(estado))
    // R7, paso 1: agregar Empty dentro de ResourceState y compilar.
    // Observar el error del when incompleto en renderizarEstado.
    // R7, paso 2: agregar la rama que devuelve "[UI] Sin productos".
    // R7, paso 3: sustituir Success(emptyList()) de esta demo por Empty.
    // Volver a compilar y ejecutar. No se necesita un else generico.
}

fun main(args: Array<String>) {
    val demos: List<() -> Unit> = listOf(
        ::demo1, ::demo2, ::demo3, ::demo4, ::demo5, ::demo6, ::demo7
    )
    if (args.isEmpty()) {
        demos.forEach { it() }
        return
    }
    val numero = args.singleOrNull()?.toIntOrNull()
    if (numero == null || numero !in 1..demos.size) {
        println("Uso: java -jar demo13.jar [1..7]")
        return
    }
    demos[numero - 1]()
}
