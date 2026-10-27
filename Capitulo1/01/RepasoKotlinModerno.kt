/*
 * Tema 1.1: Repaso avanzado de Kotlin moderno.
 * Nueve demos de consola. Kotlin/JVM 2.0 o superior; solo biblioteca estandar.
 *
 * COMPILAR (Windows, desde la carpeta del archivo):
 *   kotlinc RepasoKotlinModerno.kt -include-runtime -d repaso.jar
 * EJECUTAR UNA DEMO:
 *   java -jar repaso.jar 1
 * EJECUTAR TODAS:
 *   java -jar repaso.jar todas
 * VER INDICE:
 *   java -jar repaso.jar
 *
 * Como leer el archivo:
 * 1. Busca DEMO 1, DEMO 2, etc. Cada funcion comienza con objetivo y ejercicio.
 * 2. Lee los comentarios y predice la salida antes de ejecutar.
 * 3. Las lineas marcadas ERROR_INTENCIONAL permanecen comentadas para que
 *    el archivo compile. Activa solo una por vez, compila y vuelve a comentarla.
 *
 * Los ejemplos son material complementario a las diapositivas.
 * No requieren IDE, Gradle, red, bibliotecas externas ni concurrencia.
 */

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// -----------------------------------------------------------------------------
// DEMO 1. Inferencia y analisis de tipos con K2.
// -----------------------------------------------------------------------------

private fun analizarDato(dato: Any?) {
    // dato es un parametro: el compilador NO sabe de antemano que recibira.
    // is comprueba un tipo. El resultado es un Boolean, no una conversion.
    val esTexto = dato is String

    // K2 conserva la relacion entre esTexto y la comprobacion anterior.
    // Dentro del if puede tratar dato como String y permitir dato.length.
    if (esTexto) {
        println("Es texto: '$dato'; longitud = ${dato.length}")
    } else {
        println("No es texto: $dato")
    }
}

private fun demo1() {
    // OBJETIVO: distinguir el tipo que se deduce del tipo que se comprueba.
    val cantidad = 3             // Inferencia: Int. No es tipado dinamico.
    val saludo = "Hola"          // Inferencia: String.
    println("Cantidad + 1 = ${cantidad + 1}")
    println("Saludo en mayusculas = ${saludo.uppercase()}")

    analizarDato("Kotlin")
    analizarDato(42)
    analizarDato(null)

    // ERROR_INTENCIONAL_INFERENCIA: cantidad no cambia de tipo al compilar.
    // val numero: Int = "tres"

    // EJERCICIO: pasa "K2" a analizarDato. Debe informar longitud = 2.
    // Esta demo muestra analisis de tipos, NO mide velocidad de compilacion
    // ni prueba una diferencia de rendimiento entre K1 y K2.
}

// -----------------------------------------------------------------------------
// DEMO 2. Value classes: tipos distintos para valores con distinto significado.
// Estos tipos tambien se reutilizan en la demo 9.
// -----------------------------------------------------------------------------

@JvmInline
value class CorreoElectronico(val valor: String) {
    init {
        // init se ejecuta al construir el valor. require comprueba una condicion
        // en ejecucion; si es falsa, lanza IllegalArgumentException.
        // Revisar solo '@' es una regla didactica, NO una validacion completa
        // de todos los formatos de correo.
        require(valor.contains('@')) { "El correo debe contener @" }
    }
}

@JvmInline
value class TokenAcceso(val token: String)

private fun enviarAviso(destino: CorreoElectronico) {
    println("Aviso dirigido a ${destino.valor}")
}

