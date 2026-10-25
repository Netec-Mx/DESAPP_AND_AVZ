# 1.4 — Patrones avanzados de programación funcional

<br/><br/>

## Propósito de esta demo

Entender cómo preparar funciones reutilizables, combinar transformaciones y representar errores como valores. Empezamos con operaciones pequeñas y terminamos validando un registro de usuario en consola.

El código está completo y comentado. Este material incluye explicaciones, salidas y glosario. Conservamos las hojas de la PPT y trabajamos con Kotlin/JVM, sin Android.

<br/><br/>

## Ejecutar

Descarga `Demo_1_4.kt`. Abre CMD o PowerShell en la carpeta donde lo guardaste y ejecuta:

```bat
kotlinc Demo_1_4.kt -include-runtime -d demo14.jar
java -jar demo14.jar
```

Para ver solo uno de los seis bloques:

```bat
java -jar demo14.jar 4
```

<br/><br/>

**Solo se necesita Kotlin y Java.** Nuestro `Either`, `map`, `flatMap` y `compose` están definidos en el archivo. No necesitas configurar `COR13`, instalar Arrow ni agregar `-cp`.

Si prefieres copiar el código desde este documento, utiliza únicamente el **Código completo** del final. Los fragmentos de las explicaciones muestran partes del programa. Después de editar un archivo, recompílalo antes de ejecutar su JAR.

<br/><br/>

## Recorrido de los temas

| Bloque | Idea | Lo que muestra |
| --- | --- | --- |
| 1 | Función pura y datos inmutables | Normalizar un texto sin modificar el original |
| 2 | Currificación y aplicación parcial | Fijar un argumento y reutilizar la función resultante |
| 3 | Composición | Combinar funciones y reconocer el orden de aplicación |
| 4 | `Either`, `map` y `flatMap` | Transformar un éxito, encadenar una validación y conservar errores |
| 5 | Validadores puros y errores tipados | Validar correo y longitud de contraseña devolviendo datos |
| 6 | Pipeline de registro | Reunir las piezas y presentar el resultado con un `when` |

<br/><br/>

## 1. Función pura: calcular sin cambiar el exterior

Una función pura produce el mismo resultado para los mismos argumentos y no provoca efectos secundarios observables.

```kotlin
private fun normalizarEmail(email: String): String = email.trim().lowercase()
```

Esta función recibe un texto, elimina los espacios de los extremos y produce otro texto en minúsculas. No imprime, modifica variables externas ni cambia el `String` recibido.

La demo la llama dos veces con la misma entrada. Los corchetes de la salida permiten ver que el original conserva sus espacios.

Salida observada:

```text
=== DEMO 1: Funcion pura y datos inmutables ===
Original: [  ANA@EJEMPLO.COM  ]
Primera llamada: [ana@ejemplo.com]
Segunda llamada: [ana@ejemplo.com]
Misma entrada, mismo resultado: true
Original despues: [  ANA@EJEMPLO.COM  ]
```

**Idea clave:** la transformación calcula; la presentación imprime. Los `println` de `demo1` son efectos secundarios, pero están separados de la función que transforma el dato.

**Transparencia referencial** significa que, dentro de una expresión, podríamos sustituir esa llamada por su resultado sin cambiar el comportamiento observable. En este caso, sustituir la llamada correspondiente por `"ana@ejemplo.com"` conserva el resultado del cálculo.

<br/><br/>

## 2. Currificación y aplicación parcial: dos ideas relacionadas

Primero observamos una suma conocida:

```kotlin
sumar(5, 3)
```

La función normal recibe dos argumentos juntos. La forma currificada recibe uno y devuelve una función que recibirá el siguiente:

```kotlin
sumarCurrificada(5)(3)
```

Se lee en dos pasos:

1. `sumarCurrificada(5)` devuelve una función que conserva el valor `5`.
2. La segunda llamada, con `(3)`, suma ese `3` al `5` conservado.

Para hacer visibles los pasos, podemos guardar la función intermedia:

```kotlin
val sumarCinco: (Int) -> Int = sumarCurrificada(5)
println(sumarCinco(3))
```

