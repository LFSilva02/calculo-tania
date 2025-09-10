package backend;
import java.util.Arrays;

/**
 * Eliminacao de Gauss com Pivotamento Parcial.
 * Resolve Ax = b. Entrada: A (n x n) e b (n).
 * Saida: ResultadoGauss contendo:
 * - vetor solucao x
 * - matrizes L, U da decomposicao LU
 * - matriz P de permutacao
 * - etapas intermediarias do processo
 * - excecao se o sistema for singular
 *
 * Passos (conforme os slides):
 * 1) Formar a matriz aumentada [A|b]
 * 2) Para k=0..n-1: escolher pivô (pivotamento parcial), zerar entradas abaixo do pivô
 *    usando multiplicadores L_ik = A[i][k]/A[k][k] e operação de linha:
 *    L_i <- L_i - L_ik * L_k
 * 3) Retrosubstituição para obter x_n, x_{n-1}, ..., x_1.
 */
public class GaussEliminacao {
    // Classe para armazenar os resultados do metodo de Gauss
    public static class ResultadoGauss {
        public double[] solucao;       // vetor solucao x
        public double[][][] etapas;    // matriz A em cada etapa da eliminacao
        public double[][] vetores;     // vetor b em cada etapa
        public int[] permutacoes;      // vetor de permutacoes das linhas
        public double[][] matrizL;     // matriz L da decomposicao LU
        public double[][] matrizU;     // matriz U da decomposicao LU
        public double[][] matrizP;     // matriz de permutacao P

        public ResultadoGauss(int n, int numEtapas) {
            solucao = new double[n];
            etapas = new double[numEtapas][n][n];
            vetores = new double[numEtapas][n];
            permutacoes = new int[n];
            matrizL = new double[n][n];
            matrizU = new double[n][n];
            matrizP = new double[n][n];
            
            // Inicializa permutacoes e matriz P como identidade
            for (int i = 0; i < n; i++) {
                permutacoes[i] = i;
                matrizP[i][i] = 1.0;
            }
        }
    }

    /**
     * Resolve o sistema linear Ax=b por Eliminacao de Gauss com pivotamento parcial.
     * @param A matriz dos coeficientes (sera COPIADA; a original nao e alterada)
     * @param b vetor de termos independentes (sera COPIADO)
     * @return ResultadoGauss contendo a solucao, etapas, matrizes L, U e P
     */
    public static ResultadoGauss solve(double[][] A, double[] b) {
        int n = A.length;
        if (n == 0 || A[0].length != n || b.length != n) {
            throw new IllegalArgumentException("Dimensoes invalidas: A deve ser n x n e b tamanho n.");
        }

        // Inicializa o resultado com n+1 etapas (matriz original + n etapas de eliminacao)
        ResultadoGauss resultado = new ResultadoGauss(n, n + 1);
        
        // Copias para nao alterar os originais
        double[][] M = new double[n][n];
        double[] rhs = new double[n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, M[i], 0, n);
            rhs[i] = b[i];
        }
        
