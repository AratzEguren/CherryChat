package servidor;

import java.io.*;
import java.net.Socket;

public class ClienteHandler extends Thread {
    private final Socket socket;
    private final ServidorMain servidor;
    private PrintWriter salida;
    private BufferedReader entrada;
    private String nombreUsuario;

    public ClienteHandler(Socket socket, ServidorMain servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            salida = new PrintWriter(socket.getOutputStream(), true);

            // Primer mensaje esperado: nombre de usuario
            salida.println("SOLICITAR_NOMBRE"); // protocolo simple
            String posibleNombre = entrada.readLine();
            if (posibleNombre == null || posibleNombre.trim().isEmpty()) {
                salida.println("ERROR:NOMBRE_INVALIDO");
                cerrarConexion();
                return;
            }
            this.nombreUsuario = posibleNombre.trim();

            // Registrar en servidor
            servidor.registrarCliente(this);
            servidor.broadcast(">> " + nombreUsuario + " se ha unido al chat", this);

            // Bucle de lectura de mensajes
            String linea;
            while ((linea = entrada.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                // Manejar desconexión pedido por cliente
                if (linea.equalsIgnoreCase("DESCONECTAR") || linea.equalsIgnoreCase("/quit")) {
                    break;
                }

                // Protocolos soportados:
                // 1) Comando tipo /privado destinatario mensaje...
                // 2) Mensaje normal -> público
                // 3) Mensaje explícito PRV:dest:mensaje  (por si cliente lo envía así)

                if (linea.startsWith("/privado ")) {
                    // formato: /privado destinatario texto...
                    String[] partes = linea.split(" ", 3);
                    if (partes.length >= 3) {
                        String destinatario = partes[1];
                        String texto = partes[2];
                        boolean ok = servidor.enviarPrivado(destinatario, texto, nombreUsuario);
                        if (!ok) {
                            enviarMensaje("SYSTEM:Usuario '" + destinatario + "' no encontrado.");
                        } else {
                            enviarMensaje("LOCAL:Has enviado (privado) a " + destinatario + ": " + texto);
                        }
                    } else {
                        enviarMensaje("SYSTEM:Uso correcto: /privado <usuario> <mensaje>");
                    }
                } else if (linea.startsWith("PRIVADO:")) {
                    // posible protocolo: PRIVADO:dest:mensaje
                    String[] p = linea.split(":", 3);
                    if (p.length >= 3) {
                        String destinatario = p[1];
                        String texto = p[2];
                        boolean ok = servidor.enviarPrivado(destinatario, texto, nombreUsuario);
                        if (!ok) enviarMensaje("SYSTEM:Usuario '" + destinatario + "' no encontrado.");
                    } else {
                        enviarMensaje("SYSTEM:Formato PRIVADO incorrecto.");
                    }
                } else {
                    // Mensaje público
                    servidor.broadcast(nombreUsuario + ": " + linea, this);
                }
            }

        } catch (IOException e) {
            System.err.println("Error en conexión con cliente " + nombreUsuario + ": " + e.getMessage());
        } finally {
            servidor.eliminarCliente(this);
            servidor.broadcast("<< " + nombreUsuario + " ha abandonado el chat", this);
            cerrarConexion();
        }
    }

    // Enviar texto a este cliente
    public void enviarMensaje(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    private void cerrarConexion() {
        try {
            if (entrada != null) entrada.close();
        } catch (IOException ignored) {}
        if (salida != null) salida.close();
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}
