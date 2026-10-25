# Demo 1.3 — Sealed classes, enum classes y flujos de datos

**Caso:** modelar y presentar los estados de carga de un catálogo de productos.

Material complementario a las hojas compartidas de la PPT. Seguimos el formato del tema 1.2: explicación, observación de la salida y un pequeño cambio del participante por bloque. La práctica principal se ejecuta en consola con Kotlin/JVM.

<br/><br/>

## Archivos

| Archivo | Uso |
| --- | --- |
| `Demo_1_3.kt` | Siete demos con código comentado y retos R1–R7. Solo biblioteca estándar. |
| `Guia_1_3.md` | Secuencia de trabajo, explicaciones y código completo de ambos archivos al final. |
| `Demo_1_3_StateFlow.kt` | Ampliación opcional con `StateFlow` real. Requiere `kotlinx.coroutines`. |

<br/><br/>

## Ejecutar la práctica principal

Descarga directamente `Demo_1_3.kt` y abre PowerShell o CMD en la carpeta donde lo guardaste. Compila ese archivo por su nombre, para trabajar únicamente con esta práctica:

```powershell
kotlinc Demo_1_3.kt -include-runtime -d demo13.jar
java -jar demo13.jar
```

Para ejecutar solo un bloque, agrega un número del 1 al 7:

```powershell
java -jar demo13.jar 3
```

El código inicial está completo: los retos están comentados y no bloquean la ejecución. Después de cada edición, vuelve a compilar. Ejecuta el JAR solo si esa compilación termina correctamente.

Si copias el código desde esta guía, usa únicamente el listado de **Código completo principal**. Los fragmentos intermedios explican partes del programa y no se deben concatenar. No copies las líneas de tres acentos graves que delimitan los bloques Markdown.

<br/><br/>

## Temas y orden de trabajo

| Demo | Tema | Reto breve |
| --- | --- | --- |
| 1 | Banderas independientes y contrato del estado | Dejar activa solo la alternativa de datos |
| 2 | `enum class`: constantes con propiedades | Cambiar el tiempo de respuesta de prioridad alta |
| 3 | `sealed class`: variantes con diferentes datos | Modificar una instancia de éxito |
| 4 | `sealed interface`: combinar una clase base e interfaces | Cambiar el origen de una acción |
| 5 | `ResourceState<out T>`, `Nothing` y smart cast | Cambiar el mensaje visible conservando la causa técnica |
| 6 | Productor, estados y consumidor | Agregar un producto al catálogo |
| 7 | Exhaustividad de `when` al ampliar el modelo | Agregar y manejar la variante `Empty` |
| Opcional | Observación de un estado mediante `StateFlow` | Modificar el mensaje de error |

<br/><br/>

## Demo 1 — El problema que queremos resolver

Ejecuta `java -jar demo13.jar 1`.

`BanderasCarga` permite combinar libremente un indicador de carga, una lista opcional y un error opcional. El ejemplo inicial compila con las tres alternativas activas a la vez:

```text
Cargando: true
Datos: [Teclado]
Error: Sin conexion
Valido segun nuestro contrato: false
```

**Contrato elegido para esta demo:** una solicitud iniciada se encuentra en una de tres alternativas: está cargando, tiene datos o terminó con error. Con propiedades independientes, el compilador no impide romper ese contrato y tenemos que comprobarlo en ejecución.

No es una regla universal que carga y datos sean incompatibles: otra aplicación podría mostrar resultados anteriores mientras actualiza. Primero se define el comportamiento deseado y después se elige el modelo.

**R1.** En la construcción de `BanderasCarga`, cambia `cargando` a `false` y `error` a `null`. Conserva los datos. Anticipa el resultado de `validarBanderas`.

<br/><br/>

## Demo 2 — `enum class`: elegir un valor de un conjunto fijo

Ejecuta `java -jar demo13.jar 2`.

`Prioridad` solo puede ser `BAJA`, `NORMAL` o `ALTA`. Cada constante es una instancia compartida y tiene una propiedad `etiqueta`. Los valores de esa propiedad pueden ser distintos entre constantes.

