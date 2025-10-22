package cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class VentanaConexion extends JFrame {

    private JTextField campoIP;
    private JTextField campoPuerto;
    private JTextField campoUsuario;
    private JButton botonConectar;

    public VentanaConexion() {
        setTitle("Conectar al Chat");
        setSize(350, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // IP
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("IP Servidor:"), gbc);

        campoIP = new JTextField("localhost");
        gbc.gridx = 1;
        add(campoIP, gbc);

        // Puerto
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Puerto:"), gbc);

        campoPuerto = new JTextField("3040");
        gbc.gridx = 1;
        add(campoPuerto, gbc);

        // Usuario
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Nombre de usuario:"), gbc);

        campoUsuario = new JTextField();
        gbc.gridx = 1;
        add(campoUsuario, gbc);

        // Botón conectar
        botonConectar = new JButton("Conectar");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(botonConectar, gbc);

        // Acción del botón
        botonConectar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                conectarAlServidor();
            }
        });
    }

    private void conectarAlServidor() {
        String ip = campoIP.getText().trim();
        String puertoStr = campoPuerto.getText().trim();
        String usuario = campoUsuario.getText().trim();

        if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int puerto;
        try {
            puerto = Integer.parseInt(puertoStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Puerto inválido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Aquí abriríamos la ventana de chat y cerraríamos esta ventana
        SwingUtilities.invokeLater(() -> {
            VentanaChat chat = new VentanaChat(ip, puerto, usuario);
            chat.setVisible(true);
            this.dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new VentanaConexion().setVisible(true);
        });
    }
}
