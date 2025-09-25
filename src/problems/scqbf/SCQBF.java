package problems.scqbf;

import problems.Evaluator;
import solutions.Solution;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SCQBF implements Evaluator<Integer> {
    public final Integer size;

    public double[][] A;

    //S[i][j] = true means variable j is covered by set i
    public boolean[][] S;

    public final double[] variables;

    public SCQBF(String filename) throws IOException {
        size = readInput(filename);
        variables = new double[size];
    }

    @Override
    public Integer getDomainSize() {
        return size;
    }

    @Override
    public Double evaluate(Solution<Integer> sol) {
        setVariables(sol);
        sol.cost = evaluateQBF();

        return sol.cost;
    }

    public void setVariables(Solution<Integer> sol) {
        resetVariables();
        if (!sol.isEmpty()) {
            for (Integer elem : sol) {
                variables[elem] = 1.0;
            }
        }
    }

    public void resetVariables() {
        Arrays.fill(variables, 0.0);
    }

    public Double evaluateQBF() {
        double aux = 0, sum = 0;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                aux += variables[j] * A[i][j];
            }
            sum += aux * variables[i];
            aux = 0;
        }
        return sum;
    }

    @Override
    public Double evaluateInsertionCost(Integer elem, Solution<Integer> sol) {
        setVariables(sol);
        return evaluateInsertionQBF(elem);
    }

    public Double evaluateInsertionQBF(int i) {
        if (variables[i] == 1)
            return 0.0;

        return evaluateContributionQBF(i);
    }

    @Override
    public Double evaluateRemovalCost(Integer elem, Solution<Integer> sol) {
        setVariables(sol);
        return evaluateRemovalQBF(elem, sol);
    }

    public Double evaluateRemovalQBF(int i, Solution<Integer> sol) {
        if (variables[i] == 0)
            return 0.0;

        if (!isFeasible(sol)) {
            return Double.NEGATIVE_INFINITY;
        }

        return -evaluateContributionQBF(i);
    }

    @Override
    public Double evaluateExchangeCost(Integer elemIn, Integer elemOut, Solution<Integer> sol) {
        setVariables(sol);
        return evaluateExchangeQBF(elemIn, elemOut, sol);
    }

    public Double evaluateExchangeQBF(int in, int out, Solution<Integer> sol) {
        if (in == out)
            return 0.0;
        if (variables[in] == 1)
            return evaluateRemovalQBF(out, sol);
        if (variables[out] == 0)
            return evaluateInsertionQBF(in);

        Solution<Integer> tempSolution = new Solution<>(sol);
        tempSolution.remove(Integer.valueOf(out));
        tempSolution.add(in);
        if (!isFeasible(tempSolution)) {
            return Double.NEGATIVE_INFINITY;
        }

        double sum = 0.0;
        sum += evaluateContributionQBF(in);
        sum -= evaluateContributionQBF(out);
        sum -= (A[in][out] + A[out][in]);

        return sum;
    }

    @Override
    public Double evaluateDoubleExchangeCost(Integer in1, Integer in2, Integer out, Solution<Integer> sol) {
        setVariables(sol);
        return evaluateDoubleExchangeQBF(in1, in2, out, sol);
    }

    public Double evaluateDoubleExchangeQBF(int in1, int in2, int out, Solution<Integer> sol) {
        if (in1 == out || in2 == out || in1 == in2)
            return Double.NEGATIVE_INFINITY;
        if (variables[in1] == 1 || variables[in2] == 1)
            return Double.NEGATIVE_INFINITY;
        if (variables[out] == 0)
            return Double.NEGATIVE_INFINITY;

        Solution<Integer> tempSolution = new Solution<>(sol);
        tempSolution.remove(Integer.valueOf(out));
        tempSolution.add(in1);
        tempSolution.add(in2);

        if (!isFeasible(tempSolution)) {
            return Double.NEGATIVE_INFINITY;
        }

        double delta = 0.0;

        delta += evaluateContributionQBF(in1);
        delta += evaluateContributionQBF(in2);
        delta -= evaluateContributionQBF(out);

        delta -= (A[in1][out] + A[out][in1]);
        delta += (A[in1][in2] + A[in2][in1]);
        delta -= (A[in2][out] + A[out][in2]);

        return delta;
    }

    private Double evaluateContributionQBF(int i) {
        double sum = 0.0;

        for (int j = 0; j < size; j++) {
            if (i != j)
                sum += variables[j] * (A[i][j] + A[j][i]);
        }
        sum += A[i][i];

        return sum;
    }

    public Boolean isFeasible(Solution<Integer> sol) {
        Set<Integer> coveredVars = getCoveredVars(sol);
        return coveredVars.size() == size;
    }

    private Set<Integer> getCoveredVars(Solution<Integer> sol) {
        Set<Integer> coveredVars = new HashSet<>();

        for (Integer subsetIdx: sol) {
            for (int j = 0; j < size; j++) {
                if (S[subsetIdx][j]) {
                    coveredVars.add(j);
                }
            }
        }
        return coveredVars;
    }

    public ArrayList<Integer> candidates(Solution<Integer> sol) {
        ArrayList<Integer> cands = new ArrayList<>();

        if (isFeasible(sol)) {
            // If already feasible, any subset not in solution can be candidate
            for (int i = 0; i < size; i++) {
                if (!sol.contains(i)) {
                    cands.add(i);
                }
            }
            return cands;
        } else {
            // only consider subsets that cover uncovered variables
            var uncovered = getUncoveredVars(sol);
            for (int i = 0; i < size; i++) {
                if (!sol.contains(i) && subSetHasUncoveredVar(i, uncovered)) {
                    cands.add(i);
                }
            }
        }

        return cands;
    }

    private boolean subSetHasUncoveredVar(Integer subset, Set<Integer> uncovered) {
        for (int j = 0; j < size; j++) {
            if (S[subset][j] && uncovered.contains(j)) {
                return true;
            }
        }
        return false;
    }

    public Set<Integer> getUncoveredVars(Solution<Integer> sol) {
        Set<Integer> vars = new HashSet<>();
        for (int i = 0; i < size; i++) {
            vars.add(i);
        }

        Set<Integer> coveredVars = getCoveredVars(sol);

        vars.removeAll(coveredVars);
        return vars;
    }

    protected Integer readInput(String filename) throws IOException {
        Reader fileInst = new BufferedReader(new FileReader(filename));
        StreamTokenizer stok = new StreamTokenizer(fileInst);

        stok.nextToken();
        int _size = (int) stok.nval;

        int[] setSizes = new int[_size];
        for (int i = 0; i < _size; i++) {
            stok.nextToken();
            setSizes[i] = (int) stok.nval;
        }

        S = new boolean[_size][_size];

        for (int i = 0; i < _size; i++) {
            for (int j = 0; j < setSizes[i]; j++) {
                stok.nextToken();
                int varIdx = (int) stok.nval;
                S[i][varIdx - 1] = true;
            }
        }

        A = new double[_size][_size];
        for (int i = 0; i < _size; i++) {
            for (int j = 0; j < _size; j++) {
                if (j >= i) {
                    stok.nextToken();
                    A[i][j] = stok.nval;
                } else {
                    A[i][j] = 0.0;
                }
            }
        }

        return _size;
    }
}