`Prioridad.entries` permite recorrerlas. `horasDeRespuesta` usa un `when` como expresión y devuelve un entero. Como enumera todas las constantes, no necesita `else`.

```text
BAJA: Baja, 48 h
NORMAL: Normal, 24 h
ALTA: Alta, 4 h
Misma constante: true
```

`===` comprueba identidad: las dos referencias a `Prioridad.ALTA` apuntan a la misma constante.

**R2.** Cambia de `4` a `2` las horas de respuesta de `ALTA`. Predice qué línea de salida cambia.

**Idea clave:** un enum puede tener propiedades y funciones. Su utilidad aquí es ofrecer un conjunto fijo de opciones; no crear una nueva instancia de prioridad por cada solicitud.

<br/><br/>

## Demo 3 — `sealed class`: una alternativa puede llevar sus propios datos

Ejecuta `java -jar demo13.jar 3`.

`ResultadoCarga` define tres variantes: `Cargando`, `Exito(cantidad)` y `Error(mensaje)`. Cada valor representa una de esas alternativas. Éxito y error llevan datos de formas diferentes.

- `object Cargando` declara una instancia compartida: se escribe sin paréntesis.
- `data class Exito` permite crear instancias con distintas cantidades.
- `data class Error` guarda un mensaje por instancia.
- `sealed class` no se instancia directamente. En el ejemplo se muestra explícitamente su constructor `protected`, que es la visibilidad predeterminada de los constructores sellados.

En `describirResultado`, la rama `ResultadoCarga.Cargando` compara con ese objeto; `is ResultadoCarga.Exito` comprueba el tipo. Dentro de esa rama, Kotlin permite leer `resultado.cantidad`: es un **smart cast**.

**R3.** Cambia la cantidad del segundo `Exito` de `5` a `9`. Predice qué mensaje cambia y si la comparación de éxitos pasa a ser verdadera.

`==` compara datos en estas data classes; `===` compara identidad. Son preguntas distintas.


<br/><br/>


## Demo 4 — `sealed interface`: las variantes también pueden heredar de otra clase

Ejecuta `java -jar demo13.jar 4`.

`AccionCatalogo` agrupa dos alternativas: `SolicitarActualizacion` y `CancelarCarga`. La clase de actualización además hereda `origen` de `EventoConOrigen` e implementa `Auditable`.

```kotlin
class SolicitarActualizacion(origen: String) :
    EventoConOrigen(origen), AccionCatalogo, Auditable
```

Este fragmento resume la cabecera; la implementación completa está en el archivo. Kotlin admite una clase base y varias interfaces. La interfaz sellada no aporta constructor propio, pero sus implementaciones sí pueden tener datos por instancia.

**R4.** Cambia `SolicitarActualizacion("teclado")` por `SolicitarActualizacion("menu")`. Anticipa qué dos líneas cambian.

**Alcance de la jerarquía:** en Kotlin/JVM, las subclases o implementaciones directas de un tipo sellado se declaran en su mismo paquete y módulo. En estas demos están todas en el mismo archivo. Esto restringe las alternativas directas; no significa que solo puedan existir unas cuantas instancias.

<br/><br/>

## Comparar las tres herramientas

| Herramienta | Qué elegimos | Cómo se presentan los datos en esta práctica |
| --- | --- | --- |
| `enum class` | Una de las constantes declaradas | Cada prioridad tiene etiqueta; las constantes se comparten |
| `sealed class` | Una de las variantes de una clase base | Éxito lleva cantidad; error lleva mensaje |
| `sealed interface` | Una de las implementaciones directas previstas | Actualizar tiene origen y también hereda de una clase |

Elegimos por el modelo y las necesidades de herencia. Para esta práctica no necesitamos comparar rendimiento, memoria ni interoperabilidad con Swift.

<br/><br/>

## Demo 5 — Leer `ResourceState<out T>` por partes

Ejecuta `java -jar demo13.jar 5`.

Retomamos los nombres `ResourceState`, `Product` y `DataFlowProcessor` del caso de la PPT, completando la presentación con funciones de consola.