| Concepto | Significado | En la demo |
| --- | --- | --- |
| Currificación | Expresar una función de varios argumentos como funciones sucesivas de un argumento | `sumarCurrificada(5)(3)` |
| Aplicación parcial | Fijar algunos argumentos y obtener una función para los restantes | Guardar `sumarCurrificada(5)` en `sumarCinco` |

La misma idea se aplica a una regla de longitud:

```kotlin
val validarMinimo8 = validarLongitud(8)
```

Aquí ya fijamos el mínimo. Todavía no validamos ningún texto. La función resultante puede usarse después con muchos textos diferentes. La captura o **closure** es lo que permite conservar ese valor `8`.

Salida observada:

```text
=== DEMO 2: Currificacion y aplicacion parcial ===
Funcion normal: 8
Funcion currificada: 8
sumarCinco(3): 8
sumarCinco(10): 15
Longitud de abc suficiente: false
Longitud de Password123 suficiente: true
```

**Idea clave:** configurar una función y ejecutarla con un dato son momentos diferentes. Es una aplicación de las funciones como valores estudiadas en 1.2.

<br/><br/>

## 3. Composición: conectar la salida de una función con la entrada de otra

Nuestra función `compose` permite escribir:

```kotlin
val primeroSumar = duplicar compose sumarUno
```

Al recibir `4`, la función compuesta hace lo siguiente:

```text
sumarUno(4) = 5
duplicar(5) = 10
```

La función situada a la derecha de `compose` se aplica primero. Si invertimos el orden, obtenemos `duplicar(4) = 8` y luego `sumarUno(8) = 9`.

También componemos dos transformaciones de texto:

```kotlin
val prepararEmail = aMinusculas compose quitarEspacios
```

`prepararEmail` recibe un texto, lo entrega a `quitarEspacios` y pasa ese resultado a `aMinusculas`. Las funciones pueden tener tipos distintos siempre que la salida intermedia sea compatible con la entrada siguiente.

Salida observada:

```text
=== DEMO 3: Composicion: el orden importa ===
Sumar uno y duplicar, con 4: 10
Duplicar y sumar uno, con 4: 9
Texto preparado: ana@ejemplo.com
```

**Idea clave:** componer crea una nueva función reutilizable. En la definición genérica, `A` es el tipo de entrada, `B` el intermedio y `C` el de salida. `infix` permite la escritura `f compose g`, equivalente a `f.compose(g)`.

<br/><br/>

## 4. Either: devolver un error también es devolver un valor

`Either<L, R>` representa una de dos alternativas. En nuestra implementación:

- `Left<L>` contiene el error.
- `Right<R>` contiene el éxito.

Por ejemplo, `Either<String, Int>` puede contener un mensaje de error o un número correcto. El tipo está definido mediante una jerarquía sellada, retomando lo estudiado en 1.3.

### map transforma el contenido de un éxito

```kotlin
exito.map { it * 2 }
```

Si `exito` contiene `Right(5)`, el resultado es `Right(10)`. Si contiene un `Left`, la función de transformación no se ejecuta y se conserva ese error.

Este `map` es una extensión de nuestro `Either`; no es `List.map`. El receptor indica sobre qué estructura estamos trabajando.

### flatMap encadena una función que ya devuelve Either

`validarPositivo` devuelve un `Either`: éxito cuando el número es positivo y error en caso contrario.

| Operación | Qué devuelve la función recibida | Resultado de la operación |
| --- | --- | --- |
| `map` | Un valor `T` | Lo envuelve en `Right<T>` |
| `flatMap` | Un `Either<L, T>` | Entrega ese resultado directamente |

Si usamos `map` con una función que ya devuelve `Either`, aparecen dos niveles: `Right(Right(5))`. `flatMap` evita esa envoltura adicional: conserva un solo resultado de éxito o error.

Salida observada:

```text
=== DEMO 4: Either, map y flatMap con numeros ===
map sobre Right: Right(value=10)
map sobre Left: Left(value=Dato no disponible)
map con validarPositivo: Right(value=Right(value=5))
flatMap con validarPositivo: Right(value=5)
flatMap puede producir un error: Left(value=Debe ser positivo)
flatMap conserva el error previo: Left(value=Dato no disponible)
```

**Idea clave:** `map` sirve para transformar un dato correcto; `flatMap` sirve para continuar con un paso que también puede devolver un error. Ambos conservan un error previo sin ejecutar su transformación.

