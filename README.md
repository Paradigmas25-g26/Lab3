# Lab3
# Programación Distribuida con Spark - Laboratorio 3 Paradigmas

## Integrantes
- Maria Lilen Bena
- Camila Micaela Castillo Cortes
- Nicolas Uriel Mansutti
- Diego Alexis Ramirez

## Descripción

FeedReader es una aplicación Java que permite leer, procesar y analizar feeds RSS (y está preparada para extenderse a otros formatos como Reddit). El programa descarga los feeds definidos en un archivo de suscripciones, los procesa y muestra los artículos y entidades nombradas encontradas en los textos.

Este proyecto extiende el lector de feeds RSS del Lab 2, adaptándolo al procesamiento distribuido con Apache Spark. Permite escalar la descarga de múltiples feeds y el análisis de sus artículos mediante paralelización.

---

## Organización del Proyecto

El proyecto se distribuye en varios directorios y archivos clave:

- **README.md**: Documentación principal del proyecto.
- **src/**: Contiene todo el código fuente.
    - **feed/**: Clases relacionadas con la gestión de feeds.
    - **parser/**: Clases encargadas de parsear los feeds y extraer información.
    - **subscription/**: Clases para gestionar las suscripciones a los feeds.
    - **namedEntity/**: Clases para el análisis de entidades nombradas.
        - **heuristic/**: Implementaciones de heurísticas para el reconocimiento de entidades.
        - **topic/**: Clases relacionadas con la clasificación de temas.

    -**httpRequest/**: Clases para realizar peticiones HTTP y gestionar respuestas.
- **FeedReaderMain**: Código principal de la aplicación, incluyendo el parser de feeds, gestión de artículos y análisis de entidades.
- **subscriptions.json**: Lista de URLs de feeds a procesar.

Esta organización permite un desarrollo ordenado y facilita futuras ampliaciones o mantenimientos.



---

## Configuración del entorno y ejecución
Instrucciones para el usuario sobre cómo correr las dos partes del laboratorio con spark. Explicación del resultado que se espera luego de ejecutar cada parte.
El usuario debe ingresar en el Makefile la ruta al directorio de spark y colocarla en la variable **SPARK_HOME** para su correcta compilación 

**instrucciones de ejecución:**
Para imprimir los articulos sin las named entities  escribir **make run** en consola sobre la carpeta raiz del proyecto
Para imprimir el tituló de los articulos con un conteo de las entidades nombradas reconocidas por la euristica rapida proporcionada por la catédra escribir **make run-ne** en consola sobre la carpeta raiz del proyecyo


## Decisiones de diseño
- Extendimos la clase de rss parser para que pueda procesar xml tipo atom por medio de la clase hija atomparser.
- Los xml tipo atom maneja de una manera diferente la fecha por lo que tomamos la desición de devolver la fecha actual.
- Para mayor eficiencia y escalabilidad, optamos por implementar funciones serializables en lugar de funciones paralelizables. Ademas de que es mas recomendada para objetos mas estructurados.


## Conceptos importantes

1. **Flujo de la aplicación**  
La aplicación FeedReader sigue un flujo estructurado para procesar los feeds RSS y extraer las entidades nombradas. 
   1.  La aplicación comienza leyendo el archivo `subscriptions.json`, que contiene una lista de URLs de feeds RSS a procesar.
   2. Luego, se crea un dataset de Spark a partir de las URLs leídas, lo que permite manejar grandes volúmenes de datos de manera eficiente y paralelizada.
   3. **Parseo de feeds**: Una vez descargado el contenido, la tarea de parsear los feeds se paraleliza entre los workers y luego se junta toda esa información en el driver.
   Según si el usuraio pidió named entities o no, se realiza un procesamiento diferente:
      - Si no se solicitan named entities, simplemente se printean los artículos de cada feed.
      - Si se solicitan named entities, se procede a paralelizar la extracción de los artículos y cómputos de las entidades nombradas. Finalmente, ese cómputo se lleva al driver, el cual imprime el resultado.


2. **¿Por qué se decide usar Apache Spark para este proyecto?**  
Decidimos utilizar Spark para este proyecto debido a su capacidad para manejar grandes volúmenes de datos de manera eficiente y paralelizada. Spark permite procesar datos distribuidos en clústeres o nodos, lo que mejora significativamente el rendimiento en comparación con enfoques tradicionales de procesamiento secuencial con grandes volumenes de datos, como en este caso que hay que parsear 25 urls. 

3. **Liste las principales ventajas y desventajas que encontró al utilizar Spark. ¿Cómo se aplica el concepto de inversión de control en este laboratorio?**  
Como ventaja es que debido a que existe una inversion de control en la que el programdor solamente llena ciertos campos destinados para tal fin, lo que hace que la cantidad de código que se debe escribir es mucho menor. 
Sin embargo, esto puede ser tambien una desventaja al tener que descubrir exactamente qué función o método es útil para lo que se necesita.

5. **¿Considera que Spark requiere que el codigo original tenga una integración tight vs loose coupling?**  
Consideramos que tiene una integración loose coupling, ya que Spark no necesita saber la implementación interna de las funciones que se le pasan como parámetros. Esto se logra a partir de modularizar el código, permitiendo que las funciones puedan ser reemplazadas o modificadas sin afectar al resto del sistema. Este enfoque de diseño es beneficioso, porque facilita, además, la escalabilidad y paralilización del proyecto.

6. **¿El uso de Spark afectó la estructura de su código original?**  
En algunos aspectos se tuvo que adaptar el código original para que se integrara con Spark y los tipos de urls a parsear. Sin embargo, la estructura general del código se mantuvo coherente con el diseño original.
