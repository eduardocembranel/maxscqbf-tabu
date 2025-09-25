package problems.scqbf;

import solutions.Solution;

import java.io.IOException;

public class SCQBFInverse extends SCQBF {
    public SCQBFInverse(String filename) throws IOException {
        super(filename);
    }

    @Override
    public Double evaluateQBF() {
        return -super.evaluateQBF();
    }

    @Override
    public Double evaluateInsertionQBF(int i) {
        return -super.evaluateInsertionQBF(i);
    }

    @Override
    public Double evaluateRemovalQBF(int i, Solution<Integer> sol) {
        return -super.evaluateRemovalQBF(i, sol);
    }

    @Override
    public Double evaluateExchangeQBF(int in, int out, Solution<Integer> sol) {
        return -super.evaluateExchangeQBF(in, out, sol);
    }

    @Override
    public Double evaluateDoubleExchangeQBF(int in1, int in2, int out, Solution<Integer> sol) {
        return -super.evaluateDoubleExchangeQBF(in1, in2, out, sol);
    }
}
