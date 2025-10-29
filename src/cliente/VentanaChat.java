package cliente;

import javax.swing.*;

import comun.Mensaje;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;

public class VentanaChat extends JFrame {

    private JTextArea areaMensajes;
    private JTextField campoMensaje;
    private JButton botonEnviar;
    private JComboBox<String> comboUsuarios;
    private DefaultComboBoxModel<String> modeloUsuarios;

    private Socket socket;
    private ObjectOutputStream salidaObjeto;
    private ObjectInputStream entradaObjeto;

    private String usuario;

    public VentanaChat(String ip, int puerto, String usuario) {
        this.usuario = capitalize(usuario);
        setTitle("Chat - " + this.usuario);
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Área de mensajes
        areaMensajes = new JTextArea();
        areaMensajes.setEditable(false);
        areaMensajes.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(areaMensajes);

        // Campo de texto y botón
        campoMensaje = new JTextField();
        botonEnviar = new JButton("Enviar");

        // ComboBox de usuarios
        modeloUsuarios = new DefaultComboBoxModel<>();
        modeloUsuarios.addElement("A todos");
        comboUsuarios = new JComboBox<>(modeloUsuarios);

        // Panel inferior
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(comboUsuarios, BorderLayout.WEST);
        panelInferior.add(campoMensaje, BorderLayout.CENTER);
        panelInferior.add(botonEnviar, BorderLayout.EAST);

        add(scroll, BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);

        // Acción del botón
        botonEnviar.addActionListener(e -> enviarMensaje());
        campoMensaje.addActionListener(e -> enviarMensaje()); // Enter también envía

        // Conectar al servidor
        try {
            socket = new Socket(ip, puerto);
            salidaObjeto = new ObjectOutputStream(socket.getOutputStream());
            entradaObjeto = new ObjectInputStream(socket.getInputStream());

            // Enviar nombre de usuario al servidor
            salidaObjeto.writeObject(new Mensaje(this.usuario, null, this.usuario + " se ha conectado", Mensaje.Tipo.SISTEMA));
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

        String destinatario = (String) comboUsuarios.getSelectedItem();
        if ("A todos".equals(destinatario)) destinatario = null;

        try {
            Mensaje.Tipo tipo = (destinatario == null) ? Mensaje.Tipo.PUBLICO : Mensaje.Tipo.PRIVADO;
            Mensaje mensaje = new Mensaje(usuario, destinatario, texto, tipo);
            salidaObjeto.writeObject(mensaje);
            salidaObjeto.flush();

            if (tipo == Mensaje.Tipo.PRIVADO) {
                areaMensajes.append("💌 (Tú -> " + comboUsuarios.getSelectedItem() + "): " + texto + "\n");
            }else {
                areaMensajes.append("🗣️ Tú: " + texto + "\n");
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
                        SwingUtilities.invokeLater(() -> {
                            areaMensajes.append(msg.toString() + "\n");

                            if (msg.getTipo() == Mensaje.Tipo.SISTEMA) {
                                String contenido = msg.getContenido().toLowerCase();
                                String textoOriginal = msg.getContenido().trim();

                                // Lista de usuarios conectados
                                if (contenido.startsWith("usuarios_conectados:")) {
                                    String lista = textoOriginal.substring("usuarios_conectados:".length()).trim();
                                    if (!lista.isEmpty()) {
                                        String[] nombres = lista.split(",");
                                        for (String nombre : nombres) {
                                            nombre = capitalize(nombre.trim());
                                            if (!nombre.isEmpty() && !nombre.equals(usuario) && modeloUsuarios.getIndexOf(nombre) == -1) {
                                                modeloUsuarios.addElement(nombre);
                                            }
                                        }
                                    }
                                }

                                // Nuevo usuario conectado
                                else if (contenido.contains("se ha conectado") || contenido.contains("se ha unido")) {
                                    String[] partes = textoOriginal.split("\\s+");
                                    if (partes.length > 0) {
                                        String nuevoUsuario = capitalize(partes[1].trim());
                                        if (!nuevoUsuario.equals(usuario) && modeloUsuarios.getIndexOf(nuevoUsuario) == -1) {
                                            modeloUsuarios.addElement(nuevoUsuario);
                                        }
                                    }
                                }

                                // Usuario desconectado
                                else if (contenido.contains("ha abandonado el chat")) {
                                    if (textoOriginal.matches("<<\\s+.+\\s+ha abandonado el chat")) {
                                        String nombre = textoOriginal.replaceFirst("<<\\s+", "").replaceFirst("\\s+ha abandonado el chat", "").trim();
                                        nombre = capitalize(nombre);
                                        if (!nombre.isEmpty() && modeloUsuarios.getIndexOf(nombre) != -1) {
                                            modeloUsuarios.removeElement(nombre);
                                        }
                                    }
                                }
                            }
                        });
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

    // Capitaliza la primera letra de un nombre
    private String capitalize(String nombre) {
        if (nombre == null || nombre.isEmpty()) return nombre;
        return nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaConexion().setVisible(true);
        });
    }
}