# Compilación

1. Ubica SimulacionCaso2.java en tu carpeta de trabajo.
2. Compila:

javac SimulacionCaso2.java

# Ejecución: modos disponibles

## A) Flujo clásico con redirección (genera y simula)

Usa un archivo de configuración y redirígelo a la entrada estándar:

java SimulacionCaso2 < config.txt

Comportamiento:

* Lee TP, NPROC y TAMS desde config.txt.
* Genera proc0.txt … proc(NPROC-1).txt.
* Simula con marcosTotales = max(4*NPROC, NPROC) (equivale a 4 marcos por proceso).

## B) Menú interactivo (sin STDIN): Opción 1 — Generar

java SimulacionCaso2

* Selecciona 1.
* Indica la ruta del archivo de configuración (por ejemplo config.txt).
* Se crean proc<i>.txt en el directorio actual.

## C) Menú interactivo (sin STDIN): Opción 2 — Simular

java SimulacionCaso2

* Selecciona 2.
* Ingresa MARCOS_TOTALES,NPROC (ej.: 8,2).
* Se leen proc0.txt … proc(NPROC-1).txt del directorio actual y se ejecuta la simulación.

## D) Generar y luego simular con el menú

1. Ejecuta Opción 1 con tu config.txt.
2. Luego ejecuta Opción 2 con MARCOS_TOTALES,NPROC.
