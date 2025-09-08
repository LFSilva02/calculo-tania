package backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GaussSwing extends JFrame {
    private JTextField sizeField;
    private JPanel matrixPanel;
    private JButton solveButton;
    private JTextArea resultArea;
    private JTextField[][] coefFields;
    private JTextField[] rhsFields;
    private int n;

    public GaussSwing() {
        setTitle("Eliminação de Gauss - Sistema Linear");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 500);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Tamanho da matriz (n): "));
        sizeField = new JTextField("3", 3);
        topPanel.add(sizeField);
        JButton setSizeButton = new JButton("Definir tamanho");
        setSizeButton.addActionListener(this::setMatrixSize);
        topPanel.add(setSizeButton);
        add(topPanel, BorderLayout.NORTH);

        matrixPanel = new JPanel();
        add(matrixPanel, BorderLayout.CENTER);

        solveButton = new JButton("Resolver sistema");
        solveButton.addActionListener(this::solveSystem);
        solveButton.setEnabled(false);
        add(solveButton, BorderLayout.SOUTH);

        resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.EAST);
    }

    private void setMatrixSize(ActionEvent e) {
        try {
            n = Integer.parseInt(sizeField.getText().trim());
            if (n < 2 || n > 10) {
                JOptionPane.showMessageDialog(this, "Escolha n entre 2 e 10.");
                return;
            }
            matrixPanel.removeAll();
            matrixPanel.setLayout(new GridLayout(n, n + 1, 5, 5));
            coefFields = new JTextField[n][n];
            rhsFields = new JTextField[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    coefFields[i][j] = new JTextField(4);
                    matrixPanel.add(coefFields[i][j]);
                }
                rhsFields[i] = new JTextField(4);
                matrixPanel.add(rhsFields[i]);
            }
            matrixPanel.revalidate();
            matrixPanel.repaint();
            solveButton.setEnabled(true);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Tamanho inválido.");
        }
    }

    private void solveSystem(ActionEvent e) {
        try {
            double[][] A = new double[n][n];
            double[] b = new double[n];
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    A[i][j] = Double.parseDouble(coefFields[i][j].getText().trim());
                }
                b[i] = Double.parseDouble(rhsFields[i].getText().trim());
            }
            
            // Solução por Gauss
            double[] x = GaussEliminacao.solve(A, b);
            
            // Decomposição LU
            double[][][] LU = GaussEliminacao.decomposicaoLU(A);
            double[][] L = LU[0];
            double[][] U = LU[1];
            
            StringBuilder sb = new StringBuilder();
            sb.append("Solução por Eliminação de Gauss:\n");
            for (int i = 0; i < n; i++) {
                sb.append("x[").append(i + 1).append("] = ").append(String.format("%.6f", x[i])).append("\n");
            }
            
            sb.append("\nDecomposição LU:\n");
            sb.append("L = \n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(String.format("%8.3f", L[i][j]));
                }
                sb.append("\n");
            }
            
            sb.append("\nU = \n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(String.format("%8.3f", U[i][j]));
                }
                sb.append("\n");
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Erro: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GaussSwing().setVisible(true));
    }
}