| Elemento | Significado en la práctica |
| --- | --- |
| `T` | Tipo del dato de éxito; en el catálogo es `List<Product>` |
| `Success<T>(data)` | Variante que conserva ese dato |
| `Loading` | Variante sin dato de éxito |
| `Error(message, exception)` | Mensaje para presentar y causa técnica opcional |
| `out T` | Declara covarianza: el modelo expone datos de tipo `T` como salida |
| `Nothing` | Tipo sin instancias, subtipo de los demás tipos |

Gracias a `out` y a `Nothing`, `Loading` y `Error` se pueden usar en un `ResourceState<List<Product>>` sin inventar datos de productos. El código muestra que la misma instancia `Loading` también se puede guardar en `ResourceState<String>`.

`Nothing` no es una lista vacía ni equivale a `null`. En `Loading : ResourceState<Nothing>` no se construye un valor de tipo `Nothing`; simplemente esa variante no contiene el dato de éxito.

`renderizarEstado` recibe el tipo general y devuelve un texto. En la rama `is ResourceState.Success`, el smart cast permite leer `estado.data.size` y recorrer los productos sin usar `as`.

**R5.** Cambia el mensaje `"Sin conexion"` a `"Servicio en mantenimiento"` en `demo5`. Conserva la excepción. Anticipa qué salida visible cambia y cuál sigue mostrando la misma causa técnica.

<br/><br/>

## Demo 6 — Productor, estado y consumidor

Ejecuta `java -jar demo13.jar 6`.

1. `solicitarProductos` actúa como productor: primero publica `Loading`.
2. Según `simularError`, publica `Error` o `Success`.
3. La lambda `publicar` entrega cada estado al consumidor.
4. `DataFlowProcessor` lo presenta utilizando `renderizarEstado`.

El programa realiza una solicitud con error simulado y otra correcta. Solo la segunda presenta la lista de productos.

**Esta demo es síncrona:** las llamadas al callback ocurren en orden en el mismo hilo. Muestra el recorrido de los datos, pero no crea un `Flow`, un `StateFlow`, una conexión de red ni una corrutina. Reutiliza las lambdas estudiadas en 1.2.

**R6.** Agrega `Product(3, "Monitor", 2500.0)` a `productosDeEjemplo`. Recuerda la coma entre elementos. Anticipa qué solicitud cambia y cuántos productos se muestran. `demo5` utiliza la misma función de datos y también reflejará ese cambio.

<br/><br/>

## Demo 7 — Dejar que el compilador señale la variante pendiente

Ejecuta primero `java -jar demo13.jar 7`. Inicialmente hay un éxito con lista vacía y se presenta como cero productos.

**R7.** Realiza los siguientes cambios, en orden:

1. Dentro de `ResourceState`, agrega `object Empty : ResourceState<Nothing>` y compila. Esta compilación debe fallar: `renderizarEstado` todavía no maneja la nueva alternativa.
2. En su `when`, agrega `ResourceState.Empty -> "[UI] Sin productos"`. Conserva las otras ramas y compila de nuevo.
3. En `demo7`, sustituye `ResourceState.Success(emptyList())` por `ResourceState.Empty`. Ejecuta el bloque 7.

No agregues `else` para ocultar la alternativa pendiente. La intención del reto es identificar exactamente qué variante falta. El texto concreto del diagnóstico puede variar entre versiones del compilador.

**Qué se garantiza:** el `when` contempla las alternativas del modelo sellado. Eso no valida por sí solo cualquier regla de negocio, el orden de todas las transiciones o los valores internos de cada objeto.

## Ampliación opcional — Observar un `StateFlow` real

Esta ampliación conecta el modelo de estados con la mención de `StateFlow` en la PPT. Puedes reservarla hasta abordar corrutinas. Es independiente de la demo principal y sigue siendo un programa de consola.

`Demo_1_3_StateFlow.kt` contiene:

- Un `MutableStateFlow` para actualizar el estado actual.
- Una vista `StateFlow` para observarlo.
- Un recolector que presenta `Loading`, `Success` y `Error`.
- `take(3)` para finalizar esta demostración después de tres valores observados.

