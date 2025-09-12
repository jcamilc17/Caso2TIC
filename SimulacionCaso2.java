// Integrantes: David Elias Forero - , Juan Camilo Caldas - 202322445
// Simulación Caso 2 - TIC - Infraestructura Computacional
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

public class SimulacionCaso2 {

    public static class DimensionesMatriz { //Estructura auxiliar estilo tupla para almacenar dimensiones
        int filas;
        int columnas;

        public DimensionesMatriz(int filas, int columnas) {
            this.filas = filas;
            this.columnas = columnas;
        }
    }

    // Clase para representar una entrada en la tabla de páginas
    public static class EntradaPagina {
        boolean presente;        // ¿La página está en memoria RAM?
        int marcoFisico;        // ¿En qué marco físico está? (-1 si no está)
        long tiempoUltimoAcceso; // Para algoritmo LRU
    }

    // Clase para representar un proceso
    static class Proceso {
        int id;
        int tamPagina;
        int numFilas;
        int numColumnas;
        int numReferencias;
        int numPaginas;
        ArrayList<String> direccionesVirtuales;
        HashMap<Integer, EntradaPagina> tablaPaginas;
        ArrayList<Integer> marcosAsignados;
        int indiceActual;
        
        // Estadísticas
        int referencias;
        int fallos;
        int hits;
        int accesosSwap;
        
        public Proceso(int id) {
            this.id = id;
            this.direccionesVirtuales = new ArrayList<>();
            this.tablaPaginas = new HashMap<>();
            this.marcosAsignados = new ArrayList<>();
            this.indiceActual = 0;
            this.referencias = 0;
            this.fallos = 0;
            this.hits = 0;
            this.accesosSwap = 0;
        }
    }
    // Variables  globales para la opción 2
    private static long tiempoGlobal = 0;
    private static ArrayList<Proceso> procesos;
    private static Queue<Proceso> colaProcesos;
    private static int marcosTotal;

    public static void opcion1EscribirResultados(int TamsPaginas, int NumPROC, ArrayList<DimensionesMatriz> dimensiones) {
        for (int proceso = 0; proceso < NumPROC; proceso++) {
            DimensionesMatriz dim = dimensiones.get(proceso);
            int NumFilas = dim.filas; // Número de filas
            int NumColumnas = dim.columnas; // Número de columnas
            int NumReferencias = NumFilas * NumColumnas * 3; // Número de referencias
            int NumBytes = NumReferencias * 4; // Tamaño total en bytes (3 matrices de enteros)
            int NumBytesMatriz = NumBytes / 3; // Tamaño de cada matriz en bytes
            int NumPaginas = Math.ceilDiv(NumBytes, TamsPaginas);

            System.out.println("=== PROCESO " + proceso + " ===");
            System.out.println("TP=" + TamsPaginas);
            System.out.println("NF=" + NumFilas);
            System.out.println("NC=" + NumColumnas);
            System.out.println("NR=" + NumReferencias);
            System.out.println("NP=" + NumPaginas);

            try {
            // Crear archivo proc<i>.txt
            PrintWriter writer = new PrintWriter("proc" + proceso + ".txt");
            
            // Escribir cabecera del archivo
            writer.println("TP=" + TamsPaginas);
            writer.println("NF=" + NumFilas);
            writer.println("NC=" + NumColumnas);
            writer.println("NR=" + NumReferencias);
            writer.println("NP=" + NumPaginas);

            // Generar direcciones virtuales por cada celda [i][j]
            for (int i = 0; i < NumFilas; i++) {
                for (int j = 0; j < NumColumnas; j++) {
                    int posicion = i * NumColumnas + j; 

                    // Calcular direcciones virtuales para cada matriz
                    int dir_M1 = posicion * 4; // M1 empieza en 0
                    int dir_M2 = NumBytesMatriz + posicion * 4; // M2 empieza después de M1
                    int dir_M3 = 2 * NumBytesMatriz + posicion * 4; // M3 empieza después de M2

                    // Calcular páginas virtuales
                    int pag_M1 = dir_M1 / TamsPaginas; // Pagina virtual correspondiente a M1
                    int pag_M2 = dir_M2 / TamsPaginas; // Pagina virtual correspondiente a M2
                    int pag_M3 = dir_M3 / TamsPaginas; // Pagina virtual correspondiente a M3

                    // Calcular offset dentro de la página
                    int off_M1 = dir_M1 % TamsPaginas; // Offset dentro de la página para M1
                    int off_M2 = dir_M2 % TamsPaginas; // Offset dentro de la página para M2
                    int off_M3 = dir_M3 % TamsPaginas; // Offset dentro de la página para M3

                    // Escribir las 3 referencias para esta celda
                    writer.println("M1:[" + i + "-" + j + "]," + pag_M1 + "," + off_M1 + ",r");
                    writer.println("M2:[" + i + "-" + j + "]," + pag_M2 + "," + off_M2 + ",r");
                    writer.println("M3:[" + i + "-" + j + "]," + pag_M3 + "," + off_M3 + ",w");
                }
            }
            writer.close();
            System.out.println("Archivo proc" + proceso + ".txt generado exitosamente");
            } catch (FileNotFoundException e) {
                System.err.println("Error creando archivo proc" + proceso + ".txt: " + e.getMessage());
            }
        }
        System.out.println(); // Línea en blanco entre procesos
    }

    public static void opcion2SimularEjecucio(){

    }
    

    public static void main(String[] args) {
        // Configuracion de la simulación
        int TP = 128;
        int NPROC = 3;
        ArrayList<DimensionesMatriz> dimensiones = new ArrayList<>();
        dimensiones.add(new DimensionesMatriz(4, 4));
        dimensiones.add(new DimensionesMatriz(8, 8));
        dimensiones.add(new DimensionesMatriz(12, 12));
        // para cada caso NPROC
        opcion1EscribirResultados(TP, NPROC, dimensiones);
    }

}
