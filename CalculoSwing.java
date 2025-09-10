package backend;

import javax.swing.*;
import java.awt.*;

public class CalculoSwing extends JFrame {
    public CalculoSwing() {
        setTitle("Cálculo Numérico - Métodos");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Gauss + LU", new GaussPanel());
        tabs.addTab("Bissecção", new BisseccaoPanel());
        add(tabs);
    }

    // Painel para Gauss + LU
    static class GaussPanel extends backend.GaussSwing {
        public GaussPanel() {
            super();
        }
    }

    // Painel para Bissecção
    static class BisseccaoPanel extends backend.BisseccaoSwing {
        public BisseccaoPanel() {
            super();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CalculoSwing().setVisible(true));
    }
}
