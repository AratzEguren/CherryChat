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
		this.usuario = usuario;

		setTitle("Chat - " + usuario);
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

			// Enviar nombre de usuario al servidor
			salidaObjeto.writeObject(new Mensaje(usuario, null, usuario + " se ha conectado", Mensaje.Tipo.SISTEMA));
			salidaObjeto.flush();

			// Hilo receptor
			new Thread(new ReceptorMensajes()).start();

		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor: " + ex.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}

	private void enviarMensaje() {
		String texto = campoMensaje.getText().trim();
		if (texto.isEmpty())
			return;

		String destinatario = (String) comboUsuarios.getSelectedItem();
		if ("A todos".equals(destinatario))
			destinatario = null;

		try {
			Mensaje.Tipo tipo = (destinatario == null) ? Mensaje.Tipo.PUBLICO : Mensaje.Tipo.PRIVADO;
			Mensaje mensaje = new Mensaje(usuario, destinatario, texto, tipo);
			salidaObjeto.writeObject(mensaje);
			salidaObjeto.flush();

			if (tipo == Mensaje.Tipo.PRIVADO) {
				areaMensajes.append("💌 (Tú -> " + comboUsuarios.getSelectedItem() + "): " + texto + "\n");
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

								if (contenido.startsWith("usuarios_conectados:")) {
									String lista = msg.getContenido().substring("usuarios_conectados:".length()).trim();
									if (!lista.isEmpty()) {
										String[] nombres = lista.split(",");
										for (String nombre : nombres) {
											nombre = nombre.trim();
											if (!nombre.isEmpty() && !nombre.equals(usuario)
													&& modeloUsuarios.getIndexOf(nombre) == -1) {
												modeloUsuarios.addElement(nombre);
											}
										}
									}
								} else if (contenido.contains("se ha conectado") || contenido.contains("se ha unido")) {
									String nuevoUsuario = contenido.trim().split("\\s+")[1];
									if (!nuevoUsuario.equals(usuario)
											&& modeloUsuarios.getIndexOf(nuevoUsuario) == -1) {
										modeloUsuarios.addElement(nuevoUsuario);
									}
								} else if (contenido.contains("se ha desconectado")
										|| contenido.contains("ha salido")) {
									String usuarioDesconectado = msg.getEmisor();
									if (modeloUsuarios.getIndexOf(usuarioDesconectado) != -1) {
										modeloUsuarios.removeElement(usuarioDesconectado);
									}
								}
							}
						});
					}
				}
			} catch (IOException |

					ClassNotFoundException e) {
				SwingUtilities.invokeLater(() -> areaMensajes.append("🔴 Conexión perdida.\n"));
			} finally {
				try {
					if (entradaObjeto != null)
						entradaObjeto.close();
					if (salidaObjeto != null)
						salidaObjeto.close();
					if (socket != null)
						socket.close();
				} catch (IOException ignored) {
				}
			}
		}

	}

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			new VentanaConexion().setVisible(true);
		});
	}
}