private fun demo2() {
    // OBJETIVO: observar seguridad de tipos y validacion como mecanismos distintos.
    val correo = CorreoElectronico("everardo@ejemplo.com")
    val token = TokenAcceso("abc123")
    enviarAviso(correo)
    println("Texto del token = ${token.token}")

    // ERROR_INTENCIONAL_TIPO: String no es CorreoElectronico.
    // enviarAviso("everardo@ejemplo.com")

    // ERROR_INTENCIONAL_TOKEN: TokenAcceso tampoco es CorreoElectronico.
    // enviarAviso(token)

    // Esto SI compila: se construye el tipo correcto. Pero su init rechaza
    // el contenido durante la ejecucion. Capturamos el error para continuar.
    try {
        CorreoElectronico("sin-arroba")
    } catch (error: IllegalArgumentException) {
        println("Validacion en ejecucion: ${error.message}")
    }

    // Una value class puede representarse mediante su valor interno.
    // No se garantiza ausencia de envoltorios en todos los usos: por ejemplo,
    // usarla como Any requiere boxing. Tampoco implica 'siempre en la pila'.
    val comoOtroTipo: Any = correo
    println("Correo a traves de Any = $comoOtroTipo")

    // EJERCICIO: activa enviarAviso(token), compila y observa que el error
    // aparece ANTES de ejecutar. Despues vuelve a comentar la linea.
}

// -----------------------------------------------------------------------------
// DEMO 3. Delegado propio con trazas de getValue y setValue.
// -----------------------------------------------------------------------------

private class RegistroDelegate(inicial: String) : ReadWriteProperty<Any?, String> {
    // Cada objeto RegistroDelegate conserva su propio valor.
    private var almacenado = inicial

    init {
        // Construir el delegado NO equivale a asignar mediante setValue.
        println("[CREAR delegado] valor inicial = '$inicial'")
    }

    // ReadWriteProperty exige estos dos metodos.
    // Los operadores se heredan de la interfaz: basta escribir override.
    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        // thisRef: objeto propietario (Ficha en esta demo).
        // property: metadatos de la propiedad; property.name obtiene su nombre.
        // KProperty<*> permite describir propiedades sin fijar aqui su tipo.
        // No necesitamos instalar kotlin-reflect para consultar este nombre.
        val propietario = thisRef?.javaClass?.simpleName ?: "sin propietario"
        println("[GET] $propietario.${property.name} devuelve '$almacenado'")
        return almacenado
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        // value: nuevo String que el codigo quiere asignar a la propiedad.
        println("[SET] ${property.name}: '$almacenado' -> '$value'")
        almacenado = value
    }
}

private class Ficha {
    // Reutilizamos una clase de delegado con dos INSTANCIAS independientes.
    // by encarga los accesos a los metodos del delegado correspondiente.
    var alias: String by RegistroDelegate("Inicial")
    var ciudad: String by RegistroDelegate("CDMX")
}

private fun demo3() {
    // OBJETIVO: ver que escribir y leer una propiedad llama metodos diferentes.
    val ficha = Ficha()                    // Construye los dos delegados.
    println("Lectura inicial: ${ficha.alias}") // Llama a getValue.
    ficha.alias = "KotlinPro"             // Llama a setValue.
    println("Lectura final: ${ficha.alias}")  // Llama a getValue otra vez.
    println("Ciudad independiente: ${ficha.ciudad}")

    // Conceptualmente: leer ficha.alias pide al delegado getValue(ficha, ...).
    // Asignar ficha.alias pide setValue(ficha, ..., nuevoValor).
    // El compilador genera la redireccion: nosotros no llamamos esos metodos.

    // EJERCICIO: agrega ficha.ciudad = "Puebla" y luego leela.
    // Identifica las trazas SET y GET; alias debe conservar KotlinPro.
}

// -----------------------------------------------------------------------------
// DEMO 4. lazy: calcular al leer por primera vez y conservar el resultado.
// -----------------------------------------------------------------------------

private class Configuracion(val entorno: String, val intentos: Int)

