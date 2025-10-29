package servidor;

import java.io.*;
import java.net.Socket;
import comun.Mensaje;

public class ClienteHandler extends Thread {

	private final Socket socket;
	private final ServidorMain servidor;
	private ObjectOutputStream salida;
	private ObjectInputStream entrada;
	private String nombreUsuario;

	public ClienteHandler(Socket socket, ServidorMain servidor) {
		this.socket = socket;
		this.servidor = servidor;
	}

	@Override
	public void run() {
		try {
			// ⚠️ El orden de creación de streams es importante
			salida = new ObjectOutputStream(socket.getOutputStream());
			entrada = new ObjectInputStream(socket.getInputStream());

			// ✅ Primer mensaje: el cliente envía su nombre dentro de un objeto Mensaje
			Mensaje mensajeInicial = (Mensaje) entrada.readObject();
			if (mensajeInicial == null || mensajeInicial.getRemitente() == null
					|| mensajeInicial.getRemitente().trim().isEmpty()) {
				enviarMensaje(new Mensaje("SERVER", "ERROR:NOMBRE_INVALIDO"));
				cerrarConexion();
				return;
			}

			this.nombreUsuario = mensajeInicial.getRemitente().trim();

			// 🔎 Comprobar duplicados
			synchronized (servidor) {
				boolean existe = servidor.getClientes().stream().anyMatch(
						c -> c.getNombreUsuario() != null && c.getNombreUsuario().equalsIgnoreCase(nombreUsuario));
				if (existe) {
					enviarMensaje(new Mensaje("SERVER", "ERROR:NOMBRE_DUPLICADO"));
					cerrarConexion();
					return;
				}
			}

			// ✅ Registrar usuario
			servidor.registrarCliente(this);
			servidor.broadcast(
					new Mensaje("SERVER", null, ">> " + nombreUsuario + " se ha unido al chat", Mensaje.Tipo.SISTEMA),
					this);

			// 🔁 Bucle de recepción
			Mensaje recibido;
			while ((recibido = (Mensaje) entrada.readObject()) != null) {
				String texto = recibido.getContenido();

				// 📴 Desconexión
				if (texto.equalsIgnoreCase("/quit") || texto.equalsIgnoreCase("DESCONECTAR")) {
					break;
				}

				// 📬 Mensaje privado o público
				if (recibido.getDestinatario() != null && !recibido.getDestinatario().isEmpty()) {
					boolean enviado = servidor.enviarPrivado(recibido, this);
					if (!enviado) {
						enviarMensaje(new Mensaje("SERVER",
								"El usuario '" + recibido.getDestinatario() + "' no está conectado."));
					}
				} else {
					servidor.broadcast(recibido, this);
				}
			}

		} catch (IOException | ClassNotFoundException e) {
			System.err.println("Error en conexión con cliente " + nombreUsuario + ": " + e.getMessage());
		} finally {
			servidor.eliminarCliente(this);
			servidor.broadcast(
					new Mensaje("SERVER", null, "<< " + nombreUsuario + " ha abandonado el chat", Mensaje.Tipo.SISTEMA),
					this);
			cerrarConexion();
		}
	}

	// Enviar objeto Mensaje
	public void enviarMensaje(Mensaje mensaje) {
		try {
			if (salida != null) {
				salida.writeObject(mensaje);
				salida.flush();
			}
		} catch (IOException e) {
			System.err.println("Error al enviar mensaje a " + nombreUsuario + ": " + e.getMessage());
		}
	}

	public String getNombreUsuario() {
		return nombreUsuario;
	}

	private void cerrarConexion() {
		try {
			if (entrada != null)
				entrada.close();
		} catch (IOException ignored) {
		}
		try {
			if (salida != null)
				salida.close();
		} catch (IOException ignored) {
		}
		try {
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException ignored) {
		}
	}
}
