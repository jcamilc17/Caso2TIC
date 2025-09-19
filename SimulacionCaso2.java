// Integrantes: David Elias Forero - , Juan Camilo Caldas - 202322445
// Simulación Caso 2 - TIC - Infraestructura Computacional
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimulacionCaso2 {

    /* ================== Modelos básicos ================== */

    static final int BYTES_POR_ENTERO = 4;

    static class Dimension {
        int filas;
        int columnas;
        Dimension(int filas, int columnas) { this.filas = filas; this.columnas = columnas; }
    }

    static class Configuracion {
        int tamanoPaginaBytes;
        int numeroProcesos;
        List<Dimension> tamanios;
    }

    static class Referencia {
        int pagina;
        char operacion;       // 'r' o 'w'
        String etiqueta;      // "M1:[i-j]" (solo para logs)
        Referencia(int pagina, char operacion, String etiqueta){
            this.pagina = pagina; this.operacion = operacion; this.etiqueta = etiqueta;
        }
    }

    static class EntradaMarco {
        boolean presente;
        int indiceMarco;     // índice local dentro del proceso
        long timestamp;
    }

    static class Proceso {
        int id;

        int tamanoPaginaBytes;
        int numFilas;
        int numColumnas;
        int numReferencias;
        int numPaginas;

        List<Referencia> referencias = new ArrayList<>();
        Map<Integer, EntradaMarco> tablaPaginas = new HashMap<>();

        int capacidadMarcos;         // capacidad (cantidad de marcos asignados)
        int marcosEnUso;             // cuantos marcos locales tiene ocupados
        int indiceReferencia;        // índice de la referencia actual
        boolean[] esPrimerIntento;   // para contar hit/fallo solo 1 vez

        int aciertos;
        int fallos;
        int accesosSwap;

        // IDs de marcos globales que posee este proceso
        List<Integer> marcosGlobales = new ArrayList<>();

        Proceso(int id){ this.id = id; }
        boolean termino(){ return indiceReferencia >= referencias.size(); }
    }

    /* ================== Lectura de configuración por STDIN ================== */

    static Configuracion leerConfigDesdeStdin() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        List<String> lineas = new ArrayList<>();
        String s;
        while ((s = br.readLine()) != null) lineas.add(s);
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException(
                "No se recibió entrada por STDIN. Ejemplo:  java SimulacionCaso2 < data/config.txt");
        }
        return parsearConfig(lineas);
    }

    private static Configuracion parsearConfig(List<String> lineas) {
        int tamanoPaginaBytes = -1;
        int numeroProcesos = -1;
        String tamaniosRaw = null;

        for (String linea : lineas) {
            String t = linea.trim();
            if (t.isEmpty() || t.startsWith("#")) continue;
            if (t.startsWith("TP=")) tamanoPaginaBytes = Integer.parseInt(t.substring(3).trim());
            else if (t.startsWith("NPROC=")) numeroProcesos = Integer.parseInt(t.substring(6).trim());
            else if (t.startsWith("TAMS=")) tamaniosRaw = t.substring(5).trim();
        }
        if (tamanoPaginaBytes <= 0 || numeroProcesos <= 0 || tamaniosRaw == null)
            throw new IllegalArgumentException("config invalido (se requieren TP, NPROC y TAMS)");

        String[] tokensTamanios = tamaniosRaw.split(",");
        if (tokensTamanios.length != numeroProcesos)
            throw new IllegalArgumentException("NPROC no coincide con la cantidad de tamanios");

        Pattern patronTam = Pattern.compile("\\s*(\\d+)x(\\d+)\\s*");
        List<Dimension> listaTamanios = new ArrayList<>();
        for (String tok : tokensTamanios) {
            Matcher m = patronTam.matcher(tok);
            if (!m.matches()) throw new IllegalArgumentException("Tamanio invalido: " + tok);
            listaTamanios.add(new Dimension(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))));
        }

        Configuracion cfg = new Configuracion();
        cfg.tamanoPaginaBytes = tamanoPaginaBytes;
        cfg.numeroProcesos = numeroProcesos;
        cfg.tamanios = listaTamanios;
        return cfg;
    }

    /* ================== Opcion 1 ================== */

    static void generarArchivosProc(Configuracion cfg) {
        for (int idProceso = 0; idProceso < cfg.numeroProcesos; idProceso++) {
            Dimension dim = cfg.tamanios.get(idProceso);
            int nf = dim.filas, nc = dim.columnas, tp = cfg.tamanoPaginaBytes;
            int nr = nf * nc * 3;
            int bytesMatriz = nf * nc * BYTES_POR_ENTERO;
            int baseM1 = 0, baseM2 = bytesMatriz, baseM3 = 2 * bytesMatriz;

            int np = contarPaginasUnicas(tp, bytesMatriz, baseM1, baseM2, baseM3);

            try (PrintWriter out = new PrintWriter("proc" + idProceso + ".txt")) {
                // Cabecera
                out.println("TP=" + tp);
                out.println("NF=" + nf);
                out.println("NC=" + nc);
                out.println("NR=" + nr);
                out.println("NP=" + np);

                // Líneas M1/M2/M3
                for (int fila = 0; fila < nf; fila++) {
                    for (int col = 0; col < nc; col++) {
                        int pos = fila * nc + col;

                        int dir1 = baseM1 + pos * BYTES_POR_ENTERO;
                        out.println("M1:[" + fila + "-" + col + "]," + (dir1 / tp) + "," + (dir1 % tp) + ",r");

                        int dir2 = baseM2 + pos * BYTES_POR_ENTERO;
                        out.println("M2:[" + fila + "-" + col + "]," + (dir2 / tp) + "," + (dir2 % tp) + ",r");

                        int dir3 = baseM3 + pos * BYTES_POR_ENTERO;
                        out.println("M3:[" + fila + "-" + col + "]," + (dir3 / tp) + "," + (dir3 % tp) + ",w");
                    }
                }
            } catch (IOException e) {
                System.err.println("Error proc" + idProceso + ": " + e.getMessage());
            }
        }
    }

    static int contarPaginasUnicas(int tamanoPaginaBytes, int bytesMatriz, int baseM1, int baseM2, int baseM3) {
        Set<Integer> paginas = new HashSet<>();
        agregarRangoPaginas(paginas, tamanoPaginaBytes, baseM1, bytesMatriz);
        agregarRangoPaginas(paginas, tamanoPaginaBytes, baseM2, bytesMatriz);
        agregarRangoPaginas(paginas, tamanoPaginaBytes, baseM3, bytesMatriz);
        return paginas.size();
    }

    static void agregarRangoPaginas(Set<Integer> paginas, int tamanoPaginaBytes, int base, int bytes) {
        if (bytes <= 0) return;
        int primera = base / tamanoPaginaBytes;
        int ultima  = (base + bytes - 1) / tamanoPaginaBytes;
        for (int p = primera; p <= ultima; p++) paginas.add(p);
    }

    /* ================== Opcion 2 ================== */

    static long tiempoGlobal = 0;

    static Proceso cargarProcesoDesdeArchivo(int id) throws IOException {
        System.out.println("PROC " + id + " == Leyendo archivo de configuración ==");
        Proceso proceso = new Proceso(id);
        try (BufferedReader br = new BufferedReader(new FileReader("proc" + id + ".txt"))) {
            proceso.tamanoPaginaBytes = Integer.parseInt(br.readLine().split("=")[1].trim());
            System.out.println("PROC " + id + "leyendo TP. Tam Páginas: " + proceso.tamanoPaginaBytes);

            proceso.numFilas         = Integer.parseInt(br.readLine().split("=")[1].trim());
            System.out.println("PROC " + id + "leyendo NF. Num Filas: " + proceso.numFilas);

            proceso.numColumnas      = Integer.parseInt(br.readLine().split("=")[1].trim());
            System.out.println("PROC " + id + "leyendo NC. Num Cols: " + proceso.numColumnas);

            proceso.numReferencias   = Integer.parseInt(br.readLine().split("=")[1].trim());
            System.out.println("PROC " + id + "leyendo NR. Num Referencias: " + proceso.numReferencias);

            proceso.numPaginas       = Integer.parseInt(br.readLine().split("=")[1].trim());
            System.out.println("PROC " + id + "leyendo NP. Num Paginas: " + proceso.numPaginas);

            String linea;
            while ((linea = br.readLine()) != null) {
                String[] partes = linea.split(",");
                int pagina = Integer.parseInt(partes[1].trim());
                char op = partes[3].trim().charAt(0);
                proceso.referencias.add(new Referencia(pagina, op, partes[0]));
            }
        }
        System.out.println("PROC " + id + "== Terminó de leer archivo de configuración ==");
        proceso.esPrimerIntento = new boolean[proceso.referencias.size()];
        Arrays.fill(proceso.esPrimerIntento, true);
        return proceso;
    }

    static int elegirVictimaLRU(Proceso proceso) {
        int victima = -1; long mejor = Long.MAX_VALUE;
        for (Map.Entry<Integer, EntradaMarco> e : proceso.tablaPaginas.entrySet()) {
            EntradaMarco em = e.getValue();
            if (em.presente && em.timestamp < mejor) {
                mejor = em.timestamp;
                victima = e.getKey();
            }
        }
        if (victima == -1) {
            for (Map.Entry<Integer, EntradaMarco> e : proceso.tablaPaginas.entrySet()) {
                if (e.getValue().presente) { victima = e.getKey(); break; }
            }
        }
        return victima;
    }

    static void actualizarTimestamp(EntradaMarco entrada) { entrada.timestamp = ++tiempoGlobal; }

    static void terminarProceso(Proceso p, Queue<Integer> marcosLibres, List<Proceso> procesos) {
        System.out.println("========================");
        System.out.println("Termino proc: " + p.id);
        System.out.println("========================");

        for (int marcoGlobal : p.marcosGlobales) {
            System.out.println("PROC " + p.id + " removiendo marco: " + marcoGlobal);
            marcosLibres.add(marcoGlobal);
        }
        int marcosLiberados = p.marcosGlobales.size();

        p.marcosGlobales.clear();
        p.capacidadMarcos = 0;
        p.marcosEnUso = 0;
        p.tablaPaginas.clear();

        if (marcosLiberados > 0) {
            Proceso destino = null; int maxFallos = -1;
            for (Proceso q : procesos) {
                if (!q.termino() && q != p && q.fallos > maxFallos) { maxFallos = q.fallos; destino = q; }
            }
            if (destino != null) {
                for (int k = 0; k < marcosLiberados && !marcosLibres.isEmpty(); k++) {
                    int marcoGlobal = marcosLibres.remove();
                    destino.marcosGlobales.add(marcoGlobal);
                    destino.capacidadMarcos++;
                    System.out.println("PROC " + destino.id + " asignando marco nuevo " + marcoGlobal);
                }
            }
        }
    }

    static void simularEjecucion(int marcosTotales, int numeroProcesos) throws IOException {
        if (marcosTotales < numeroProcesos) throw new IllegalArgumentException("minimo 1 marco por proceso");

        Queue<Integer> marcosLibres = new ArrayDeque<>();
        for (int idMarco = 0; idMarco < marcosTotales; idMarco++) marcosLibres.add(idMarco);

        System.out.println("Inicio:");
        List<Proceso> procesos = new ArrayList<>(numeroProcesos);
        Queue<Proceso> cola = new ArrayDeque<>();

        int marcosPorProceso = marcosTotales / numeroProcesos;
        for (int id = 0; id < numeroProcesos; id++) {
            Proceso p = cargarProcesoDesdeArchivo(id);

            for (int k = 0; k < marcosPorProceso && !marcosLibres.isEmpty(); k++) {
                int marcoGlobal = marcosLibres.remove();
                p.marcosGlobales.add(marcoGlobal);
                System.out.println("Proceso " + p.id + ": recibe marco " + marcoGlobal);
            }
            p.capacidadMarcos = p.marcosGlobales.size();
            p.marcosEnUso = 0;

            procesos.add(p);
            cola.add(p);
        }

        while (!cola.isEmpty()) {
            Proceso p = cola.poll();

            if (p.termino()) {
                terminarProceso(p, marcosLibres, procesos);
                continue;
            }

            System.out.println("Turno proc: " + p.id);
            System.out.println("PROC " + p.id + " analizando linea_: " + p.indiceReferencia);

            Referencia ref = p.referencias.get(p.indiceReferencia);
            EntradaMarco entrada = p.tablaPaginas.get(ref.pagina);
            boolean paginaPresente = (entrada != null && entrada.presente);
            boolean esPrimerIntento = p.esPrimerIntento[p.indiceReferencia];

            if (paginaPresente) {
                if (esPrimerIntento) p.aciertos++;
                actualizarTimestamp(entrada);
                System.out.println("PROC " + p.id + " hits: " + p.aciertos);
                System.out.println("PROC " + p.id + " envejecimiento");
                p.esPrimerIntento[p.indiceReferencia] = false;
                p.indiceReferencia++;
            } else {
                if (esPrimerIntento) p.fallos++;

                if (p.marcosEnUso < p.capacidadMarcos) {
                    EntradaMarco nueva = new EntradaMarco();
                    nueva.presente = true; nueva.indiceMarco = p.marcosEnUso; actualizarTimestamp(nueva);
                    p.tablaPaginas.put(ref.pagina, nueva);
                    p.marcosEnUso++; p.accesosSwap += 1;
                } else {
                    int victima = elegirVictimaLRU(p);
                    EntradaMarco entradaVictima = p.tablaPaginas.get(victima);
                    int marcoLocal = (entradaVictima != null) ? entradaVictima.indiceMarco : 0;
                    if (entradaVictima != null) entradaVictima.presente = false;

                    EntradaMarco nueva = new EntradaMarco();
                    nueva.presente = true; nueva.indiceMarco = marcoLocal; actualizarTimestamp(nueva);
                    p.tablaPaginas.put(ref.pagina, nueva);
                    p.accesosSwap += 2;
                }
                System.out.println("PROC " + p.id + " falla de pag: " + p.fallos);
                System.out.println("PROC " + p.id + " envejecimiento");
                p.esPrimerIntento[p.indiceReferencia] = false;
            }

            if (p.termino()) {
                terminarProceso(p, marcosLibres, procesos);
            } else {
                cola.add(p);
            }
        }

        for (Proceso p : procesos) {
            System.out.println("Proceso: " + p.id);
            System.out.println("- Num referencias: " + p.referencias.size());
            System.out.println("- Fallas: " + p.fallos);
            System.out.println("- Hits: " + p.aciertos);
            System.out.println("- SWAP: " + p.accesosSwap);
            double tasaFallos = p.referencias.isEmpty() ? 0.0 : (double)p.fallos / p.referencias.size();
            double tasaExito  = p.referencias.isEmpty() ? 0.0 : (double)p.aciertos / p.referencias.size();
            System.out.printf(Locale.US, "- Tasa fallas: %.4f%n", tasaFallos);
            System.out.printf(Locale.US, "- Tasa éxito: %.4f%n", tasaExito);
        }
    }

    /* ================== main ================== */

    public static void main(String[] args) throws Exception {
        // Lee config de STDIN
        Configuracion cfg = leerConfigDesdeStdin();

        // Opcion 1
        generarArchivosProc(cfg);

        // Opcion 2 (por defecto: 4 marcos por proceso)
        int marcosTotales = Math.max(cfg.numeroProcesos * 4, cfg.numeroProcesos);
        simularEjecucion(marcosTotales, cfg.numeroProcesos);
    }
}