private fun demo4() {
    // OBJETIVO: distinguir construir el delegado de ejecutar su lambda.
    // lazy recibe la lambda y devuelve un objeto Lazy<Configuracion>.
    // En esta demo todo ocurre en un solo hilo. NONE omite sincronizacion:
    // no aporta garantias para inicializacion concurrente.
    val carga = lazy(LazyThreadSafetyMode.NONE) {
        println("[LAZY] Creando configuracion UNA vez")
        Configuracion("Laboratorio", 3) // Ultima expresion: resultado de la lambda.
    }
    val configuracion by carga

    println("Antes de leer: inicializado = ${carga.isInitialized()}")
    println("Primera lectura: ${configuracion.entorno}")
    println("Despues de leer: inicializado = ${carga.isInitialized()}")
    println("Segunda lectura: ${configuracion.intentos} intentos")

    // La segunda lectura recupera el resultado; NO ejecuta de nuevo la lambda.
    // lazy no significa 'en segundo plano' ni 'asincrono'.
    // Si no lees configuracion, su inicializador no se ejecuta.

    // EJERCICIO: comenta AMBAS lecturas de configuracion y vuelve a ejecutar.
    // El mensaje [LAZY] desaparece y ambos isInitialized() imprimen false.
}

// -----------------------------------------------------------------------------
// DEMO 5. observable: reaccionar DESPUES de cada asignacion.
// -----------------------------------------------------------------------------

private fun demo5() {
    // OBJETIVO: ver que observable notifica asignaciones, incluso al mismo valor.
    var notificaciones = 0
    var nombre: String by Delegates.observable("Sin nombre") { propiedad, antes, nuevo ->
        // La lambda recibe tres argumentos: propiedad, valor anterior y nuevo.
        // Se ejecuta DESPUES de guardar el nuevo valor. No puede vetar el cambio.
        notificaciones++
        println("[OBSERVABLE] ${propiedad.name}: '$antes' -> '$nuevo'")
    }

    // Construir el delegado con el valor inicial NO llama al observador.
    println("Inicial: $nombre; notificaciones = $notificaciones")
    nombre = "Everardo"  // Primera notificacion.
    nombre = "Everardo"  // Tambien notifica, aunque el contenido sea igual.
    nombre = "Luis" // Tercera notificacion.
    println("Final: $nombre; notificaciones = $notificaciones")

    // EJERCICIO: dentro de la lambda, imprime un mensaje adicional solo si
    // antes != nuevo. El observador seguira ejecutandose tres veces, pero
    // ese mensaje adicional aparecera solo dos veces.
}

// -----------------------------------------------------------------------------
// DEMO 6. vetoable: decidir ANTES si se guarda el nuevo valor.
// -----------------------------------------------------------------------------

private fun demo6() {
    // OBJETIVO: comprobar que rechazar una asignacion conserva el valor anterior.
    var edad: Int by Delegates.vetoable(0) { propiedad, antes, nuevo ->
        val aceptar = nuevo >= 0
        println("[VETOABLE] ${propiedad.name}: $antes -> $nuevo; aceptar = $aceptar")
        // La ultima expresion es el Boolean devuelto por esta lambda.
        // true permite guardar; false impide guardar. No lanza una excepcion.
        aceptar
    }

    println("Inicial: $edad")
    edad = 25
    println("Tras asignar 25: $edad")
    edad = -3
    println("Tras intentar -3: $edad") // Conserva 25.

    // El valor inicial 0 se entrega al construir el delegado: la lambda no lo
    // valida. Es responsabilidad de quien lo crea escoger un inicial adecuado.
    // EJERCICIO: acepta solo nuevo in 0..120 e intenta asignar 130.
    // La edad debe continuar en 25.
}

// -----------------------------------------------------------------------------
// DEMO 7. Propiedades delegadas a un mapa.
// -----------------------------------------------------------------------------

private class DatosDeCurso(private val datos: Map<String, Any?>) {
    // El delegado es el mapa: busca la clave con el nombre de la propiedad.
    // No se guardan aqui campos independientes llamados nombre y horas.
    // Las claves y los valores del mapa deben corresponder a estos tipos.
    val nombre: String by datos
    val horas: Int by datos
}

private fun demo7() {
    // OBJETIVO: ver otro modo de obtener valores usando la misma sintaxis by.
    val datos: Map<String, Any?> = mapOf("nombre" to "Kotlin moderno", "horas" to 2)
    val curso = DatosDeCurso(datos)
    println("Curso: ${curso.nombre}")
    println("Duracion: ${curso.horas} horas")

    // Si una clave requerida falta, el error aparece al LEER la propiedad.
    val incompleto = DatosDeCurso(mapOf("nombre" to "Otro curso"))
    try {
        println(incompleto.horas)
    } catch (error: NoSuchElementException) {
        println("Falta la clave horas: NoSuchElementException")
    }

    // EJERCICIO: agrega la propiedad val modalidad: String by datos y la clave
    // "modalidad" con el texto "Presencial". Luego imprime curso.modalidad.
}

