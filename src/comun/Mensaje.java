package comun;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mensaje implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum Tipo {
		PUBLICO, PRIVADO, SISTEMA
	}

	private String remitente;
	private String destinatario; // null si es público
	private String contenido;
	private Tipo tipo;
	private String timestamp;

	// Constructor completo
	public Mensaje(String remitente, String destinatario, String contenido, Tipo tipo) {
		this.remitente = remitente;
		this.destinatario = destinatario;
		this.contenido = contenido;
		this.tipo = tipo;
		this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	// Constructor simplificado (por compatibilidad con mensajes del sistema)
	public Mensaje(String remitente, String contenido) {
		this(remitente, null, contenido, Tipo.SISTEMA);
	}

	// Getters
	public String getRemitente() {
		return remitente;
	}

	public String getDestinatario() {
		return destinatario;
	}

	public String getContenido() {
		return contenido;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public String getTimestamp() {
		return timestamp;
	}

	@Override
	public String toString() {
		switch (tipo) {
		case PUBLICO:
			return "[" + timestamp + "] " + remitente + ": " + contenido;
		case PRIVADO:
			return "[" + timestamp + "] (Privado) " + remitente + " -> " + destinatario + ": " + contenido;
		case SISTEMA:
			return "[" + timestamp + "] (Sistema): " + contenido;
		default:
			return contenido;
		}
	}
}