Los nombres `Left` y `Right` son una convención de este modelo. Nuestro código sigue la misma orientación hacia `Right` que utiliza `Either` en Arrow, pero no necesita esa biblioteca.

<br/><br/>

## 5. Validaciones puras: expresar exactamente qué falló

Ahora el error deja de ser un texto libre y se representa mediante `ValidationError`:

| Variante | Significado |
| --- | --- |
| `EmailVacio` | No se recibió un correo con contenido |
| `EmailInvalido` | El texto no cumple la regla de formato de esta demo |
| `PasswordMuyCorta(minimo)` | La longitud es menor que el mínimo configurado |

`validarEmail` devuelve `Either<ValidationError, String>`. Si el correo cumple la regla, devuelve el texto en `Right`. Si no, devuelve la variante de error correspondiente en `Left`.

Para la contraseña se prepara una función reutilizable:

```kotlin
private val validarPassword8 = crearValidadorPassword(8)
```

Es otra aplicación parcial: se fija el mínimo y se obtiene una función que recibirá contraseñas. Aquí solo se comprueba longitud. El patrón de correo es igualmente una regla didáctica sencilla.

Salida observada:

```text
=== DEMO 5: Validaciones puras y errores tipados ===
Correo valido: OK
Correo vacio: ERROR: El correo esta vacio
Correo sin arroba: ERROR: El formato del correo es invalido
Clave corta: ERROR: La clave requiere 8 caracteres
Clave suficiente: OK
```

**Idea clave:** los validadores producen resultados; `describirError` convierte cada error en un mensaje. Un `when` exhaustivo permite manejar las alternativas conocidas del modelo de error.

<br/><br/>

## 6. Pipeline: reunir las piezas en un registro

`RegistroUseCase` representa una operación de registro simulada. Su resultado es `Either<ValidationError, UsuarioProcesado>`.

El recorrido completo es:

1. Normalizar el correo.
2. Validar ese correo.
3. Si el correo es correcto, validar la longitud de la contraseña mediante `flatMap`.
4. Si la contraseña es correcta, crear `UsuarioProcesado` mediante `map`.
5. Presentar el éxito o el error con `when` en la consola.

El núcleo de ese recorrido es:

```kotlin
return validarEmail(normalizarEmail(email))
    .flatMap { emailValido ->
        validarPassword8(password)
            .map { UsuarioProcesado(emailValido) }
    }
```

`validarPassword8` ya produce un `Either`. Por eso el primer enlace usa `flatMap`. Crear `UsuarioProcesado` produce un objeto común; por eso el segundo enlace usa `map`.

La lambda final conserva acceso a `emailValido`, obtenido en la validación anterior. La contraseña debe aprobar su validación, mientras que el objeto final solo conserva el correo normalizado.

Salida observada:

```text
=== DEMO 6: Registro: todas las piezas juntas ===
Caso 1, datos correctos: EXITO: ana@ejemplo.com
Caso 2, correo vacio: ERROR: El correo esta vacio
Caso 3, correo invalido: ERROR: El formato del correo es invalido
Caso 4, clave corta: ERROR: La clave requiere 8 caracteres
Caso 5, ambos invalidos: ERROR: El correo esta vacio
```

**El caso 5 es especialmente útil para explicar el flujo:** ambos datos son incorrectos, pero se obtiene solo `EmailVacio`. El primer `Left` corta el encadenamiento y evita ejecutar los pasos posteriores. Esta implementación no acumula todos los errores de una entrada.

El programa devuelve un resultado en memoria y lo muestra en consola. `RegistroUseCase` es una clase de Kotlin: no necesita un ViewModel ni un entorno Android para demostrar el patrón.

<br/><br/>

## La palabra «monádico» en este ejemplo

Para seguir esta demo basta con reconocer dos operaciones: `Right` introduce un valor correcto en la forma de resultado y `flatMap` permite encadenar otro cálculo que devuelve esa misma forma de resultado.

El cálculo puede continuar por éxito o conservar el error. Esa forma de encadenar es la idea práctica que aquí conecta `Either` con el término **mónada**. Su estudio formal incluye leyes de identidad y asociatividad; no necesitamos desarrollarlas para leer este programa.

<br/><br/>

## Precisiones para acompañar las hojas

