package backend;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Metodo da Bisseccao com front-end Swing e avaliador de expressoes embutido.
 * Digite f(x) usando: + - * / ^ e funcoes: sin, cos, tan, sqrt, log, exp, abs.
 * Exemplos: x^2 - 4   |   x^3 - 9*x + 4   |   sin(x) - 0.5
 */
public class BisseccaoSwing extends JFrame {

    private JTextField funcField, tolField, resultadoField;

    public BisseccaoSwing() {
        setTitle("Metodo da Bisseccao");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 220);
        setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel lf = new JLabel("Funcao f(x):");
        c.gridx = 0; c.gridy = 0; c.weightx = 0; add(lf, c);

        funcField = new JTextField("x*x - 4"); // exemplo padrao
        c.gridx = 1; c.gridy = 0; c.weightx = 1; add(funcField, c);

        JLabel lt = new JLabel("Precisao:");
        c.gridx = 0; c.gridy = 1; c.weightx = 0; add(lt, c);

        tolField = new JTextField("0.001");
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

            // Avaliador de expressoes
            Expr expr = new Expr(func);

            // 1) Buscar intervalo [a,b] com mudanca de sinal (Bolzano)
            double a = 0, b = 0;
            boolean encontrado = false;
            double passo = 1.0;
            double inicio = -100, fim = 100;

            double xPrev = inicio;
            double fPrev = expr.eval(xPrev);

            for (double x = inicio + passo; x <= fim; x += passo) {
                double f = expr.eval(x);
                if (Double.isNaN(fPrev) || Double.isNaN(f)) {
                    xPrev = x; fPrev = f; continue;
                }
                if (fPrev == 0) { a = xPrev - passo; b = xPrev + passo; encontrado = true; break; }
                if (f == 0)     { a = x - passo;     b = x + passo;     encontrado = true; break; }
                if (fPrev * f < 0) {
                    a = xPrev; b = x; encontrado = true; break;
                }
                xPrev = x; fPrev = f;
            }

            if (!encontrado) {
                resultadoField.setText("Nenhum intervalo encontrado em [-100,100].");
                return;
            }

            // 2) Bisseccao
            int iter = 0, iterMax = 1000;
            double m = (a + b) / 2.0;

            while ((Math.abs(b - a) / 2.0) > tol && iter < iterMax) {
                m = (a + b) / 2.0;
                double fm = expr.eval(m);
                double fa = expr.eval(a);

                if (Double.isNaN(fm) || Double.isNaN(fa)) {
                    resultadoField.setText("Erro ao avaliar a funcao. Verifique a sintaxe.");
                    return;
                }

                if (fa * fm < 0) {
                    b = m;
                } else {
                    a = m;
                }
                iter++;
            }

            resultadoField.setText(String.format("Intervalo: [%.6f, %.6f]  Raiz ~ %.8f  (iter: %d)",
                    a, b, (a + b) / 2.0, iter));

        } catch (NumberFormatException ex) {
            resultadoField.setText("Erro: tolerancia invalida.");
        } catch (Exception ex) {
            resultadoField.setText("Erro: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BisseccaoSwing().setVisible(true));
    }

    // ---------------------- Avaliador de expressoes -----------------------
    // Parser recursivo simples: suporta + - * / ^, parenteses e funcoes basicas.
    // Uso: new Expr("x^2 - 4").eval(2.0)
    static class Expr {
        private final String s;
        private int pos = -1, ch;

        Expr(String s) {
            // normaliza: remove espacos, troca virgula por ponto
            this.s = s.replace(" ", "").replace(",", ".");
            nextChar();
        }

        double eval(double xVal) {
            pos = -1; nextChar();
            double v = parseExpression(xVal);
            if (pos < s.length()) throw new RuntimeException("Entrada invalida em: " + (char)ch);
            return v;
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

        // grammar:
        // expression = term { (+|-) term }
        // term       = factor { (*|/) factor }
        // factor     = (+|-) factor | number | x | func '(' expression ')' | '(' expression ')' | factor ^ factor
        private double parseExpression(double xVal) {
            double v = parseTerm(xVal);
            for (;;) {
                if (eat('+')) v += parseTerm(xVal);
                else if (eat('-')) v -= parseTerm(xVal);
                else return v;
            }
        }

        private double parseTerm(double xVal) {
            double v = parseFactor(xVal);
            for (;;) {
                if (eat('*')) v *= parseFactor(xVal);
                else if (eat('/')) v /= parseFactor(xVal);
                else return v;
            }
        }

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
            } else if (ch >= 'a' && ch <= 'z') { // funcao
                while (ch >= 'a' && ch <= 'z') nextChar();
                String func = s.substring(startPos, this.pos);
                if (!eat('(')) throw new RuntimeException("Falta '(' apos " + func);
                double arg = parseExpression(xVal);
                if (!eat(')')) throw new RuntimeException("Falta ')'");
                v = applyFunc(func, arg);
            } else {
                throw new RuntimeException("Token inesperado: " + (char) ch);
            }

            if (eat('^')) { // potencia
                double exp = parseFactor(xVal);
                v = Math.pow(v, exp);
            }

            return v;
        }

        private double applyFunc(String f, double a) {
            switch (f) {
                case "sin": return Math.sin(a);
                case "cos": return Math.cos(a);
                case "tan": return Math.tan(a);
                case "sqrt": return Math.sqrt(a);
                case "log": return Math.log(a);      // ln
                case "exp": return Math.exp(a);
                case "abs": return Math.abs(a);
                default: throw new RuntimeException("Funcao desconhecida: " + f);
            }
        }
    }
}
