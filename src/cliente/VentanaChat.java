package cliente;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class VentanaChat extends JFrame {

    private JTextArea areaMensajes;
    private JTextField campoMensaje;
    private JButton botonEnviar;

    private Socket socket;
    private ObjectOutputStream salidaObjeto;
    private ObjectInputStream entradaObjeto;

    private String usuario;

    public VentanaChat(String ip, int puerto, String usuario) {
        this.usuario = usuario;

        setTitle("Chat - " + usuario);
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Layout
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(areaMensajes);

        campoMensaje = new JTextField();
        botonEnviar = new JButton("Enviar");

        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(campoMensaje, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        // Acción del botón
        botonEnviar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarMensaje();
            }
        });

        campoMensaje.addActionListener(e -> enviarMensaje()); // Enter también envía

        // Conectar al servidor
        try {
            socket = new Socket(ip, puerto);
            salidaObjeto = new ObjectOutputStream(socket.getOutputStream());
            entradaObjeto = new ObjectInputStream(socket.getInputStream());

            // Enviar nombre de usuario al servidor (protocolo simple)
            salidaObjeto.writeObject(new Mensaje(usuario, null, usuario + " se ha conectado", Mensaje.Tipo.SISTEMA));
            salidaObjeto.flush();

            // Hilo receptor
            new Thread(new ReceptorMensajes()).start();

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    private void enviarMensaje() {
        String texto = campoMensaje.getText().trim();
        if (texto.isEmpty()) return;

        try {
            if (texto.startsWith("/privado ")) {
                // Formato: /privado destinatario mensaje...
                String[] partes = texto.split(" ", 3);
                if (partes.length >= 3) {
                    String destinatario = partes[1];
                    String contenido = partes[2];
                    Mensaje privado = new Mensaje(usuario, destinatario, contenido, Mensaje.Tipo.PRIVADO);
                    salidaObjeto.writeObject(privado);
                    salidaObjeto.flush();
                    areaMensajes.append("💌 (Tú -> " + destinatario + "): " + contenido + "\n");
                } else {
                    areaMensajes.append("⚙️ Uso correcto: /privado <usuario> <mensaje>\n");
                }
            } else {
                // Mensaje público
                Mensaje publico = new Mensaje(usuario, null, texto, Mensaje.Tipo.PUBLICO);
                salidaObjeto.writeObject(publico);
                salidaObjeto.flush();
            }
            campoMensaje.setText("");
        } catch (IOException e) {
            areaMensajes.append("❌ Error al enviar mensaje: " + e.getMessage() + "\n");
        }
    }

    private class ReceptorMensajes implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Object obj = entradaObjeto.readObject();
                    if (obj instanceof Mensaje) {
                        Mensaje msg = (Mensaje) obj;
                        SwingUtilities.invokeLater(() -> areaMensajes.append(msg.toString() + "\n"));
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                SwingUtilities.invokeLater(() -> areaMensajes.append("🔴 Conexión perdida.\n"));
            } finally {
                try {
                    if (entradaObjeto != null) entradaObjeto.close();
                    if (salidaObjeto != null) salidaObjeto.close();
                    if (socket != null) socket.close();
                } catch (IOException ignored) {}
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaChat("localhost", 3040, "Aratz").setVisible(true);
        });
    }
}
