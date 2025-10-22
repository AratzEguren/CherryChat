package cliente;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Tipo {
        PUBLICO,
        PRIVADO,
        SISTEMA
    }

    private String emisor;
    private String destinatario; // null si es público
    private String contenido;
    private Tipo tipo;
    private String timestamp;

    public Mensaje(String emisor, String destinatario, String contenido, Tipo tipo) {
        this.emisor = emisor;
        this.destinatario = destinatario;
        this.contenido = contenido;
        this.tipo = tipo;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // Getters
    public String getEmisor() { return emisor; }
    public String getDestinatario() { return destinatario; }
    public String getContenido() { return contenido; }
    public Tipo getTipo() { return tipo; }
    public String getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        switch (tipo) {
            case PUBLICO:
                return "[" + timestamp + "] " + emisor + ": " + contenido;
            case PRIVADO:
                return "[" + timestamp + "] (Privado) " + emisor + " -> " + destinatario + ": " + contenido;
            case SISTEMA:
                return "[" + timestamp + "] (Sistema): " + contenido;
            default:
                return contenido;
        }
    }
}
