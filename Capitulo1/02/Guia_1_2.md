# Demo 1.2 — Lambdas, funciones de orden superior y scope functions

**Caso:** explorar operaciones y preparar datos de usuarios para presentarlos.

Material complementario a las hojas compartidas de la PPT. Las actividades se realizan en Kotlin/JVM de consola, con `kotlinc` y Java. No requieren Android, Android Studio, Gradle ni bibliotecas adicionales.

<br/><br/>

## Objetivo

Al terminar podrás leer un tipo de función, pasar y devolver lambdas, explicar los modificadores de inlining y elegir una scope function según el acceso al objeto y el resultado que necesitas.

<br/><br/>

## Archivos y ejecución

- `Demo_1_2.kt`: código inicial ejecutable, con comentarios y marcas R1–R8.
- `Guia_1_2.md`: explicaciones, instrucciones y código completo al final.
- `Respuestas_1_2.md`: cambios resueltos, salidas esperadas y explicación  

Guarda el archivo `.kt` en tu carpeta de trabajo y abre ahí PowerShell o CMD. Necesitas tener `kotlinc` y `java` disponibles en la terminal.

```powershell
kotlinc Demo_1_2.kt -include-runtime -d demo12.jar
java -jar demo12.jar
```

Para trabajar solo una demo, agrega su número:

```powershell
java -jar demo12.jar 1
java -jar demo12.jar 7
```

**Después de cada cambio, vuelve a compilar antes de ejecutar el JAR.** Si la compilación falla, el JAR anterior puede seguir existiendo: su salida correspondería al código anterior.

El archivo inicial funciona completo. Los retos están en comentarios y se resuelven agregando o cambiando unas cuantas líneas. Primero anticipa el resultado; después ejecuta y explícalo.


<br/><br/>

## Ruta de trabajo

| Bloque | Concepto principal | Cambio del participante |
| --- | --- | --- |
| 1 | Lambda, tipo de función, última expresión y `Unit` | Crear `triplicar` con `it` |
| 2 | Recibir y devolver funciones; trailing lambda y captura | Crear una función que multiplique por cuatro |
| 3 | `filter`, `map` y parámetro implícito | Conservar impares |
| 4 | `inline` y retornos locales/no locales | Cambiar el destino de un `return` |
| 5 | `noinline` y función guardada | Invocar de nuevo una acción pendiente |
| 6 | `crossinline` y retorno con etiqueta | Omitir parte de la tarea |
| 7 | `let`, `apply`, `run`, `also`, `with` y `run` global | Activar un `?.let` con un valor no nulo |
| 8 | Integración: callbacks, genéricos, nulabilidad y mapeo | Validar que el usuario esté activo |

<br/><br/>

## Demo 1 — Guardar una función no la ejecuta

Ejecuta `java -jar demo12.jar 1`.

Lee primero esta declaración:

```kotlin
val duplicar: (Int) -> Int = { numero ->
    println("Dentro de la lambda: $numero")
    numero * 2
}
```

- `duplicar` guarda un valor de tipo función.
- `(Int) -> Int` dice que recibe un entero y produce un entero.
- `numero` es el parámetro de la lambda.
- `numero * 2` es la última expresión: entrega el resultado.
- `duplicar(5)` invoca la función guardada y entonces ejecuta el cuerpo.

La salida `Lambda creada; todavia no se ha invocado` aparece antes del mensaje interno de la lambda. Observa también `sumar`, con dos parámetros, y `saludar`, de tipo `() -> Unit`: cero parámetros y ningún resultado útil para el llamador.

**R1.** Al final de `demo1`, escribe una lambda `triplicar: (Int) -> Int` usando el parámetro implícito `it`. Invócala con `5` e imprime el resultado. Conserva la lambda `duplicar` para comparar.

<br/><br/>

## Demo 2 — Una función puede recibir o devolver otra función

Ejecuta `java -jar demo12.jar 2`.

En `ejecutarOperacion`, el primer parámetro es un número y el segundo es una función. La instrucción `operacion(valor)` ejecuta la función recibida. Las tres primeras llamadas imprimen `15`: cambia la escritura del argumento, pero la operación es la misma.

```kotlin
ejecutarOperacion(5, sumarDiez)
ejecutarOperacion(5, { it + 10 })
ejecutarOperacion(5) { it + 10 }
```

