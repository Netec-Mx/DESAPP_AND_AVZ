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