- La pureza depende de lo que hace cada función. Devolver `Either` no impide por sí solo imprimir, modificar datos externos o lanzar una excepción dentro de una transformación. Nuestros validadores comunican sus errores esperados mediante `Left`.
- `Either` y `Result` no son el mismo tipo. `Result<T>` pertenece a Kotlin y su fallo contiene un `Throwable`. Nuestro `Either<L, R>` permite elegir un tipo de error como `ValidationError`.
- El `when` exhaustivo de esta demo se apoya en nuestro modelo sellado. No atribuimos esa comprobación de ramas `Left`/`Right` a `Result`.
- Elegimos estos patrones por claridad y reutilización. La demo no mide ni compara rendimiento.

<br/><br/>

## Glosario del submódulo 1.4

| Término | Significado sencillo y relación con la demo |
| --- | --- |
| Función pura | Depende de sus argumentos y no provoca efectos secundarios observables. `normalizarEmail` es un ejemplo. |
| Efecto secundario | Acción observable además de devolver el resultado: imprimir, escribir un archivo o modificar un dato externo. |
| Transparencia referencial | Posibilidad de sustituir una expresión por su resultado sin cambiar el comportamiento observable del programa. |
| Inmutabilidad | Un valor u objeto no cambia después de crearse. En la demo los textos originales no se modifican. Una propiedad `val` por sí sola no hace inmutable todo un objeto. |
| Función de orden superior | Recibe o devuelve funciones. Ejemplos: `validarLongitud`, `map` y `compose`. |
| Lambda | Expresión que crea una función, como `{ it * 2 }`. |
| Captura o closure | Acceso que conserva una función a valores de su entorno, como el mínimo `8` del validador. |
| Currificación | Transformar una función de varios argumentos en funciones sucesivas de un argumento: de `f(a, b)` a `f(a)(b)`. |
| Aplicación parcial | Fijar algunos argumentos y obtener una función para los restantes. `sumarCinco` ya conserva el `5`. |
| Composición | Combinar funciones pasando el resultado de una a la siguiente. `f compose g` aplica primero `g`. |
| Pipeline | Secuencia de transformaciones o validaciones que colaboran para producir un resultado final. |
| Función infix | Función que admite escribirse entre receptor y argumento, como `duplicar compose sumarUno`. |
| Either | Tipo que representa una de dos alternativas. En esta demo, error o éxito. Lo implementamos en el archivo. |
| Left / Right | Variantes de `Either`: por convenio, `Left` contiene el error y `Right` contiene el éxito. |
| Error tipado | Fallo representado por una variante conocida del modelo, como `EmailVacio`, en lugar de solo un texto libre. |
| map | En nuestro `Either`, transforma el valor de éxito y lo vuelve a envolver; conserva un error previo. |
| flatMap | Encadena una función que ya devuelve un `Either`, sin agregar una envoltura adicional. |
| Cortocircuito | Se omiten los siguientes pasos dependientes cuando ya existe un error. Esta demo devuelve el primer error. |
| Mónada | Abstracción para introducir valores y encadenar cálculos dentro de una forma de resultado, respetando ciertas leyes. Aquí se ilustra con `Right` y `flatMap`. |
| Covarianza, `out` | Permite usar un tipo genérico más específico donde se acepta uno más general. Ayuda a representar las dos variantes de `Either`. |
| Nothing | Tipo sin instancias y subtipo de los demás tipos. En `Left` y `Right` representa el lado que esa variante no contiene. |
| when exhaustivo | Expresión que contempla todas las alternativas conocidas de un tipo sellado. Se usa para presentar éxito y error. |
| Result | Tipo estándar de Kotlin para un éxito de tipo `T` o un fallo representado por `Throwable`. Es distinto del `Either` de esta práctica. |


<br/><br/>

## Referencias

- [Funciones de orden superior, tipos de función y capturas — Kotlin](https://kotlinlang.org/docs/lambdas.html).
- [Funciones de extensión — Kotlin](https://kotlinlang.org/docs/extensions.html).
- [Genéricos y covarianza — Kotlin](https://kotlinlang.org/docs/generics.html).
- [Either, map y flatMap — Arrow](https://apidocs.arrow-kt.io/arrow-core/arrow.core/-either/index.html). Referencia del patrón; esta demo define su propio tipo.
- [Result — Kotlin](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin/-result/).