La tercera escritura usa **trailing lambda**: una lambda que corresponde al último parámetro puede colocarse fuera de los paréntesis. Pasar `sumarDiez` entrega la función; escribir `sumarDiez(5)` la ejecutaría y produciría un entero.

Después, `crearMultiplicador(3)` devuelve una lambda. Esta conserva acceso a `factor`, aunque la llamada que la creó ya terminó. Eso se conoce como captura de una variable del entorno o **closure**. La multiplicación ocurre al llamar `porTres(5)`.

**R2.** Crea `porCuatro` usando `crearMultiplicador(4)` e imprime el resultado de invocarla con `6`.

<br/><br/>

## Demo 3 — La función receptora determina para qué se usa la lambda

Ejecuta `java -jar demo12.jar 3`.

`filter` llama a su lambda con cada número y usa el `Boolean` devuelto para decidir si lo conserva. `map` transforma los elementos seleccionados. Las salidas iniciales son:

```text
Original: [1, 2, 3, 4, 5, 6]
Seleccionados: [2, 4, 6]
Transformados: [20, 40, 60]
```

`{ numero -> numero * 10 }` y `{ it * 10 }` expresan lo mismo aquí. Usa un nombre explícito cuando ayude a entender el significado. `it` es un parámetro implícito disponible en lambdas de un parámetro; no representa automáticamente el objeto de cualquier bloque.

**R3.** Cambia solo la condición de `filter` para conservar los impares. Predice las listas seleccionada y transformada antes de ejecutar. Observa que la lista original conserva sus valores.

<br/><br/>

## Demo 4 — `inline` y el destino de un retorno

Ejecuta `java -jar demo12.jar 4`.

Las variantes normal e inline imprimen la misma secuencia de tres mensajes: inicio, saludo y fin. `inline` afecta la compilación: inserta el cuerpo de la función y sus lambdas elegibles en el sitio de llamada. La salida por sí sola no demuestra la forma del código generado ni una mejora de rendimiento.

En `demostrarRetorno(0)`, el `return` sin etiqueta dentro de la lambda sale de **`demostrarRetorno`**. Es un retorno no local, permitido para esta lambda inline. Por ello se omiten `Fin inline` de esa llamada y `Continua demostrarRetorno`. `Continua demo4` sí aparece, porque `demo4` sigue después de llamar a `demostrarRetorno`.

**R4.** Cambia únicamente esta línea de `demostrarRetorno`:

```kotlin
if (valor == 0) return
```

Usa `return@ejecutarInline` en lugar de `return`. Anticipa qué mensajes finales aparecerán. La etiqueta indica que se abandona la lambda, permitiendo que `ejecutarInline` continúe después de `accion()`.

**Precisión para la explicación oral:** el inlining puede evitar el objeto asociado a una lambda y su invocación indirecta; no garantiza cero asignaciones de memoria para toda la operación. La representación y reutilización de lambdas sin inline dependen del compilador y de la plataforma. Para esta demo no necesitamos medir memoria ni tiempos.


<br/><br/>

## Demo 5 — `noinline`: conservar la función como valor

Ejecuta `java -jar demo12.jar 5`.

`prepararAcciones` ejecuta `inmediata`, devuelve `posterior` y termina. La acción posterior se ejecuta después, cuando el código llama a `pendiente()`.

```text
Ahora: preparar datos
La accion pendiente ya esta guardada
Despues: mostrar reporte
```

Como `prepararAcciones` es inline, el parámetro que queremos devolver directamente se marca `noinline`. Así puede conservarse como un valor de función normal. En una función que no es inline, devolver una lambda no requiere este modificador.

**R5.** Agrega una segunda llamada a `pendiente()` inmediatamente después de la primera. Predice cuántas veces aparece cada mensaje. Una función guardada no se consume al invocarla.

<br/><br/>

## Demo 6 — `crossinline`: controlar desde dónde se puede regresar

Ejecuta `java -jar demo12.jar 6`.

`prepararTarea` envuelve la acción en un `Runnable`. `crossinline` impide que la lambda use un retorno no local hacia la función llamadora. Un retorno local con etiqueta sí es válido.

Con `omitir = false` aparece:

```text
Tarea creada
Entrada a la tarea
Procesando datos
Continua demo6
```

