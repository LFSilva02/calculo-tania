package backend;
import java.util.Arrays;

/**
 * Metodo da Eliminacao de Gauss com pivotamento parcial.
 * Resolve Ax = b. Entrada: A (n x n) e b (n).
 * Saida: vetor x (n) ou excecao se o sistema for singular.
 *
 * Passos (conforme os slides):
 * 1) Formar a matriz aumentada [A|b]
 * 2) Para k=0..n-1: escolher pivô (pivotamento parcial), zerar entradas abaixo do pivô
 *    usando multiplicadores L_ik = A[i][k]/A[k][k] e operacao de linha:
 *    L_i <- L_i - L_ik * L_k
 * 3) Retrossubstituicao para obter x_n, x_{n-1}, ..., x_1.
 */
public class GaussEliminacao {

    /**
     * Resolve o sistema linear Ax=b por Eliminacao de Gauss com pivotamento parcial.
     * @param A matriz dos coeficientes (sera COPIADA; a original nao e alterada)
     * @param b vetor de termos independentes (sera COPIADO)
     * @return vetor solucao x
     */
    public static double[] solve(double[][] A, double[] b) {
        int n = A.length;
        if (n == 0 || A[0].length != n || b.length != n) {
            throw new IllegalArgumentException("Dimensoes invalidas: A deve ser n x n e b tamanho n.");
        }

        // Copias para nao alterar os originais
        double[][] M = new double[n][n];
        double[]   rhs = new double[n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            rhs[i] = b[i];
        }

        // ELIMINACAO (triangularizacao): k = coluna/pivo
        for (int k = 0; k < n; k++) {

            // Pivotamento parcial: escolhe a linha com maior |M[i][k]| para ser a k
            int pivo = k;
            double maxAbs = Math.abs(M[k][k]);
            for (int i = k + 1; i < n; i++) {
                double val = Math.abs(M[i][k]);
                if (val > maxAbs) {
                    maxAbs = val;
                    pivo = i;
                }
            }
            // Se necessario, troca linha k com linha pivo
            if (pivo != k) {
                double[] tmp = M[k];  M[k] = M[pivo];  M[pivo] = tmp;
                double tb = rhs[k];   rhs[k] = rhs[pivo]; rhs[pivo] = tb;
            }

            // Verifica pivô nulo (sistema singular ou mal condicionado)
            double pivotValue = M[k][k];
            if (Math.abs(pivotValue) < 1e-15) {
                throw new ArithmeticException("Sistema singular ou pivô numericamente zero na etapa k=" + k);
            }

            // Zera elementos abaixo do pivô na coluna k
            for (int i = k + 1; i < n; i++) {
                double Lik = M[i][k] / pivotValue; // multiplicador L_ik (ver slides)
                // Linha i <- Linha i - Lik * Linha k (em A e em b)
                for (int j = k; j < n; j++) {
                    M[i][j] -= Lik * M[k][j];
                }
                rhs[i] -= Lik * rhs[k];
            }
        }

        // RETROSSUBSTITUICAO
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double soma = rhs[i];
            for (int j = i + 1; j < n; j++) {
                soma -= M[i][j] * x[j];
            }
            double diag = M[i][i];
            if (Math.abs(diag) < 1e-15) {
                throw new ArithmeticException("Sistema singular na retrossubstituicao (diagonal ~ 0).");
            }
            x[i] = soma / diag;
        }
        return x;
    }

    // ---------------- Exemplo rapido de uso ----------------
    /**
     * Realiza a decomposição LU de uma matriz A.
     * Retorna as matrizes L (triangular inferior) e U (triangular superior).
     * A = L * U
     * @param A matriz de entrada (n x n)
     * @return array com [L, U]
     */
    public static double[][][] decomposicaoLU(double[][] A) {
        int n = A.length;
        if (n == 0 || A[0].length != n) {
            throw new IllegalArgumentException("Matriz deve ser quadrada");
        }

        double[][] L = new double[n][n];
        double[][] U = new double[n][n];
        
        // Inicializa L com 1's na diagonal
        for (int i = 0; i < n; i++) {
            L[i][i] = 1.0;
        }
        
        // Copia A para U inicialmente
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, U[i], 0, n);
        }

        // Decomposição LU sem pivotamento
        for (int k = 0; k < n; k++) {
            // Verifica pivô nulo
            if (Math.abs(U[k][k]) < 1e-15) {
                throw new ArithmeticException("Pivô nulo encontrado na decomposição LU");
            }

            for (int i = k + 1; i < n; i++) {
                L[i][k] = U[i][k] / U[k][k];
                for (int j = k; j < n; j++) {
                    U[i][j] = U[i][j] - L[i][k] * U[k][j];
                }
            }
        }

        return new double[][][] {L, U};
    }

    public static void main(String[] args) {
        // Exemplo:
        // 2x + 3y -  z =  5
        // 4x +  y + 5z =  6
        // -2x + 5y + 3z = 28
        double[][] A = {
                { 2,  3, -1},
                { 4,  1,  5},
                {-2,  5,  3}
        };
        double[] b = {5, 6, 28};

        double[] x = solve(A, b);
        System.out.println("Solução por Gauss:");
        System.out.println("x = " + Arrays.toString(x));

        System.out.println("\nDecomposição LU:");
        double[][][] LU = decomposicaoLU(A);
        System.out.println("L = ");
        for (double[] row : LU[0]) System.out.println(Arrays.toString(row));
        System.out.println("U = ");
        for (double[] row : LU[1]) System.out.println(Arrays.toString(row));
    }
}