package backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class MetodosCalculo {
    private JFrame frame;
    private JPanel cardPanel;
    private CardLayout cardLayout;

    public MetodosCalculo() {
        frame = new JFrame("Metodos de Calculo Numerico");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        // Painel superior com botões
        JPanel buttonPanel = new JPanel();
        JButton gaussButton = new JButton("Gauss e LU");
        JButton bisseccaoButton = new JButton("Bisseccao");

        buttonPanel.add(gaussButton);
        buttonPanel.add(bisseccaoButton);
        frame.add(buttonPanel, BorderLayout.NORTH);

        // Painel para os metodos
        cardPanel = new JPanel();
        cardLayout = new CardLayout();
        cardPanel.setLayout(cardLayout);

        // Criando painéis dos métodos
        JPanel gaussPanel = createGaussPanel();
        JPanel bisseccaoPanel = createBisseccaoPanel();

        cardPanel.add(gaussPanel, "GAUSS");
        cardPanel.add(bisseccaoPanel, "BISSECCAO");

        frame.add(cardPanel, BorderLayout.CENTER);

        // Ações dos botões
        gaussButton.addActionListener(e -> cardLayout.show(cardPanel, "GAUSS"));
        bisseccaoButton.addActionListener(e -> cardLayout.show(cardPanel, "BISSECCAO"));

        frame.setLocationRelativeTo(null);
    }

    private JPanel createGaussPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Método de Gauss e Decomposição LU"));

        // Painel de entrada
        JPanel inputPanel = new JPanel(new FlowLayout());
        inputPanel.add(new JLabel("Tamanho da matriz:"));
        JTextField sizeField = new JTextField("3", 5);
        inputPanel.add(sizeField);
        JButton createButton = new JButton("Criar Matriz");
        inputPanel.add(createButton);

        // Painel da matriz
        JPanel matrixPanel = new JPanel();
        matrixPanel.setLayout(new GridLayout(1, 1));

        // Painel de resultado
        JTextArea resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(matrixPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.EAST);

        createButton.addActionListener(e -> {
            try {
                int n = Integer.parseInt(sizeField.getText().trim());
                if (n < 2 || n > 10) {
                    JOptionPane.showMessageDialog(panel, "Tamanho deve ser entre 2 e 10");
                    return;
                }

                matrixPanel.removeAll();
                matrixPanel.setLayout(new GridLayout(n, n + 1, 5, 5));

                JTextField[][] A = new JTextField[n][n];
                JTextField[] b = new JTextField[n];

                // Criar campos da matriz
                for (int i = 0; i < n; i++) {
                    for (int j = 0; j < n; j++) {
                        A[i][j] = new JTextField("0", 4);
                        matrixPanel.add(A[i][j]);
                    }
                    b[i] = new JTextField("0", 4);
                    matrixPanel.add(b[i]);
                }

                // Botão resolver
                JButton solveButton = new JButton("Resolver");
                solveButton.addActionListener(ev -> {
                    try {
                        double[][] matrix = new double[n][n];
                        double[] vector = new double[n];

                        // Ler valores
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < n; j++) {
                                matrix[i][j] = Double.parseDouble(A[i][j].getText().trim());
                            }
                            vector[i] = Double.parseDouble(b[i].getText().trim());
                        }

                        // Resolver e mostrar resultados
                        GaussEliminacao.ResultadoGauss resultado = GaussEliminacao.solve(matrix, vector);

                        StringBuilder sb = new StringBuilder();
                        sb.append("Solução por Eliminação de Gauss:\n");
                        for (int i = 0; i < n; i++) {
                            sb.append(String.format("x[%d] = %.6f\n", i+1, resultado.solucao[i]));
                        }

                        sb.append("\nEtapas da Eliminação:\n");
                        for (int k = 0; k <= n; k++) {
                            sb.append("\nEtapa ").append(k).append(":\n");
                            for (int i = 0; i < n; i++) {
                                for (int j = 0; j < n; j++) {
                                    sb.append(String.format("%8.3f", resultado.etapas[k][i][j]));
                                }
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

                        sb.append("\nDecomposição LU:\nMatriz L:\n");
                        for (int i = 0; i < n; i++) {
                            for (int j = 0; j < n; j++) {
                                sb.append(String.format("%8.3f", resultado.matrizL[i][j]));
                            }
                            sb.append("\n");
                        }

                        sb.append("\nMatriz U:\n");
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
                });

                panel.add(solveButton, BorderLayout.SOUTH);
                panel.revalidate();
                panel.repaint();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Tamanho inválido");
            }
        });

        return panel;
    }

    private JPanel createBisseccaoPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Método da Bissecção"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;

        // Campos de entrada
        JLabel funcLabel = new JLabel("Função f(x):");
        JTextField funcField = new JTextField("x^2 - 4");
        
        JLabel tolLabel = new JLabel("Tolerância:");
        JTextField tolField = new JTextField("0.001");
        
        JButton calcButton = new JButton("Calcular");
        JTextArea resultArea = new JTextArea(10, 40);
        resultArea.setEditable(false);

        // Layout
        c.gridx = 0; c.gridy = 0; panel.add(funcLabel, c);
        c.gridx = 1; c.gridy = 0; c.weightx = 1; panel.add(funcField, c);
        
        c.gridx = 0; c.gridy = 1; c.weightx = 0; panel.add(tolLabel, c);
        c.gridx = 1; c.gridy = 1; c.weightx = 1; panel.add(tolField, c);
        
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; panel.add(calcButton, c);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; panel.add(new JScrollPane(resultArea), c);

        // Ação do botão calcular
        calcButton.addActionListener(e -> {
            try {
                String func = funcField.getText().trim();
                double tol = Double.parseDouble(tolField.getText().trim());
                if (tol <= 0) {
                    resultArea.setText("Erro: tolerância deve ser > 0");
                    return;
                }

                BisseccaoSwing.Expr expr = new BisseccaoSwing.Expr(func);
                double inicio = -100, fim = 100;
                int iterMax = 1000;
                java.util.List<String> raizes = new java.util.ArrayList<>();
                
                // Passos de varredura
                double[] passos = {1.0, 0.1};
                for (double passo : passos) {
                    double xPrev = inicio;
                    double fPrev = expr.safeEval(xPrev);

                    for (double x = inicio + passo; x <= fim + 1e-12; x += passo) {
                        double f = expr.safeEval(x);

                        if (Double.isNaN(fPrev) || Double.isInfinite(fPrev) ||
                            Double.isNaN(f) || Double.isInfinite(f)) {
                            xPrev = x;
                            fPrev = f;
                            continue;
                        }

                        if (Math.abs(fPrev) < 1e-12) {
                            raizes.add(String.format("Raiz exata: x = %.10f", xPrev));
                        }
                        if (Math.abs(f) < 1e-12) {
                            raizes.add(String.format("Raiz exata: x = %.10f", x));
                        }

                        if (fPrev * f < 0) {
                            double a = xPrev, b = x;
                            int iter = 0;
                            double m = (a + b) / 2.0;

                            while (iter < iterMax) {
                                m = (a + b) / 2.0;
                                double fm = expr.safeEval(m);
                                
                                if (Math.abs(fm) < tol) {
                                    raizes.add(String.format("Raiz por bissecção: x = %.10f", m));
                                    break;
                                }
                                
                                if (fm * expr.safeEval(a) < 0) {
                                    b = m;
                                } else {
                                    a = m;
                                }
                                
                                if (Math.abs(b - a) < tol) {
                                    raizes.add(String.format("Raiz por bissecção: x = %.10f", m));
                                    break;
                                }
                                
                                iter++;
                            }
                        }
                        xPrev = x;
                        fPrev = f;
                    }
                    if (!raizes.isEmpty()) break;
                }

                if (raizes.isEmpty()) {
                    resultArea.setText("Nenhuma raiz encontrada em [-100,100]");
                } else {
                    resultArea.setText(String.join("\n", raizes));
                }

            } catch (Exception ex) {
                resultArea.setText("Erro: " + ex.getMessage());
            }
        });

        return panel;
    }

    public void show() {
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MetodosCalculo().show());
    }
}