**R6.** Cambia `omitir` a `true`. Identifica qué mensaje deja de aparecer y explica por qué `Continua demo6` sí se imprime.

`tarea.run()` es una llamada normal: aquí todo se ejecuta en el mismo hilo. Crear un `Runnable`, usar `crossinline` o guardar una lambda no inicia por sí mismo otro hilo ni una corrutina.

<br/><br/>

## Demo 7 — Scope functions: dos preguntas por llamada

Ejecuta `java -jar demo12.jar 7`.

Antes de leer cada bloque, pregunta: **¿cómo accedo al objeto?** y **¿qué valor recibe la variable de la izquierda?**

| Función | Acceso al objeto del bloque | Valor que devuelve | En esta demo |
| --- | --- | --- | --- |
| `apply` | `this` | El mismo objeto | `Configuracion` configurada |
| `also` | `it` | El mismo objeto | `Configuracion` observada |
| `let` | `it` | Resultado del bloque | `Int`: longitud del nombre |
| `run` con receptor | `this` | Resultado del bloque | `String`: resumen |
| `with(objeto)` | `this` | Resultado del bloque | `Int`: doble de intentos |
| `run { }` global | Sin receptor propio | Resultado del bloque | `Int`: producto de variables locales |

`this` es el objeto receptor: permite escribir `nombre` en lugar de `this.nombre`. `it` es un argumento: para acceder a su propiedad escribimos `it.nombre`. En ambos casos se trabaja sobre el objeto recibido; estas funciones no crean automáticamente una copia.

Los operadores `===` comprueban identidad: que ambas referencias apuntan al mismo objeto. La configuración conserva las modificaciones hechas con `apply`, y `also` devuelve ese mismo objeto aunque dentro se llame a `println`.

**R7.** Cambia `nombreOpcional` de `null` a `"  Ana  "`, con dos espacios a cada lado. Predice si se ejecutará el bloque y qué contendrá `limpio` después de `trim()`.

En `nombreOpcional?.let { ... }`, el operador **`?.`** omite la llamada cuando el valor es nulo. `let` por sí solo también puede llamarse sobre un valor nullable y entonces recibiría ese valor nulo.


<br/><br/>

## Demo 8 — Integrar las piezas: preparar un usuario para presentarlo

Ejecuta `java -jar demo12.jar 8`.

Lee el recorrido en este orden:

1. `UsuarioDto` representa los datos de entrada, con campos que pueden ser nulos.
2. `convertirUsuario` llama a la función de orden superior `ejecutarTransaccionSegura`.
3. `onError` recibe una lambda que imprime el mensaje de validación.
4. La trailing lambda corresponde al parámetro `bloque` y recibe un `UsuarioDto` no nulo.
5. `usuario.run` usa al DTO como `this` y devuelve un `UsuarioUiModel` como última expresión.
6. `requireNotNull(id)` valida el identificador. Si es nulo, lanza `IllegalArgumentException`, que la función auxiliar atiende devolviendo `null`.
7. `?:` aporta valores alternativos para nombre y correo nulos.
8. `modelo?.also` registra el resultado si existe un modelo.

En la firma genérica, `T` representa la entrada y `R` la salida. En esta llamada se infieren `UsuarioDto` y `UsuarioUiModel`. El retorno `R?` permite comunicar que no se produjo resultado.

El nombre de la función auxiliar mantiene el vínculo con el caso de la PPT. En esta demo no hay base de datos ni rollback: se comprueba un recurso opcional y se transforma. El manejo de errores se limita a las validaciones previstas.

| Caso | Datos relevantes | Comportamiento inicial |
| --- | --- | --- |
| 1 | Ana con ID | Produce modelo con nombre `ANA` |
| 2 | Luis sin ID | Imprime error y no produce modelo |
| 3 | Recurso `null` | Imprime error y no produce modelo |
| 4 | Eva con `esActivo = false` | Produce modelo: todavía no se valida ese campo |
| 5 | ID válido, nombre y correo nulos | Usa los valores alternativos |

**R8.** Inmediatamente después de obtener `idSeguro`, agrega una validación con `require(esActivo)` y mensaje `"Usuario inactivo"`. Anticipa cuál caso cambia y ejecuta de nuevo.

<br/><br/>

## Cierre: lo que debes poder explicar

