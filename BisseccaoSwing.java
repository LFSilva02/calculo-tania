package backend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Metodo da Bisseccao com front-end Swing e avaliador de expressoes embutido.
 * Digite f(x) usando + - * / ^ e funcoes: sin, cos, tan, sqrt, log(=ln), exp, abs, log10, sign, floor, ceil.
 * Suporta multiplicacao implicita: 2x, (x+1)(x-1), 2sin(x), x(x+1) etc.
 * Constantes: pi, e.  (Trigonometria em radianos)
 * Exemplos: x^2 - 4   |   x^3 - 9*x + 4   |   sin(x) - 0.5   |   (x-2)(x+3)   |   2x + 3
 */
public class BisseccaoSwing extends JFrame {

    private JTextField funcField, tolField, resultadoField;

    public BisseccaoSwing() {
        setTitle("Metodo da Bisseccao");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(520, 240);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lf = new JLabel("Funcao f(x):");
        c.gridx = 0; c.gridy = 0; c.weightx = 0; add(lf, c);

        funcField = new JTextField("x^3+8*x^2-4*x-2"); // exemplo padrao
        c.gridx = 1; c.gridy = 0; c.weightx = 1; add(funcField, c);

        JLabel lt = new JLabel("Precisao:");
        c.gridx = 0; c.gridy = 1; c.weightx = 0; add(lt, c);

        tolField = new JTextField("0.001"); // tolerancia padrao
        c.gridx = 1; c.gridy = 1; c.weightx = 1; add(tolField, c);

        JButton calcBtn = new JButton("Calcular");
        calcBtn.addActionListener(this::calcular);
        c.gridx = 0; c.gridy = 2; c.gridwidth = 2; c.weightx = 1; add(calcBtn, c);

        resultadoField = new JTextField();
        resultadoField.setEditable(false);
        c.gridx = 0; c.gridy = 3; c.gridwidth = 2; add(resultadoField, c);

        setLocationRelativeTo(null);
    }

    private void calcular(ActionEvent e) {
        try {
            String func = funcField.getText().trim();
            double tol = Double.parseDouble(tolField.getText().trim());
            if (tol <= 0) {
                resultadoField.setText("Erro: tolerancia deve ser > 0");
                return;
            }

            Expr expr = new Expr(func);
            double inicio = -100, fim = 100;
            int iterMax = 1000;
            List<String> raizes = new ArrayList<>();

            // Passos de varredura: tenta grosso (1.0) e refina (0.1) se nada for achado
            double[] passos = {1.0, 0.1};
            for (double passo : passos) {
                escanearIntervalo(expr, inicio, fim, passo, tol, iterMax, raizes);
                if (!raizes.isEmpty()) break;
            }

            if (raizes.isEmpty()) {
                resultadoField.setText("Nenhuma raiz encontrada em [-100,100].");
            } else {
                resultadoField.setText(String.join("  |  ", raizes));
            }
        } catch (NumberFormatException ex) {
            resultadoField.setText("Erro: tolerancia invalida.");
        } catch (Exception ex) {
            resultadoField.setText("Erro: " + ex.getMessage());
        }
    }

    private void escanearIntervalo(Expr expr, double inicio, double fim, double passo,
                                   double tol, int iterMax, List<String> raizes) {
        double xPrev = inicio;
        double fPrev = expr.safeEval(xPrev);

        for (double x = inicio + passo; x <= fim + 1e-12; x += passo) {
            double f = expr.safeEval(x);

            // pular pontos invalidos
            if (Double.isNaN(fPrev) || Double.isInfinite(fPrev)) {
                xPrev = x; fPrev = f; continue;
            }
            if (Double.isNaN(f) || Double.isInfinite(f)) {
                xPrev = x; fPrev = f; continue;
            }

            // Raizes "exatas" nos pontos de grade
            if (Math.abs(fPrev) < 1e-12) {
                raizes.add(String.format("Raiz exata: x = %.10f", xPrev));
            }
            if (Math.abs(f) < 1e-12) {
                raizes.add(String.format("Raiz exata: x = %.10f", x));
            }

            // Mudanca de sinal => aplica bissecao no subintervalo [xPrev, x]
            if (fPrev * f < 0) {
                double a = xPrev, b = x;
                int iter = 0;
                double m = (a + b) / 2.0;
                double fm;

                // bissecao robusta
                while (iter < iterMax) {
                    m = (a + b) / 2.0;
                    fm = expr.safeEval(m);
                    if (Double.isNaN(fm) || Double.isInfinite(fm)) break;

                    double largura = Math.abs(b - a) / 2.0;

                    if (Math.abs(fm) <= tol || largura <= tol) {
                        raizes.add(String.format("Intervalo: [%.8f, %.8f]  Raiz ~ %.10f  (iter: %d)",
                                a, b, m, iter));
                        break;
                    }

                    double fa = expr.safeEval(a);
                    if (Double.isNaN(fa) || Double.isInfinite(fa)) break;

                    if (fa * fm < 0) {
                        b = m;
                    } else {
                        a = m;
                    }
                    iter++;
                }
            }

            xPrev = x; fPrev = f;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BisseccaoSwing().setVisible(true));
    }