// -----------------------------------------------------------------------------
// DEMO 8. Contratos: mismo resultado Boolean, diferente informacion de tipos.
// -----------------------------------------------------------------------------

private fun String?.esValidoSinContrato(): Boolean =
    this != null && this.isNotEmpty()

@OptIn(ExperimentalContracts::class)
private fun String?.esValidoConContrato(): Boolean {
    // contract debe ser la primera instruccion del cuerpo de esta funcion.
    contract {
        // Lectura: SI la funcion devuelve true, su receptor NO es null.
        // returns(true) DESCRIBE un resultado; no devuelve true.
        // this@esValidoConContrato es el String? sobre el que se hizo la llamada.
        // La etiqueta permite referirnos a ese receptor desde la lambda contract.
        returns(true) implies (this@esValidoConContrato != null)
    }
    // Esta es la comprobacion REAL durante la ejecucion.
    // && evita evaluar isNotEmpty() si el receptor es null.
    return this != null && this.isNotEmpty()
}

private fun procesarTexto(texto: String?) {
    // Usamos un PARAMETRO, en vez de val texto: String? = "Kotlin".
    // Asi el compilador no conoce de antemano si el valor es nulo.
    println("Entrada: ${if (texto == null) "null" else "'$texto'"}")
    println("Sin contrato: ${texto.esValidoSinContrato()}")
    println("Con contrato: ${texto.esValidoConContrato()}")

    if (texto.esValidoSinContrato()) {
        // Sin contrato el compilador no obtiene de esta llamada la garantia
        // de no nulidad, aunque el cuerpo de la funcion haga esa comprobacion.
        println("Sin contrato, acceso seguro: ${texto?.length}")

        // ERROR_INTENCIONAL_SIN_CONTRATO: al descomentar, debe fallar al compilar.
        // println(texto.length)
    }

    if (texto.esValidoConContrato()) {
        // Aqui SI hay smart cast a String. No usamos ?. ni !!.
        println("Con contrato, acceso directo: ${texto.length}")
    } else {
        // false NO implica null: el texto tambien puede ser "".
        println("No valido; es null = ${texto == null}")
    }
    // Fuera del if no podemos usar texto.length sin comprobar su nulidad.
    // La variable no cambio de objeto ni de tipo declarado: cambio lo que
    // el compilador puede garantizar en una rama del flujo.
}

private fun demo8() {
    // OBJETIVO: separar comportamiento en ejecucion de informacion al compilar.
    procesarTexto("Kotlin")
    procesarTexto("")
    procesarTexto(null)
    procesarTexto("   ") // isNotEmpty() es true para tres espacios.

    // @OptIn acepta usar la API experimental al DECLARAR el contrato.
    // ::class pasa una referencia a ExperimentalContracts.
    // No es necesario poner @OptIn en cada funcion que llama a esta funcion.
    // El contrato no sustituye la validacion ni agrega comprobaciones runtime;
    // la implementacion debe cumplir la garantia descrita.

    // EJERCICIO: cambia isNotEmpty() por isNotBlank() en AMBAS funciones.
    // La entrada de tres espacios pasara de true a false. El contrato se
    // conserva: true sigue garantizando receptor no nulo.
}

// -----------------------------------------------------------------------------
// DEMO 9. Integracion: perfil + value classes + delegado + contrato de sesion.
// -----------------------------------------------------------------------------

private class TextoInvertidoDelegate(inicial: String) : ReadWriteProperty<Any?, String> {
    // Transformacion didactica reversible: NO es un cifrado seguro.
    private var invertido = inicial.reversed()

    init {
        println("[INICIALIZAR] Apodo almacenado invertido: '$invertido'")
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): String {
        val original = invertido.reversed()
        println("[GET ${property.name}] '$invertido' -> '$original'")
        return original
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
        invertido = value.reversed()
        println("[SET ${property.name}] '$value' -> '$invertido'")
    }
}

