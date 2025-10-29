package servidor;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

import comun.Mensaje;

public class ServidorMain {

    private final int puerto;
    private final ServerSocket serverSocket;
    private final List<ClienteHandler> clientes = new ArrayList<>();
    private final File logFile = new File("log.txt");
    private final int MAX_CLIENTES;
    private volatile String ultimoMensaje = "Ninguno";
    private final LocalDateTime inicio;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public ServidorMain(int puerto, int maxClientes) throws IOException {
        this.puerto = puerto;
        this.MAX_CLIENTES = maxClientes;
        this.serverSocket = new ServerSocket(this.puerto);
        this.inicio = LocalDateTime.now();
        escribirLog("SERVIDOR INICIADO en puerto " + puerto + " - " + timestamp());
        arrancarInformePeriodico(10); // muestra info cada 10 segundos
    }

    public static void main(String[] args) {
        int puerto = 3040;
        int maxClientes = 5;
        if (args.length >= 1) puerto = Integer.parseInt(args[0]);
        if (args.length >= 2) maxClientes = Integer.parseInt(args[1]);

        try {
            ServidorMain servidor = new ServidorMain(puerto, maxClientes);
            servidor.aceptarConexiones();
        } catch (IOException e) {
            System.err.println("No se pudo arrancar el servidor: " + e.getMessage());
        }
    }

    public void aceptarConexiones() {
        System.out.println("Servidor escuchando en puerto " + puerto);
        while (true) {
            try {
                Socket socket = serverSocket.accept();

                synchronized (this) {
                    if (clientes.size() >= MAX_CLIENTES) {
                        ObjectOutputStream salida = new ObjectOutputStream(socket.getOutputStream());
                        Mensaje error = new Mensaje("SERVER", "ERROR:SERVIDOR_LLENO");
                        salida.writeObject(error);
                        salida.flush();
                        salida.close();
                        escribirLog("Conexión rechazada (servidor lleno) desde " + socket.getRemoteSocketAddress());
                        socket.close();
                        continue;
                    }
                }

                ClienteHandler handler = new ClienteHandler(socket, this);
                handler.start();

            } catch (IOException e) {
                System.err.println("Error aceptando conexión: " + e.getMessage());
            }
        }
    }

    // ===========================
    // CLIENTES
    // ===========================

    public synchronized boolean registrarCliente(ClienteHandler cliente) {
        // Evitar nombres duplicados
        for (ClienteHandler c : clientes) {
            if (c.getNombreUsuario() != null && c.getNombreUsuario().equalsIgnoreCase(cliente.getNombreUsuario())) {
                return false;
            }
        }
        clientes.add(cliente);
        escribirLog("Usuario entra: " + cliente.getNombreUsuario());
        return true;
    }

    public synchronized void eliminarCliente(ClienteHandler cliente) {
        if (clientes.remove(cliente)) {
            escribirLog("Usuario sale: " + cliente.getNombreUsuario());
        }
    }

    public synchronized List<ClienteHandler> getClientes() {
        return new ArrayList<>(clientes);
    }

    // ===========================
    // MENSAJES
    // ===========================

    // Mensaje público
    public synchronized void broadcast(Mensaje mensaje, ClienteHandler emisor) {
        ultimoMensaje = mensaje.getContenido();
        escribirLog("Mensaje público de " + mensaje.getRemitente() + ": " + mensaje.getContenido());

        for (ClienteHandler c : clientes) {
            if (c != emisor) {
                c.enviarMensaje(mensaje);
            }
        }
    }

    // Mensaje privado
    public synchronized boolean enviarPrivado(Mensaje mensaje, ClienteHandler emisor) {
        String destinatario = mensaje.getDestinatario();
        ultimoMensaje = "[PRIVADO] " + mensaje.getRemitente() + " -> " + destinatario + ": " + mensaje.getContenido();
        escribirLog("Mensaje privado: " + ultimoMensaje);

        for (ClienteHandler c : clientes) {
            if (c.getNombreUsuario() != null && c.getNombreUsuario().equalsIgnoreCase(destinatario)) {
                c.enviarMensaje(mensaje);
                return true;
            }
        }

        // Si no se encuentra el destinatario
        emisor.enviarMensaje(new Mensaje("SERVER", "Usuario '" + destinatario + "' no encontrado."));
        return false;
    }

    // ===========================
    // LOG Y ESTADÍSTICAS
    // ===========================

    public synchronized void escribirLog(String texto) {
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println("[" + timestamp() + "] " + texto);
        } catch (IOException e) {
            System.err.println("No se pudo escribir en log: " + e.getMessage());
        }
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public synchronized int getNumeroConectados() {
        return clientes.size();
    }

    public Duration getTiempoActivo() {
        return Duration.between(inicio, LocalDateTime.now());
    }

    public synchronized String getUltimoMensaje() {
        return ultimoMensaje;
    }

    private void arrancarInformePeriodico(int segundos) {
        scheduler.scheduleAtFixedRate(() -> {
            String info = String.format(
                "INFO -> Usuarios conectados: %d | Tiempo activo: %s | Último mensaje: %s",
                getNumeroConectados(),
                formatearDuracion(getTiempoActivo()),
                getUltimoMensaje());
            System.out.println(info);
        }, segundos, segundos, TimeUnit.SECONDS);
    }

    private String formatearDuracion(Duration d) {
        long s = d.getSeconds();
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return String.format("%02dh:%02dm:%02ds", h, m, sec);
    }
}