- Diferenciar guardar una lambda, pasarla e invocarla.
- Leer los tipos `(Int) -> Int`, `(Int) -> Boolean` y `() -> Unit`.
- Reconocer qué lambda corresponde a cada parámetro de una función.
- Distinguir un retorno de la lambda de un retorno de la función que la contiene.
- Explicar por qué una lambda se marca `noinline` o `crossinline`.
- Elegir una scope function por su acceso al objeto y por su resultado.
- Seguir el mapeo desde el DTO hasta el modelo de presentación.

<br/><br/>

## Referencias oficiales

- [Funciones de orden superior y lambdas](https://kotlinlang.org/docs/lambdas.html): tipos de función, última expresión, trailing lambdas y captura.
- [Funciones inline](https://kotlinlang.org/docs/inline-functions.html): inlining, `noinline`, `crossinline` y retornos no locales.
- [Scope functions](https://kotlinlang.org/docs/scope-functions.html): receptor, argumento y resultado de cada función.

<br/><br/>

## Código completo de la demo inicial

El siguiente listado es el mismo contenido de `Demo_1_2.kt`. Puedes copiarlo a ese archivo para trabajar con una sola guía.

```kotlin
// Tema 1.2: Lambdas, funciones de orden superior y scope functions.
// Kotlin/JVM de consola. Solo biblioteca estandar; sin Android ni dependencias.
// Compilar: kotlinc Demo_1_2.kt -include-runtime -d demo12.jar
// Ejecutar todo: java -jar demo12.jar
// Ejecutar un bloque: java -jar demo12.jar 7
// Los retos R1-R8 estan comentados: el archivo inicial ya puede ejecutarse.

fun cabecera(numero: Int, titulo: String) {
    println("\n=== DEMO $numero: $titulo ===")
}

fun demo1() {
    cabecera(1, "Una funcion guardada en una variable")

    // (Int) -> Int: recibe UN entero y produce UN entero.
    // Guardar la lambda NO ejecuta su cuerpo.
    val duplicar: (Int) -> Int = { numero ->
        println("Dentro de la lambda: $numero")
        numero * 2 // Ultima expresion: este es el resultado, sin escribir return.
    }
    println("Lambda creada; todavia no se ha invocado")
    val resultado = duplicar(5) // Aqui se ejecuta el cuerpo con numero = 5.
    println("Resultado: $resultado")

    // El tipo a la izquierda permite inferir el tipo de a y b.
    val sumar: (Int, Int) -> Int = { a, b -> a + b }
    println("Suma: ${sumar(3, 4)}")

    // () indica cero parametros; Unit indica que no produce un resultado util.
    val saludar: () -> Unit = { println("Hola, grupo") }
    saludar()

    // R1: agregar una lambda triplicar: (Int) -> Int usando it.
    // Invocarla con 5 e imprimir el resultado. No cambiar duplicar.
}

// Es de orden superior porque RECIBE una funcion.
// operacion es el parametro; no es una palabra reservada.
fun ejecutarOperacion(valor: Int, operacion: (Int) -> Int): Int {
    return operacion(valor) // Nuestra funcion decide CUANDO ejecutar la lambda.
}

// Tambien es de orden superior porque DEVUELVE una funcion.
fun crearMultiplicador(factor: Int): (Int) -> Int {
    // La lambda captura factor: podra usarlo despues de terminar esta llamada.
    return { numero -> numero * factor }
}

fun demo2() {
    cabecera(2, "Recibir y devolver funciones")
    val sumarDiez: (Int) -> Int = { it + 10 }

    // Pasamos la funcion guardada, sin invocarla previamente.
    println("Variable: ${ejecutarOperacion(5, sumarDiez)}")
    println("Dentro: ${ejecutarOperacion(5, { it + 10 })}")
    // La lambda puede ir fuera porque corresponde al ULTIMO parametro.
    println("Fuera: ${ejecutarOperacion(5) { it + 10 }}")

    val porTres = crearMultiplicador(3) // Devuelve la lambda; no multiplica aun.
    println("Funcion devuelta: ${porTres(5)}")

    // R2: crear porCuatro mediante crearMultiplicador e invocarla con 6.
}

fun demo3() {
    cabecera(3, "it, filter y map")
    val numeros = listOf(1, 2, 3, 4, 5, 6)

    // filter espera (Int) -> Boolean: true conserva el elemento.
    val seleccionados = numeros.filter { numero -> numero % 2 == 0 }
    // map espera una transformacion: produce un resultado por elemento.
    // it es el parametro implicito cuando la lambda tiene UN solo parametro.
    val transformados = seleccionados.map { it * 10 }

    println("Original: $numeros")
    println("Seleccionados: $seleccionados")
    println("Transformados: $transformados")

    // R3: cambiar SOLO la condicion de filter para conservar los impares.
    // Antes de ejecutar, anticipar ambas listas de salida.
}

fun ejecutarNormal(accion: () -> Unit) {
    println("Inicio normal")
    accion()
    println("Fin normal")
}

inline fun ejecutarInline(accion: () -> Unit) {
    println("Inicio inline")
    accion()
    println("Fin inline")
}

fun demostrarRetorno(valor: Int) {
    ejecutarInline {
        println("Dentro del bloque")
        if (valor == 0) return // Sale de demostrarRetorno: retorno NO local.
        println("Valor distinto de cero")
    }
    println("Continua demostrarRetorno")
}

fun demo4() {
    cabecera(4, "inline y el destino de return")
    ejecutarNormal { println("Hola") }
    ejecutarInline { println("Hola") }
    // La misma salida no demuestra como se genero el bytecode.
    // inline inserta cuerpo y lambdas elegibles en el sitio de llamada.
    // No significa que toda la operacion quede libre de objetos.
    demostrarRetorno(0)
    println("Continua demo4")

    // R4: en demostrarRetorno, cambiar return por return@ejecutarInline.
    // Predecir cuales de los mensajes finales apareceran ahora.
}

inline fun prepararAcciones(
    inmediata: () -> Unit,
    noinline posterior: () -> Unit
): () -> Unit {
    inmediata()
    // posterior se conserva como valor para usarlo fuera de esta llamada.
    // Dentro de una funcion inline necesitamos noinline para devolverla asi.
    return posterior
}

fun demo5() {
    cabecera(5, "noinline: guardar para despues")
    val pendiente = prepararAcciones(
        inmediata = { println("Ahora: preparar datos") },
        posterior = { println("Despues: mostrar reporte") }
    )
    println("La accion pendiente ya esta guardada")
    pendiente() // Tener la funcion y ejecutarla son pasos distintos.

    // R5: agregar otra llamada a pendiente(). Cuantas veces aparece Despues?
}

inline fun prepararTarea(crossinline accion: () -> Unit): Runnable {
    // Runnable es una interfaz de Java. Aqui envolvemos la accion.
    // crossinline impide que la lambda intente salir de su funcion llamadora.
    return Runnable { accion() }
}

fun demo6() {
    cabecera(6, "crossinline: un retorno dentro del bloque")
    val omitir = false // R6: cambiar a true y anticipar la salida.
    val tarea = prepararTarea {
        println("Entrada a la tarea")
        if (omitir) return@prepararTarea // Sale SOLO de esta lambda.
        // Un return sin etiqueta no esta permitido aqui por crossinline.
        println("Procesando datos")
    }
    println("Tarea creada")
    tarea.run() // Llamada normal en el MISMO hilo; no inicia otro hilo.
    println("Continua demo6")
}

data class Configuracion(
    var nombre: String = "Sin nombre",
    var intentos: Int = 1
)

fun demo7() {
    cabecera(7, "Scope functions: acceso y resultado")
    val original = Configuracion()

    val configurada: Configuracion = original.apply {
        // this es original; se puede omitir al acceder a sus propiedades.
        nombre = "Laboratorio"
        intentos = 3
    } // apply devuelve original, no un objeto nuevo.
    println("apply mismo objeto: ${configurada === original}")

    val observada: Configuracion = configurada.also {
        println("also observa: ${it.nombre}")
    } // El println produce Unit; also de todos modos devuelve configurada.
    println("also mismo objeto: ${observada === original}")

    val largo: Int = configurada.let {
        it.nombre.length // let devuelve esta expresion; aqui es un Int.
    }
    println("let longitud: $largo")

    val resumen: String = configurada.run {
        // this es configurada. La ultima expresion es un String.
        "$nombre / $intentos intentos"
    }
    println("run resumen: $resumen")

    val doble: Int = with(configurada) {
        // Recibe el objeto como argumento: with(objeto) { ... }.
        intentos * 2
    }
    println("with doble: $doble")

    val total: Int = run {
        // Esta variante NO tiene objeto receptor propio ni it implicito.
        val porBloque = 4
        val bloques = 2
        porBloque * bloques
    }
    println("run global total: $total")

    val nombreOpcional: String? = null // R7: cambiar a "  Ana  ".
    val limpio: String? = nombreOpcional?.let {
        println("Se ejecuto el let del nombre")
        it.trim()
    }
    // Es ?. quien evita entrar si el valor es null; let solo no hace esa prueba.
    println("Nombre limpio: ${limpio ?: "Sin nombre"}")
}

// DTO: forma en que recibimos datos. UiModel: forma para presentarlos.
// Son solo clases de Kotlin: aqui no hay servidor ni pantalla real.
data class UsuarioDto(
    val id: String?,
    val nombre: String?,
    val email: String?,
    val esActivo: Boolean
)

data class UsuarioUiModel(
    val id: String,
    val nombreCompleto: String,
    val correo: String
)

// T es el tipo de entrada y R el de salida; T : Any excluye un T nullable.
// recurso: T? permite recibir null; R? permite comunicar falta de resultado.
inline fun <T : Any, R : Any> ejecutarTransaccionSegura(
    recurso: T?,
    onError: (String) -> Unit,
    bloque: (T) -> R
): R? {
    if (recurso == null) {
        onError("El recurso es nulo")
        return null
    }
    return try {
        bloque(recurso)
    } catch (e: IllegalArgumentException) {
        // Para esta demo solo atendemos errores de validacion previstos.
        onError(e.message ?: "Datos invalidos")
        null
    }
    // El nombre conserva el vinculo con la PPT; no hay una transaccion de BD
    // ni rollback. Solo validamos/transformamos un recurso opcional.
}

fun convertirUsuario(dto: UsuarioDto?): UsuarioUiModel? {
    // Aqui T = UsuarioDto y R = UsuarioUiModel; el compilador los infiere.
    return ejecutarTransaccionSegura(
        recurso = dto,
        onError = { mensaje -> println("ERROR: $mensaje") }
    ) { usuario -> // Trailing lambda: corresponde al parametro bloque.
        usuario.run {
            // Dentro de run, this es el UsuarioDto recibido.
            val idSeguro = requireNotNull(id) { "ID invalido" }
            // R8: agregar aqui require(esActivo) { "Usuario inactivo" }.
            UsuarioUiModel(
                id = idSeguro,
                nombreCompleto = (nombre ?: "Anonimo").uppercase(),
                correo = email ?: "sin_correo@dominio.com"
            ) // Ultima expresion de run: devuelve UsuarioUiModel.
        }
    }
}

fun demo8() {
    cabecera(8, "Integracion: DTO a modelo de presentacion")
    val casos: List<UsuarioDto?> = listOf(
        UsuarioDto("USR_1", "Ana", "ana@app.com", true),
        UsuarioDto(null, "Luis", null, true),
        null,
        UsuarioDto("USR_4", "Eva", null, false),
        UsuarioDto("USR_5", null, null, true)
    )

    casos.forEachIndexed { indice, dto ->
        println("Caso ${indice + 1}")
        val modelo = convertirUsuario(dto)
        modelo?.also {
            // also registra un efecto secundario y devuelve el mismo modelo.
            println("Listo: ${it.id} | ${it.nombreCompleto} | ${it.correo}")
        }
        if (modelo == null) println("Sin modelo de presentacion")
    }
    // R8: tras agregar la validacion, cual caso cambia y por que?
}

fun main(args: Array<String>) {
    // Lista de funciones: se guardan referencias, no se ejecutan al construirla.
    val demos: List<() -> Unit> = listOf(
        ::demo1, ::demo2, ::demo3, ::demo4, ::demo5, ::demo6, ::demo7, ::demo8
    )
    if (args.isEmpty()) {
        demos.forEach { demo -> demo() }
    } else {
        val numero = args.singleOrNull()?.toIntOrNull()
        if (numero == null || numero !in 1..demos.size) {
            println("Uso: java -jar demo12.jar [1..8]")
            return
        }
        demos[numero - 1]()
    }
}
```



 