        // Salva estado inicial (etapa 0)
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                resultado.etapas[0][i][j] = M[i][j];
            }
            resultado.vetores[0][i] = rhs[i];
        }

        // Eliminacao (Triangularizacao): k = coluna/pivo
        for (int k = 0; k < n; k++) {

            // Pivotamento Parcial: escolhe a linha com maior |M[i][k]| para ser a k
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
                // Troca as linhas em M e rhs
                double[] tmp = M[k];  M[k] = M[pivo];  M[pivo] = tmp;
                double tb = rhs[k];   rhs[k] = rhs[pivo]; rhs[pivo] = tb;
                
                // Atualiza vetor de permutacoes e matriz P
                int tmpPerm = resultado.permutacoes[k];
                resultado.permutacoes[k] = resultado.permutacoes[pivo];
                resultado.permutacoes[pivo] = tmpPerm;
                
                // Troca linhas na matriz P
                double[] tmpP = resultado.matrizP[k];
                resultado.matrizP[k] = resultado.matrizP[pivo];
                resultado.matrizP[pivo] = tmpP;
            }

            // Verifica pivô nulo (sistema singular ou mal condicionado)
            double pivotValue = M[k][k];
            if (Math.abs(pivotValue) < 1e-15) {
                throw new ArithmeticException("Sistema singular ou pivo numericamente nulo na etapa k=" + k);
            }

            // Zera elementos abaixo do pivo na coluna k
            for (int i = k + 1; i < n; i++) {
                double Lik = M[i][k] / pivotValue; // multiplicador L_ik
                resultado.matrizL[i][k] = Lik;  // Guarda o multiplicador na matriz L
                
                // Linha i <- Linha i - Lik * Linha k (em M e em rhs)
                for (int j = k; j < n; j++) {
                    M[i][j] -= Lik * M[k][j];
                }
                rhs[i] -= Lik * rhs[k];
            }
            
            // Guarda o estado após esta etapa
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    resultado.etapas[k + 1][i][j] = M[i][j];
                }
                resultado.vetores[k + 1][i] = rhs[i];
            }
        }

        // Guarda a matriz U resultante
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                resultado.matrizU[i][j] = M[i][j];
            }
        }
        
        // Completa a diagonal principal de L com 1's
        for (int i = 0; i < n; i++) {
            resultado.matrizL[i][i] = 1.0;
        }

        // Retrosubstituicao
        double[] x = new double[n];
        for (int i = n - 1; i >= 0; i--) {
            double soma = rhs[i];
            for (int j = i + 1; j < n; j++) {
                soma -= M[i][j] * x[j];
            }
            double diag = M[i][i];
            if (Math.abs(diag) < 1e-15) {
                throw new ArithmeticException("Sistema singular na retrosubstituicao (diagonal ~ 0).");
            }
            x[i] = soma / diag;
        }
        
        resultado.solucao = x;
        return resultado;
    }

    // ---------------- Exemplo rápido de uso ----------------
    /**
     * Realiza a decomposicao LU de uma matriz A.
     * Retorna as matrizes L (triangular inferior) e U (triangular superior).
     * A = L * U
     * @param A matriz de entrada (n x n)
     * @return array com [L, U]
     */
    public static double[][][] decomposicaoLU(double[][] A) {
        int n = A.length;
        if (n == 0 || A[0].length != n) {
            throw new IllegalArgumentException("A matriz deve ser quadrada!");
        }

        double[][] L = new double[n][n];
        double[][] U = new double[n][n];
        
        // Inicializa L com 1's na diagonal principal
        for (int i = 0; i < n; i++) {
            L[i][i] = 1.0;
        }
        
        // Copia a matriz A para U inicialmente
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, U[i], 0, n);
        }

        // Decomposicao LU sem Pivotamento
        for (int k = 0; k < n; k++) {
            // Verifica pivô nulo
            if (Math.abs(U[k][k]) < 1e-15) {
                throw new ArithmeticException("Pivo nulo encontrado na Decomposicao LU!");
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

        ResultadoGauss resultado = solve(A, b);
        
        System.out.println("Solucao pelo metodo de Gauss:");
        System.out.println("x = " + Arrays.toString(resultado.solucao));
        
        System.out.println("\nEtapas da eliminacao:");
        for (int k = 0; k <= A.length; k++) {
            System.out.println("\nEtapa " + k + ":");
            for (int i = 0; i < A.length; i++) {
                System.out.println(Arrays.toString(resultado.etapas[k][i]) + " | " + resultado.vetores[k][i]);
            }
        }
        
        System.out.println("\nMatriz de Permutacao P:");
        for (double[] row : resultado.matrizP) {
            System.out.println(Arrays.toString(row));
        }
        
        System.out.println("\nMatriz L da decomposicao LU:");
        for (double[] row : resultado.matrizL) {
            System.out.println(Arrays.toString(row));
        }
        
        System.out.println("\nMatriz U da decomposicao LU:");
        for (double[] row : resultado.matrizU) {
            System.out.println(Arrays.toString(row));
        }
    }
}