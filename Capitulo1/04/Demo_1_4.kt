package tema14

// Tema 1.4: Patrones avanzados de programacion funcional.
// Kotlin/JVM de consola. Solo biblioteca estandar: no requiere COR13 ni Arrow.
// Todo el codigo esta terminado. No hay ejercicios pendientes.
// Compilar: kotlinc Demo_1_4.kt -include-runtime -d demo14.jar
// Ejecutar todo: java -jar demo14.jar
// Ejecutar solo un bloque: java -jar demo14.jar 4

private fun cabecera(numero: Int, titulo: String) {
    println("\n=== DEMO $numero: $titulo ===")
}

// FUNCION PURA: depende de su argumento y no modifica datos externos.
// trim y lowercase devuelven textos; el String original no se modifica.
private fun normalizarEmail(email: String): String = email.trim().lowercase()

private fun demo1() {
    cabecera(1, "Funcion pura y datos inmutables")
    val original = "  ANA@EJEMPLO.COM  "
    val primera = normalizarEmail(original)
    val segunda = normalizarEmail(original)

    // Imprimimos FUERA de normalizarEmail: la presentacion es un efecto.
    println("Original: [$original]")
    println("Primera llamada: [$primera]")
    println("Segunda llamada: [$segunda]")
    println("Misma entrada, mismo resultado: ${primera == segunda}")
    println("Original despues: [$original]")
}

private fun sumar(a: Int, b: Int): Int = a + b

// Primero recibe a y devuelve una funcion. Esa funcion recibe b.
private val sumarCurrificada: (Int) -> (Int) -> Int = { a ->
    { b -> a + b }
}

// Crear esta funcion NO valida un texto todavia: configura la longitud.
private fun validarLongitud(minimo: Int): (String) -> Boolean = { texto ->
    texto.length >= minimo
}

private fun demo2() {
    cabecera(2, "Currificacion y aplicacion parcial")
    println("Funcion normal: ${sumar(5, 3)}")
    println("Funcion currificada: ${sumarCurrificada(5)(3)}")

    // Aplicacion parcial: fijamos a = 5; todavia falta recibir b.
    val sumarCinco: (Int) -> Int = sumarCurrificada(5)
    println("sumarCinco(3): ${sumarCinco(3)}")
    println("sumarCinco(10): ${sumarCinco(10)}")

    // La lambda devuelta conserva el minimo mediante una captura (closure).
    val validarMinimo8 = validarLongitud(8)
    println("Longitud de abc suficiente: ${validarMinimo8("abc")}")
    println("Longitud de Password123 suficiente: ${validarMinimo8("Password123")}")
}

// f compose g crea una funcion que aplica PRIMERO g y DESPUES f.
// A: tipo inicial; B: tipo intermedio; C: tipo final.
private infix fun <A, B, C> ((B) -> C).compose(antes: (A) -> B): (A) -> C {
    val despues = this
    return { entrada -> despues(antes(entrada)) }
}

private fun demo3() {
    cabecera(3, "Composicion: el orden importa")
    val sumarUno: (Int) -> Int = { it + 1 }
    val duplicar: (Int) -> Int = { it * 2 }

    val primeroSumar = duplicar compose sumarUno
    val primeroDuplicar = sumarUno compose duplicar
    println("Sumar uno y duplicar, con 4: ${primeroSumar(4)}")
    println("Duplicar y sumar uno, con 4: ${primeroDuplicar(4)}")

    val quitarEspacios: (String) -> String = { it.trim() }
    val aMinusculas: (String) -> String = { it.lowercase() }
    val prepararEmail = aMinusculas compose quitarEspacios
    println("Texto preparado: ${prepararEmail("  ANA@EJEMPLO.COM  ")}")
    // Una composicion se guarda y se reutiliza como cualquier otra funcion.
}

// Either se DEFINE AQUI: no es un tipo de la biblioteca estandar de Kotlin.
// Por convenio: Left contiene el error y Right contiene el exito.
private sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()
    data class Right<out R>(val value: R) : Either<Nothing, R>()
}

// map recibe una funcion R -> T. Si hay exito, transforma su valor
// y lo envuelve de nuevo en Right. Si hay error, lo deja pasar sin ejecutar
// transform. Es una extension sobre nuestro Either, no List.map.
private fun <L, R, T> Either<L, R>.map(transform: (R) -> T): Either<L, T> =
    when (this) {
        is Either.Left -> this
        is Either.Right -> Either.Right(transform(value))
    }

// flatMap recibe una funcion R -> Either<L, T>. Como esa funcion YA devuelve
// Either, entregamos su resultado directamente: no agregamos otro Right.
private fun <L, R, T> Either<L, R>.flatMap(
    transform: (R) -> Either<L, T>
): Either<L, T> = when (this) {
    is Either.Left -> this
    is Either.Right -> transform(value)
}

private fun validarPositivo(numero: Int): Either<String, Int> =
    if (numero > 0) Either.Right(numero) else Either.Left("Debe ser positivo")

