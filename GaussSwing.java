package backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class GaussSwing extends JPanel {
    private JTextField sizeField;
    private JPanel matrixPanel;
    private JButton solveButton;
    private JTextArea resultArea;
    private JTextField[][] coefFields;
    private JTextField[] rhsFields;
    private int n;

    // Para uso standalone
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Método de Gauss");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new GaussSwing());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public GaussSwing() {
        setLayout(new BorderLayout());

        // Painel superior com tamanho da matriz e botões
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Tamanho da matriz (n): "));
        sizeField = new JTextField("3", 3);
        topPanel.add(sizeField);
        JButton setSizeButton = new JButton("Definir tamanho");
        setSizeButton.addActionListener(this::setMatrixSize);
        topPanel.add(setSizeButton);
        add(topPanel, BorderLayout.NORTH);

        // Painel central para a matriz
        matrixPanel = new JPanel();
        add(matrixPanel, BorderLayout.CENTER);

        // Painel inferior com botões
        JPanel bottomPanel = new JPanel();
        solveButton = new JButton("Resolver sistema");
        solveButton.addActionListener(this::solveSystem);
        solveButton.setEnabled(false);
        bottomPanel.add(solveButton);

        // Exemplo predefinido
        JButton exemploPadrao = new JButton("Carregar exemplo");
        exemploPadrao.addActionListener(this::carregarExemplo);
        bottomPanel.add(exemploPadrao);
        
        add(bottomPanel, BorderLayout.SOUTH);

        resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        add(new JScrollPane(resultArea), BorderLayout.EAST);
    }

    private void carregarExemplo(ActionEvent e) {
        // Define tamanho 3x3
        sizeField.setText("3");
        setMatrixSize(null);
        
        // Exemplo: 2x + 3y - z = 5
        //         4x + y + 5z = 6
        //        -2x + 5y + 3z = 28
        double[][] A = {
            { 2,  3, -1},
            { 4,  1,  5},
            {-2,  5,  3}
        };
        double[] b = {5, 6, 28};

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                coefFields[i][j].setText(String.valueOf(A[i][j]));
            }
            rhsFields[i].setText(String.valueOf(b[i]));
        }
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
            GaussEliminacao.ResultadoGauss resultado = GaussEliminacao.solve(A, b);
            
            StringBuilder sb = new StringBuilder();
            sb.append("Solução por Eliminação de Gauss:\n");
            for (int i = 0; i < n; i++) {
                sb.append("x[").append(i + 1).append("] = ").append(String.format("%.6f", resultado.solucao[i])).append("\n");
            }
            
            sb.append("\nEtapas da Eliminação:\n");
            for (int k = 0; k <= n; k++) {
                sb.append("\nEtapa ").append(k).append(":\n");
                for (int i = 0; i < n; i++) {
                    // Matriz A
                    for (int j = 0; j < n; j++) {
                        sb.append(String.format("%8.3f", resultado.etapas[k][i][j]));
                    }
                    // Vetor b
                    sb.append(" | ").append(String.format("%8.3f", resultado.vetores[k][i]));
                    sb.append("\n");
                }
            }
            
            sb.append("\nMatriz de Permutação P:\n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(String.format("%8.3f", resultado.matrizP[i][j]));
                }
                sb.append("\n");
            }
            
            sb.append("\nDecomposição LU:\n");
            sb.append("L = \n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(String.format("%8.3f", resultado.matrizL[i][j]));
                }
                sb.append("\n");
            }
            
            sb.append("\nU = \n");
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    sb.append(String.format("%8.3f", resultado.matrizU[i][j]));
                }
                sb.append("\n");
            }
            
            resultArea.setText(sb.toString());
        } catch (Exception ex) {
            resultArea.setText("Erro: " + ex.getMessage());
        }
    }

    // Método main removido pois agora usamos via MetodosCalculo
}
