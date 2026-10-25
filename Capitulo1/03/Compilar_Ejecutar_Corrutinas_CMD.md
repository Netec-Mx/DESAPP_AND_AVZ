# Compilar y ejecutar corrutinas en CMD

Para ejecutar `Demo_1_3_StateFlow.kt`, abre **CMD en la carpeta donde guardaste ese archivo**.

<br/><br/>

## 1. Definir la biblioteca

Ajusta la ruta si instalaste Kotlin en otra carpeta:

```bat
set "COR13=C:\work\AndroidBasico\software\kotlin-compiler-2.4.20\kotlinc\lib\kotlinx-coroutines-core-jvm.jar"
dir "%COR13%"
```

El comando `dir` debe mostrar el archivo JAR.

<br/><br/>

## 2. Compilar

```bat
kotlinc Demo_1_3_StateFlow.kt -cp "%COR13%" -include-runtime -d demo13_stateflow.jar
```

<br/><br/>

## 3. Ejecutar

Si la compilación terminó sin errores:

```bat
java -cp "demo13_stateflow.jar;%COR13%" tema13.flujo.Demo_1_3_StateFlowKt
```

Usa **la misma ventana de CMD** para los tres pasos. Después de modificar el código, vuelve a compilar y ejecutar. `-include-runtime` incluye Kotlin; el JAR de corrutinas se incorpora mediante `-cp`.