private class PerfilUsuario(val email: CorreoElectronico, apodo: String) {
    // email es una propiedad por llevar val en el constructor.
    // apodo es solo un parametro: sirve para construir el delegado.
    var apodoSeguro: String by TextoInvertidoDelegate(apodo)
}

private class SesionUsuario(val perfil: PerfilUsuario, val token: TokenAcceso)

@OptIn(ExperimentalContracts::class)
private fun verificarSesionActiva(sesion: SesionUsuario?): Boolean {
    contract {
        // Solo comunica no nulidad cuando el resultado es true.
        // No convierte el token en otro tipo ni valida su autenticidad.
        returns(true) implies (sesion != null)
    }
    // sesion.token: objeto TokenAcceso; su .token: String interno.
    // Esto comprueba presencia de sesion y de texto en el token.
    // El nombre se mantiene para enlazar el ejercicio con el caso presentado.
    return sesion != null && sesion.token.token.isNotBlank()
}

private fun mostrarSesion(etiqueta: String, sesion: SesionUsuario?) {
    // El parametro evita que el compilador se apoye en un constructor local
    // que ya sabe que devuelve un objeto no nulo.
    println("Caso: $etiqueta")
    if (verificarSesionActiva(sesion)) {
        // El contrato habilita este acceso directo, sin ?. ni !!.
        println("Aceptada; correo = ${sesion.perfil.email.valor}")
    } else {
        // Puede faltar la sesion O puede faltar el texto del token.
        println("Rechazada; sesion nula = ${sesion == null}")
    }
}

private fun demo9() {
    // OBJETIVO: reconocer que pieza resuelve cada necesidad del caso completo.
    val usuario = PerfilUsuario(CorreoElectronico("dev@kotlin.org"), "KotlinPro")
    usuario.apodoSeguro = "ArchExpert" // Activa setValue.
    println("Apodo recuperado = ${usuario.apodoSeguro}") // Activa getValue.

    mostrarSesion("con token", SesionUsuario(usuario, TokenAcceso("jwt_xyz")))
    mostrarSesion("sin objeto", null)
    mostrarSesion("token vacio", SesionUsuario(usuario, TokenAcceso("")))
    mostrarSesion("token en blanco", SesionUsuario(usuario, TokenAcceso("   ")))

    // EJERCICIO: agrega otra sesion con TokenAcceso("abc"). Debe aceptarse.
    // Explica por que ambas sesiones rechazadas con token siguen siendo
    // objetos existentes: false no permite deducir sesion == null.
}

// -----------------------------------------------------------------------------
// Lanzador: permite estudiar una sola demo cada vez. No hay menu interactivo
// ni lectura de teclado: la seleccion se pasa como argumento al ejecutar.
// -----------------------------------------------------------------------------

private fun ejecutarDemo(numero: Int) {
    println()
    println("DEMO $numero")
    when (numero) {
        1 -> demo1()
        2 -> demo2()
        3 -> demo3()
        4 -> demo4()
        5 -> demo5()
        6 -> demo6()
        7 -> demo7()
        8 -> demo8()
        9 -> demo9()
    }
}

fun main(args: Array<String>) {
    if (args.size == 1 && args[0].equals("todas", ignoreCase = true)) {
        for (numero in 1..9) ejecutarDemo(numero)
        return
    }

    val numero = args.singleOrNull()?.toIntOrNull()
    if (numero != null && numero in 1..9) {
        ejecutarDemo(numero)
        return
    }

    println("Repaso avanzado de Kotlin moderno - Tema 1.1")
    println("1. K2, inferencia y smart cast")
    println("2. Value classes y validacion")
    println("3. Delegado propio: getValue y setValue")
    println("4. lazy")
    println("5. observable")
    println("6. vetoable")
    println("7. Propiedades delegadas a un mapa")
    println("8. Contratos y smart cast")
    println("9. Caso integrado: sesion de usuario")
    println("Uso: java -jar repaso.jar <1..9|todas>")
}
