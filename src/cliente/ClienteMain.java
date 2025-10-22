package cliente;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class ClienteMain {

    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String nombreUsuario;
    private boolean conectado = false;

    public ClienteMain(String ip, int puerto) {
        try {
            socket = new Socket(ip, puerto);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);
            conectado = true;

            // Esperar mensaje inicial del servidor
            String mensaje = entrada.readLine();
            if ("SOLICITAR_NOMBRE".equals(mensaje)) {
                System.out.print("Introduce tu nombre de usuario: ");
                Scanner sc = new Scanner(System.in);
                nombreUsuario = sc.nextLine();
                salida.println(nombreUsuario);
            }

            System.out.println("✅ Conectado al servidor como " + nombreUsuario);
            System.out.println("Escribe tu mensaje (o '/privado nombre mensaje' para privados, '/quit' para salir)");

            // Hilo para escuchar mensajes del servidor
            Thread receptor = new Thread(new Receptor());
            receptor.start();

            // Leer mensajes desde teclado y enviarlos
            Scanner teclado = new Scanner(System.in);
            while (conectado) {
                String linea = teclado.nextLine();
                if (linea.equalsIgnoreCase("/quit") || linea.equalsIgnoreCase("DESCONECTAR")) {
                    salida.println("DESCONECTAR");
                    desconectar();
                    break;
                } else {
                    salida.println(linea);
                }
            }

        } catch (IOException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
        }
    }

    private void desconectar() {
        conectado = false;
        try {
            if (entrada != null) entrada.close();
        } catch (IOException ignored) {}
        if (salida != null) salida.close();
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        System.out.println("🔴 Desconectado del servidor.");
    }

    // Clase interna: hilo que escucha mensajes del servidor
    private class Receptor implements Runnable {
        @Override
        public void run() {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    procesarMensaje(linea);
                }
            } catch (IOException e) {
                if (conectado) System.err.println("⚠️ Conexión cerrada: " + e.getMessage());
            } finally {
                desconectar();
            }
        }

        private void procesarMensaje(String mensaje) {
            if (mensaje.startsWith("PUBLICO:")) {
                System.out.println("🟢 " + mensaje.substring(8));
            } else if (mensaje.startsWith("PRIVADO:")) {
                System.out.println("💌 " + mensaje.substring(8));
            } else if (mensaje.startsWith("SYSTEM:")) {
                System.out.println("⚙️ " + mensaje.substring(7));
            } else if (mensaje.startsWith("LOCAL:")) {
                System.out.println("📤 " + mensaje.substring(6));
            } else if (mensaje.startsWith("ERROR:")) {
                System.out.println("❌ " + mensaje.substring(6));
            } else {
                System.out.println(mensaje);
            }
        }
    }

    public static void main(String[] args) {
        String ip = "localhost";
        int puerto = 3040;

        if (args.length >= 1) ip = args[0];
        if (args.length >= 2) puerto = Integer.parseInt(args[1]);

        new ClienteMain(ip, puerto);
    }
}