private fun demo4() {
    cabecera(4, "Either, map y flatMap con numeros")
    val exito: Either<String, Int> = Either.Right(5)
    val fallo: Either<String, Int> = Either.Left("Dato no disponible")
    println("map sobre Right: ${exito.map { it * 2 }}")
    println("map sobre Left: ${fallo.map { it * 2 }}")

    // validarPositivo ya devuelve Either. Con map se anidan los contenedores.
    val anidado: Either<String, Either<String, Int>> = exito.map(::validarPositivo)
    val directo: Either<String, Int> = exito.flatMap(::validarPositivo)
    println("map con validarPositivo: $anidado")
    println("flatMap con validarPositivo: $directo")

    val negativo: Either<String, Int> = Either.Right(-2)
    println("flatMap puede producir un error: ${negativo.flatMap(::validarPositivo)}")
    println("flatMap conserva el error previo: ${fallo.flatMap(::validarPositivo)}")
}

// Errores tipados: cada alternativa tiene un significado concreto.
private sealed interface ValidationError {
    object EmailVacio : ValidationError
    object EmailInvalido : ValidationError
    data class PasswordMuyCorta(val minimo: Int) : ValidationError
}

// Regla de formato didactica: no pretende cubrir todos los correos posibles.
private val patronEmail = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

private fun validarEmail(email: String): Either<ValidationError, String> = when {
    email.isBlank() -> Either.Left(ValidationError.EmailVacio)
    !patronEmail.matches(email) -> Either.Left(ValidationError.EmailInvalido)
    else -> Either.Right(email)
}

// Aplicacion parcial: primero se fija minimo; despues se reciben contrasenas.
private fun crearValidadorPassword(
    minimo: Int
): (String) -> Either<ValidationError, String> = { password ->
    if (password.length >= minimo) Either.Right(password)
    else Either.Left(ValidationError.PasswordMuyCorta(minimo))
}

private val validarPassword8 = crearValidadorPassword(8)

private fun describirError(error: ValidationError): String = when (error) {
    ValidationError.EmailVacio -> "El correo esta vacio"
    ValidationError.EmailInvalido -> "El formato del correo es invalido"
    is ValidationError.PasswordMuyCorta -> "La clave requiere ${error.minimo} caracteres"
}

private fun describirValidacion(resultado: Either<ValidationError, String>): String =
    when (resultado) {
        is Either.Left -> "ERROR: ${describirError(resultado.value)}"
        is Either.Right -> "OK"
    }

private fun demo5() {
    cabecera(5, "Validaciones puras y errores tipados")
    println("Correo valido: ${describirValidacion(validarEmail("ana@ejemplo.com"))}")
    println("Correo vacio: ${describirValidacion(validarEmail(""))}")
    println("Correo sin arroba: ${describirValidacion(validarEmail("ana.ejemplo.com"))}")
    println("Clave corta: ${describirValidacion(validarPassword8("abc"))}")
    println("Clave suficiente: ${describirValidacion(validarPassword8("Password123"))}")
    // Los validadores devuelven datos. Solo esta presentacion imprime mensajes.
}

private data class UsuarioProcesado(val email: String)

private class RegistroUseCase {
    fun registrar(email: String, password: String): Either<ValidationError, UsuarioProcesado> {
        // PASO 1: preparar y validar correo.
        return validarEmail(normalizarEmail(email))
            // PASO 2: si el correo fue valido, validar la clave.
            .flatMap { emailValido ->
                validarPassword8(password)
                    // PASO 3: si la clave fue valida, crear el resultado.
                    .map { UsuarioProcesado(emailValido) }
            }
        // Ante el primer Left se conservan sus datos y se omiten los pasos
        // posteriores del encadenamiento. No se acumulan todos los errores.
    }
}

private fun presentarRegistro(resultado: Either<ValidationError, UsuarioProcesado>): String =
    when (resultado) {
        is Either.Left -> "ERROR: ${describirError(resultado.value)}"
        is Either.Right -> "EXITO: ${resultado.value.email}"
    }

private fun demo6() {
    cabecera(6, "Registro: todas las piezas juntas")
    val registro = RegistroUseCase()
    println("Caso 1, datos correctos: " +
        presentarRegistro(registro.registrar("  ANA@EJEMPLO.COM  ", "Password123")))
    println("Caso 2, correo vacio: " +
        presentarRegistro(registro.registrar("", "Password123")))
    println("Caso 3, correo invalido: " +
        presentarRegistro(registro.registrar("ana.ejemplo.com", "Password123")))
    println("Caso 4, clave corta: " +
        presentarRegistro(registro.registrar("ana@ejemplo.com", "abc")))
    println("Caso 5, ambos invalidos: " +
        presentarRegistro(registro.registrar("", "abc")))
    // Registro simulado en memoria: devuelve un usuario procesado.
}

fun main(args: Array<String>) {
    val demos: List<() -> Unit> = listOf(::demo1, ::demo2, ::demo3, ::demo4, ::demo5, ::demo6)
    if (args.isEmpty()) {
        demos.forEach { it() }
        return
    }
    val numero = args.singleOrNull()?.toIntOrNull()
    if (numero == null || numero !in 1..demos.size) {
        println("Uso: java -jar demo14.jar [1..6]")
        return
    }
    demos[numero - 1]()
}
