package cliente;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class ListenerMensajes implements Runnable {

    private ObjectInputStream entradaObjeto;
    private JTextArea areaMensajes; // Para Swing, puede ser null si es consola
    private boolean mostrarConsola = false; // true si queremos imprimir en consola

    public ListenerMensajes(ObjectInputStream entradaObjeto, JTextArea areaMensajes) {
        this.entradaObjeto = entradaObjeto;
        this.areaMensajes = areaMensajes;
        this.mostrarConsola = (areaMensajes == null);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object obj = entradaObjeto.readObject();
                if (obj instanceof Mensaje) {
                    Mensaje msg = (Mensaje) obj;
                    mostrarMensaje(msg);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            mostrarMensaje(new Mensaje("Sistema", null, "🔴 Conexión perdida", Mensaje.Tipo.SISTEMA));
        } finally {
            try {
                if (entradaObjeto != null) entradaObjeto.close();
            } catch (IOException ignored) {}
        }
    }

    private void mostrarMensaje(Mensaje msg) {
        String texto = msg.toString() + "\n";

        if (mostrarConsola) {
            System.out.print(texto);
        } else {
            SwingUtilities.invokeLater(() -> areaMensajes.append(texto));
        }
    }
}
