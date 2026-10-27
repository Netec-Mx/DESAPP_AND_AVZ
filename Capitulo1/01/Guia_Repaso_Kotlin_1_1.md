# Tema 1.1. Repaso avanzado de Kotlin moderno

Material complementario para explicar y practicar los conceptos del capítulo 1, subtema 1.1 `Repaso avanzado de Kotlin moderno. La presentación se conserva tal como está. Las demostraciones se ejecutan en consola con Kotlin/JVM.

<br/><br/>

## 1. Lista de temas

La primera hoja presenta objetivos de todo el capítulo. Para el tema 1.1, las hojas siguientes desarrollan estos contenidos:

| Orden | Tema | Qué debe comprender el participante | Demo |
|---|---|---|---|
| 1 | K2 y análisis del código | Papel del compilador; inferencia de tipos y smart casts | 1 |
| 2 | Value classes y `@JvmInline` | Crear tipos específicos alrededor de un valor; distinguir correo de token | 2 |
| 3 | Validación mediante `init` y `require` | Separar un error de tipos al compilar de un contenido rechazado al ejecutar | 2 |
| 4 | Delegación de propiedades con `by` | Encargar la lectura y escritura a otro objeto | 3 |
| 5 | Delegados personalizados | Papel de `ReadWriteProperty`, `getValue`, `setValue`, propietario y metadatos | 3 |
| 6 | Inicialización diferida con `lazy` | Calcular en la primera lectura y conservar el resultado; significado de `NONE` | 4 |
| 7 | Observación con `Delegates.observable` | Ejecutar una acción después de cada asignación | 5 |
| 8 | Validación con `Delegates.vetoable` | Aceptar o rechazar el nuevo valor antes de guardarlo | 6 |
| 9 | Propiedades delegadas a un mapa | Obtener valores usando claves con el nombre de las propiedades | 7 |
| 10 | Contratos y smart casts | Comunicar una garantía al compilador mediante `returns(true) implies ...` | 8 |
| 11 | API experimental y sintaxis de contratos | `@OptIn`, `ExperimentalContracts::class`, receptor de extensión y etiqueta `this@...` | 8 |
| 12 | Integración de conceptos | Construir perfil y sesión; delegar el apodo; comprobar sesión y acceder al correo | 9 |

Como apoyo se repasan `val`, `var`, tipos anulables (`?`), cortocircuito de `&&`, lambdas, última expresión de una lambda, funciones de extensión y composición de objetos. Se explican conforme aparecen en el código.

Los objetivos generales de la hoja inicial también anuncian funciones de alcance, clases sealed, enumeraciones, colecciones, corrutinas y Flows. Sus demostraciones pueden desarrollarse al llegar a los apartados correspondientes; aquí el trabajo práctico se concentra en los contenidos desarrollados en las hojas de 1.1.

<br/><br/>

## 2. Archivos y ejecución en Windows

- `RepasoKotlinModerno.kt`: código completo de nueve demos, comentarios y ejercicios.
- `Guia_Repaso_Kotlin_1_1.md`: esta guía de explicación y práctica.
- `Salida_Esperada_Kotlin_1_1.txt`: salida completa de las demos originales, para comparar resultados.

Requisitos: JDK 17 o superior y compilador Kotlin/JVM 2.0 o superior disponibles en `PATH`. El código fue compilado y ejecutado con Kotlin/JVM 2.2.10 y JDK 17. Solo utiliza la biblioteca estándar; no requiere descargar dependencias para ejecutar las demos una vez instalado el compilador.

Guarda los archivos en una carpeta de trabajo, por ejemplo `C:\work\KotlinModerno11`. Abre CMD o PowerShell en esa carpeta. Los siguientes comandos sirven en ambas terminales:

```text
java -version
kotlinc -version
kotlinc RepasoKotlinModerno.kt -include-runtime -d repaso.jar
```

`kotlinc` compila el archivo. `-include-runtime` incluye la biblioteca de ejecución de Kotlin dentro del JAR. `-d` establece el nombre del archivo generado. Java sigue siendo necesario para ejecutarlo.

Si `kotlinc` no se reconoce, agrega a `PATH` el directorio `bin` de tu instalación del compilador, o llama a `kotlinc.bat` mediante su ruta completa. Abre una nueva terminal después de modificar `PATH`.

Mostrar el índice:

```text
java -jar repaso.jar
```

Ejecutar una demo concreta:

```text
java -jar repaso.jar 3
```

El número `3` llega a `main(args)` como argumento. Cámbialo por cualquier número del 1 al 9. Las demos se pueden ejecutar independientemente y siempre empiezan con sus propios valores iniciales.

Ejecutar todas:

```text
java -jar repaso.jar todas
```

Después de modificar el código, vuelve a compilar antes de ejecutar. Si la compilación falla, no ejecutes el JAR anterior: todavía contiene la versión previa.


<br/><br/>

## 3. Forma de trabajar cada demo

1. Buscar en el archivo el comentario `DEMO` con su número.
2. Leer primero el objetivo y el cuerpo de `demoN()`; después revisar las clases y funciones que utiliza.
3. Predecir el orden de los mensajes de consola.
4. Ejecutar solamente esa demo.
5. Relacionar cada mensaje con la instrucción que lo produjo.
6. Realizar el ejercicio comentado al final y recompilar.

Las líneas `ERROR_INTENCIONAL` quedan comentadas para que el material inicial compile. Para experimentar, activa una sola línea, compila, observa el diagnóstico y vuelve a comentarla.

<br/><br/>

## 4. Explicación de las demos

### Demo 1. K2, inferencia y smart cast

**Objetivo:** distinguir deducir un tipo de comprobarlo.

La declaración `val cantidad = 3` permite deducir `Int`. Esa variable no puede convertirse después en una cadena. La inferencia no equivale a tipado dinámico y no apareció por primera vez con K2.

En `analizarDato(dato: Any?)`, el parámetro admite diferentes tipos y también `null`. La expresión `dato is String` comprueba si el valor es una cadena. K2 conserva esa información al guardarla en el Boolean local `esTexto` y usarlo en el `if`. Dentro de la rama verdadera permite `dato.length`.

**Explicación:** «El compilador sigue las comprobaciones que hacemos. Cuando tiene información suficiente, nos permite usar el valor con un tipo más específico».

**Observar:** `Kotlin` tiene longitud 6; `42` y `null` van a la otra rama.

**Pregunta:** ¿por qué `dato.length` se permite dentro del `if`, pero no al inicio de `analizarDato`?

**Práctica:** llamar a `analizarDato("K2")`; debe indicar longitud 2.

La demo muestra análisis de tipos. Un programa pequeño no demuestra por sí solo cuánto acelera K2 la compilación de un proyecto.

<br/><br/>

### Demo 2. Value classes y validación

**Objetivo:** diferenciar significado del dato y validez de su contenido.

`CorreoElectronico` y `TokenAcceso` contienen cadenas, pero son tipos diferentes. `enviarAviso` recibe un correo; el compilador rechaza un `String` directo o un token.

El bloque `init` agrega una comprobación sobre el contenido. `CorreoElectronico("sin-arroba")` tiene la forma correcta para compilar, pero `require` rechaza su contenido al ejecutar. El `try/catch` captura esa excepción para que la demo pueda continuar.

**Explicación:** «El tipo responde qué representa el dato. La validación responde si su contenido cumple una regla».

**Observar:** aparece el aviso al correo válido y después el mensaje de validación del inválido.

**Práctica:** activar `enviarAviso(token)`. Debe fallar al compilar, antes de iniciar el programa.

**Nota para explicar la representación:** el compilador puede usar el valor interno sin un envoltorio adicional. Al utilizar la value class como `Any` se necesita boxing. No se deduce una ubicación fija en pila o heap. Revisar `@` es una regla mínima para esta demo, no una validación completa de correos.

<br/><br/>

### Demo 3. Delegado propio

**Objetivo:** ver qué sucede detrás de una propiedad declarada con `by`.

`Ficha` declara dos propiedades. Cada una tiene su propia instancia de `RegistroDelegate`; la clase del delegado se reutiliza, pero sus valores almacenados son independientes.

El delegado implementa `ReadWriteProperty<Any?, String>`: admite cualquier propietario y administra valores `String`. `getValue` responde a una lectura; `setValue` responde a una asignación.

| Elemento | Papel en esta demo |
|---|---|
| `thisRef` | Objeto `Ficha` al que pertenece la propiedad |
| `property` | Metadatos; `property.name` identifica `alias` o `ciudad` |
| `value` | Nuevo texto que se quiere guardar |
| `almacenado` | Estado interno de cada instancia del delegado |
| `by` | Conexión entre los accesos de la propiedad y el delegado |

**Explicación:** «La propiedad ofrece un dato; el delegado se ocupa de cómo leerlo y cómo actualizarlo».

**Observar:** construir la ficha imprime dos mensajes de creación. Leer `alias` produce GET; asignarle `KotlinPro` produce SET; volver a leer produce otro GET. `ciudad` conserva `CDMX`.

`ReadWriteProperty` ya declara los métodos como operadores. Por eso la implementación puede escribir `override fun` sin repetir `operator`.

**Práctica:** asignar `Puebla` a `ciudad`, leerla y verificar que `alias` sigue siendo `KotlinPro`.

<br/><br/>

### Demo 4. lazy

**Objetivo:** separar crear el delegado de calcular su valor.

`lazy` recibe una lambda que construirá la configuración. Al crear el delegado todavía no la ejecuta. La primera lectura de `configuracion` calcula y conserva el resultado; las siguientes lecturas recuperan ese resultado.

`carga.isInitialized()` permite observar el momento del cambio: primero `false`, después `true`.

**Explicación:** «Preparo cómo obtener el dato, pero lo calculo hasta que alguien lo necesita».

**Observar:** `[LAZY]` aparece una sola vez, aunque se hacen dos lecturas.

`LazyThreadSafetyMode.NONE` omite sincronización. La demo trabaja en un solo hilo. No significa ejecución en segundo plano.

**Práctica:** comentar ambas lecturas de `configuracion`. El mensaje `[LAZY]` debe desaparecer y los dos indicadores de inicialización deben ser `false`.

<br/><br/>

### Demo 5. observable

**Objetivo:** detectar asignaciones después de que se guardan.

La lambda de `observable` recibe la propiedad, el valor anterior y el nuevo. Su función es reaccionar; no decide si se acepta el valor.

La demo asigna `Everardo`, nuevamente `Everardo` y después `Luis`. El observador se ejecuta tres veces. Entregar el valor inicial al construir el delegado no cuenta como una notificación.

**Explicación:** «Primero se guarda el nuevo valor; después recibo el aviso para reaccionar».

**Práctica:** imprimir un mensaje adicional dentro de `if (antes != nuevo)`. El observador seguirá ejecutándose tres veces; el mensaje adicional aparecerá dos veces.

<br/><br/>

### Demo 6. vetoable

**Objetivo:** aceptar o rechazar antes de guardar.

La lambda devuelve un Boolean. Si devuelve `true`, se guarda el candidato; si devuelve `false`, se conserva el valor anterior. Aquí se acepta 25 y se rechaza -3, por lo que la edad final sigue siendo 25.

**Explicación:** «Antes de cambiar el dato, el delegado pregunta si el nuevo valor se acepta».

La última expresión `aceptar` es el resultado de la lambda. El valor inicial se entrega al construir el delegado; no pasa por esta validación.

**Práctica:** usar `nuevo in 0..120` e intentar asignar 130. Debe conservar 25.

| Comparación | observable | vetoable |
|---|---|---|
| Momento de la lambda | Después de guardar | Antes de guardar |
| Resultado necesario | No decide la asignación | Boolean: aceptar o rechazar |
| Uso mostrado | Registrar asignaciones | Impedir edades negativas |

<br/><br/>

### Demo 7. Delegación a un mapa

**Objetivo:** reutilizar `by` para obtener datos de una estructura existente.

Las propiedades de `DatosDeCurso` buscan las claves `nombre` y `horas` en el mapa. La clave debe coincidir con el nombre de la propiedad, y el valor debe tener un tipo compatible.

**Explicación:** «Esta propiedad obtiene su valor consultando una clave del mapa».

**Observar:** se imprime el curso y su duración. Al consultar `horas` en un mapa incompleto se captura `NoSuchElementException`.

**Práctica:** agregar `modalidad` a la clase y al mapa. Leerla debe mostrar `Presencial`.

<br/><br/>

### Demo 8. Contratos y smart casts

**Objetivo:** comparar dos funciones que devuelven lo mismo, pero comunican distinta información al compilador.

`esValidoSinContrato` y `esValidoConContrato` comprueban que el texto exista y no esté vacío. Con las mismas entradas devuelven los mismos Boolean.

La diferencia aparece al compilar el código que las llama. El contrato describe: «si devuelvo true, el receptor no es null». Permite que dentro de la rama verdadera se use `texto.length` directamente.

| Sintaxis | Lectura |
|---|---|
| `String?.esValidoConContrato()` | Función de extensión que puede recibir una cadena o null como receptor |
| `@OptIn(ExperimentalContracts::class)` | Acepta utilizar la API experimental al declarar el contrato |
| `ExperimentalContracts::class` | Referencia a esa clase de anotación |
| `contract { ... }` | Descripción para el análisis del compilador |
| `returns(true)` | Describe el caso de un resultado true; no lo devuelve |
| `implies` | Ese resultado garantiza la condición indicada |
| `this@esValidoConContrato` | Receptor de la función de extensión, mencionado desde la lambda |
| `return ...` | Comprobación real que calcula el resultado durante la ejecución |

**Explicación:** «La función hace la comprobación; el contrato le cuenta al compilador qué puede concluir del resultado».

`procesarTexto` recibe un parámetro cuyo contenido se desconoce al compilar. Esto permite observar el aporte del contrato sin apoyarse en una variable inicializada localmente con un valor evidentemente no nulo.

**Observar:**

- `Kotlin`: true y acceso directo a longitud 6.
- Cadena vacía: false, aunque el valor no es null.
- `null`: false.
- Tres espacios: true, porque se usa `isNotEmpty()`.

El contrato solo garantiza no nulidad en el caso true. No garantiza un tipo especial para cadenas no vacías ni permite concluir null cuando el resultado es false. Tampoco agrega una comprobación de seguridad en ejecución: su implementación debe respetar la promesa.

**Práctica A:** activar el `println(texto.length)` de la rama sin contrato. Debe fallar al compilar. Volver a comentarlo.

**Práctica B:** sustituir `isNotEmpty()` por `isNotBlank()` en ambas funciones. Tres espacios deben producir false.


<br/><br/>

### Demo 9. Caso integrado de sesión

**Objetivo:** reconocer las responsabilidades de cada pieza en un mismo ejemplo.

| Pieza | Responsabilidad |
|---|---|
| `CorreoElectronico` | Tipo específico para el correo y regla mínima en `init` |
| `TokenAcceso` | Tipo específico para el token |
| `TextoInvertidoDelegate` | Guardar texto invertido y recuperarlo en su forma original |
| `PerfilUsuario` | Reunir el correo y el apodo delegado |
| `SesionUsuario` | Reunir el perfil y el token |
| `verificarSesionActiva` | Comprobar existencia de sesión y texto no blanco en el token |
| Contrato de la función | Comunicar no nulidad cuando devuelve true |
| `mostrarSesion` | Aprovechar esa garantía para leer el correo |

La transformación del apodo usa `reversed()`: sirve para visualizar la escritura y la lectura del delegado. No implementa cifrado seguro. El nombre `apodoSeguro` se conserva para facilitar la relación con el caso de las hojas.

La expresión `sesion.token.token` tiene dos pasos: primero obtiene el `TokenAcceso` de la sesión y después obtiene su cadena interna. La función verifica presencia de texto; no valida firma, expiración ni autenticidad de un JWT.

<br/><br/>

**Secuencia de lectura en clase:**

1. Crear el correo y el perfil.
2. Observar el apodo inicial almacenado al revés.
3. Asignar `ArchExpert` y localizar SET.
4. Leer el apodo y localizar GET.
5. Construir la sesión con token.
6. Entrar a `mostrarSesion`, que recibe una referencia posiblemente nula.
7. Verificarla y leer directamente el correo en la rama true.
8. Repetir con sesión nula, token vacío y token en blanco.

**Práctica:** agregar un quinto caso con el token `abc`. Debe aceptarse porque cumple la regla de esta demo.

**Pregunta de cierre:** si el resultado es false, ¿puedo afirmar que no existe un objeto sesión? No: los casos de token vacío y en blanco contienen objetos existentes.

<br/><br/>

## 5. Respuestas breves para revisar los ejercicios

1. `analizarDato("K2")` imprime longitud 2 en la rama de texto.
2. `enviarAviso(token)` se rechaza por incompatibilidad entre `TokenAcceso` y `CorreoElectronico`.
3. Después de `ficha.ciudad = "Puebla"`, leer ciudad produce GET con Puebla; alias conserva KotlinPro.
4. Sin leer `configuracion` no aparece `[LAZY]`; sus indicadores siguen en false.
5. Agregar `if (antes != nuevo) { println("Cambio de contenido") }` dentro del observador produce dos mensajes adicionales.
6. Usar `val aceptar = nuevo in 0..120`; después de asignar 25, intentar 130 conserva 25.
7. Agregar `val modalidad: String by datos` y la entrada `"modalidad" to "Presencial"`.
8. El acceso directo sin contrato falla al compilar. Con `isNotBlank()`, la entrada de espacios produce false.
9. `mostrarSesion("otro token", SesionUsuario(usuario, TokenAcceso("abc")))` muestra el correo.

<br/><br/>

## 6. Verificación del material

El código base fue compilado con Kotlin/JVM 2.2.10 y JDK 17, tratando las advertencias como errores. Las nueve demos se ejecutaron en consola. La salida completa se conserva en `Salida_Esperada_Kotlin_1_1.txt`.

También se comprobaron por separado los errores intencionales de pasar un String como correo, pasar un token como correo y acceder directamente a la longitud en la rama sin contrato. Esas variantes se rechazan al compilar y no forman parte del archivo ejecutable base.

<br/><br/>

## 7. Documentación de referencia

- [K2 y análisis del compilador](https://kotlinlang.org/docs/k2-compiler-migration-guide.html)
- [Value classes](https://kotlinlang.org/docs/inline-classes.html)
- [Delegación y delegados estándar](https://kotlinlang.org/docs/delegated-properties.html)
- [Contratos: returns](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.contracts/-contract-builder/returns.html)
- [Contratos: implies](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.contracts/-simple-effect/implies.html)
- [ExperimentalContracts](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.contracts/-experimental-contracts/)
- [Comprobación de tipos y smart casts](https://kotlinlang.org/docs/typecasts.html)