`runBlocking`, `launch`, `yield` y `join` son el soporte de ejecución. Aquí basta con observar cómo cambia el valor y cómo el consumidor lo recibe; su estudio detallado corresponde a corrutinas.

<br/><br/>

### Preparar la dependencia

Necesitas el JAR **JVM** de `kotlinx-coroutines-core-jvm`. La ampliación se comprobó con la versión `1.10.1`; puedes usar esa versión con Kotlin 2.2.10.

En los comandos siguientes, sustituye la ruta de ejemplo por la ubicación real de tu JAR. `-include-runtime` incluye Kotlin, pero no incluye esta biblioteca externa.

<br/><br/>

### PowerShell

```powershell
$coroutines13 = "C:\work\libs\kotlinx-coroutines-core-jvm-1.10.1.jar"
kotlinc Demo_1_3_StateFlow.kt -cp "$coroutines13" -include-runtime -d demo13_stateflow.jar
java -cp "demo13_stateflow.jar;$coroutines13" tema13.flujo.Demo_1_3_StateFlowKt
```

<br/><br/>

### CMD

```bat
set "COR13=C:\work\libs\kotlinx-coroutines-core-jvm-1.10.1.jar"
kotlinc Demo_1_3_StateFlow.kt -cp "%COR13%" -include-runtime -d demo13_stateflow.jar
java -cp "demo13_stateflow.jar;%COR13%" tema13.flujo.Demo_1_3_StateFlowKt
```

La última parte es el nombre de la clase de entrada, incluido el paquete declarado en el archivo. En esta ampliación se utiliza `java -cp` para incluir ambos JAR.

Salida esperada:

```text
[UI] Cargando
[UI] Datos: [Teclado, Mouse]
[UI] Error: Sin conexion
La demo termino
```

**RF1.** Cambia el mensaje de error a `"Servicio en mantenimiento"` y vuelve a compilar y ejecutar.

<br/><br/>

### Qué observar

`StateFlow` conserva un estado actual; no es un historial de todos los cambios. Un observador lento puede omitir estados intermedios y las actualizaciones iguales por `equals` no se vuelven a entregar. Esta demostración cede ejecución con `yield()` para visualizar los cambios en un escenario controlado.

`StateFlow.collect` no finaliza por sí solo. `take(3)` limita este ejemplo; no es una estrategia general para determinar cuántos estados producirá una aplicación.

<br/><br/>

## Puntos de apoyo para explicar sin cambiar la PPT

- ADT significa tipo de datos algebraico. Aquí lo concretamos como alternativas: carga **o** éxito **o** error. Cada alternativa puede llevar sus propios datos.
- El conjunto de variantes puede ser acotado y, a la vez, admitir muchas instancias de éxito con valores diferentes.
- `object` reutiliza una instancia; esa instancia sí ocupa memoria. No se crea una nueva al acceder a ella normalmente.
- Una interfaz sellada puede declarar propiedades; las clases que la implementan pueden guardar los valores correspondientes.
- En los constructores sellados, la visibilidad predeterminada es `protected`; también puede declararse `private` cuando el diseño lo permita.
- El modelo describe las alternativas. El callback o `StateFlow` es el mecanismo que las transporta o expone.

<br/><br/>

## Referencias oficiales

- [Enum classes](https://kotlinlang.org/docs/enum-classes.html): constantes, propiedades y uso en `when`.
- [Sealed classes e interfaces](https://kotlinlang.org/docs/sealed-classes.html): jerarquías y exhaustividad.
- [Genéricos](https://kotlinlang.org/docs/generics.html): covarianza con `out`.
- [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/): estado actual, igualdad y recolección.
- [Artefacto JVM de corrutinas 1.10.1](https://repo.maven.apache.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-core-jvm/1.10.1/): dependencia usada para comprobar la ampliación.


<br/><br/>

## Código completo principal

El siguiente listado es idéntico al archivo `Demo_1_3.kt` y se copia completo en ese archivo.

```kotlin
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
```

## Código completo de la ampliación opcional

El siguiente listado se guarda en `Demo_1_3_StateFlow.kt`, como archivo independiente.

```kotlin
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
```