    // ---------------------- Avaliador de expressoes -----------------------
    // Parser recursivo com:
    // - + - * / ^, parenteses
    // - multiplicacao implicita: ao ler dois "fatores" seguidos, interpreta como *
    // - variavel x ou X
    // - constantes: pi, e
    // - funcoes: sin, cos, tan, sqrt, log(=ln), exp, abs, log10, sign, floor, ceil
    static class Expr {
        private final String s;
        private int pos = -1, ch;

        Expr(String raw) {
            // normaliza: remove espacos, troca virgula por ponto
            this.s = raw.replace(" ", "").replace(",", ".");
            nextChar();
        }

        double eval(double xVal) {
            pos = -1; nextChar();
            double v = parseExpression(xVal);
            if (pos < s.length()) throw new RuntimeException("Entrada invalida em: '" + (char)ch + "'");
            return v;
        }

        double safeEval(double xVal) {
            try {
                double v = eval(xVal);
                if (Double.isInfinite(v) || Double.isNaN(v)) return Double.NaN;
                return v;
            } catch (RuntimeException ex) {
                return Double.NaN;
            }
        }

        private void nextChar() {
            pos++;
            ch = (pos < s.length() ? s.charAt(pos) : -1);
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) { nextChar(); return true; }
            return false;
        }

        // expression = term { (+|-) term }
        private double parseExpression(double xVal) {
            double v = parseTerm(xVal);
            for (;;) {
                if (eat('+')) v += parseTerm(xVal);
                else if (eat('-')) v -= parseTerm(xVal);
                else return v;
            }
        }

        // term = factor { (*|/|implicit*) factor }
        private double parseTerm(double xVal) {
            double v = parseFactor(xVal);
            for (;;) {
                if (eat('*')) {
                    v *= parseFactor(xVal);
                } else if (eat('/')) {
                    v /= parseFactor(xVal);
                } else if (startsFactor()) {
                    // multiplicacao implicita
                    v *= parseFactor(xVal);
                } else {
                    return v;
                }
            }
        }

        // factor = (+|-) factor | number | x | const | func '(' expression ')' | '(' expression ')' | factor ^ factor
        private double parseFactor(double xVal) {
            if (eat('+')) return parseFactor(xVal); // +unario
            if (eat('-')) return -parseFactor(xVal); // -unario

            double v;
            int startPos = this.pos;

            if (eat('(')) { // (expr)
                v = parseExpression(xVal);
                if (!eat(')')) throw new RuntimeException("Falta ')'");
            } else if ((ch >= '0' && ch <= '9') || ch == '.') { // numero
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                v = Double.parseDouble(s.substring(startPos, this.pos));
            } else if (ch == 'x' || ch == 'X') { // variavel x
                nextChar();
                v = xVal;
            } else if (isLetter(ch)) { // funcao ou constante textual
                while (isLetter(ch)) nextChar();
                String name = s.substring(startPos, this.pos);

                // se vier '(' na sequencia => funcao; senao => constante textual
                if (eat('(')) {
                    double arg = parseExpression(xVal);
                    if (!eat(')')) throw new RuntimeException("Falta ')'");
                    v = applyFunc(name, arg);
                } else {
                    v = resolveConstant(name);
                }
            } else {
                throw new RuntimeException("Token inesperado: '" + (char) ch + "'");
            }

            if (eat('^')) { // potencia (associacao a direita)
                double exp = parseFactor(xVal);
                v = Math.pow(v, exp);
            }

            return v;
        }

        private boolean startsFactor() {
            // inicio de um novo fator sem operador explicito -> multiplicacao implicita
            return ch == '(' || ch == 'x' || ch == 'X' || ch == '.' ||
                   (ch >= '0' && ch <= '9') || isLetter(ch);
        }

        private boolean isLetter(int c) {
            return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
        }

        private double resolveConstant(String name) {
            switch (name) {
                case "pi": case "PI": case "Pi": return Math.PI;
                case "e": case "E": return Math.E;
                default: throw new RuntimeException("Identificador desconhecido: " + name + " (use func(...) ou constante conhecida)");
            }
        }

        private double applyFunc(String f, double a) {
            switch (f) {
                case "sin": return Math.sin(a);
                case "cos": return Math.cos(a);
                case "tan": return Math.tan(a);
                case "sqrt": return Math.sqrt(a);
                case "log": case "ln": return Math.log(a);      // ln
                case "log10": return Math.log10(a);
                case "exp": return Math.exp(a);
                case "abs": return Math.abs(a);
                case "sign": return Math.signum(a);
                case "floor": return Math.floor(a);
                case "ceil": return Math.ceil(a);
                default: throw new RuntimeException("Funcao desconhecida: " + f);
            }
        }
    }
}
