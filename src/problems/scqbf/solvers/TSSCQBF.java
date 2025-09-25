package problems.scqbf.solvers;

import metaheuristics.tabusearch.AbstractTS;
import problems.scqbf.SCQBFInverse;
import solutions.Solution;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class TSSCQBF extends AbstractTS<Integer> {
    private final int fake = -1;

    private boolean useBestImprove;

    public TSSCQBF(
            Integer tenure,
            Integer maxTimeSeconds,
            Boolean bestImprove,
            String filename,
            Boolean enableDiversification,
            Boolean enableIntensification
            ) throws IOException {
        super(new SCQBFInverse(filename), tenure, maxTimeSeconds, enableDiversification, enableIntensification);
        this.useBestImprove = bestImprove;
    }

    /*
        create candidate list with all subsets
    */
    @Override
    public ArrayList<Integer> makeCL() {
        ArrayList<Integer> _CL = new ArrayList<>();
        for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
            _CL.add(i);
        }
        return _CL;
    }

    /*
        create empty RCL that will be updated during construction phase
    */
    @Override
    public ArrayList<Integer> makeRCL() {
        return new ArrayList<>();
    }

    /*
        create tabu list of size 2*tenure
    */
    @Override
    public ArrayDeque<Integer> makeTL() {
        ArrayDeque<Integer> _TS = new ArrayDeque<>(2*tenure);
        for (int i = 0; i < 2*tenure; i++) {
            _TS.add(fake);
        }
        return _TS;
    }

    /*
        update CL to include only subsets that can cover uncovered variables
     */
    @Override
    public void updateCL() {
        CL = ObjFunction.candidates(sol);
    }

    @Override
    public Solution<Integer> createEmptySol() {
        Solution<Integer> sol = new Solution<>();
        sol.cost = 0.0;
        return sol;
    }

    @Override
    public Solution<Integer> neighborhoodMove() {
        if (useBestImprove) {
            return bestImprovingMove();
        } else {
            return firstImprovingMove();
        }
    }

    @Override
    public void updateVarFrequency() {
        if (enableDiversification) {
            for (int varIdx : sol) {
                varfrequency[varIdx]++;
            }
        }
    }

    @Override
    public void diverfisyByRestart(double factor) {
        sol = new Solution<>(bestSol);

        int k = (int) Math.ceil(ObjFunction.getDomainSize() * factor);

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < ObjFunction.getDomainSize(); i++) {
            indices.add(i);
        }
        indices.sort(Comparator.comparingInt(i -> varfrequency[i]));
        List<Integer> leastUsedVars = indices.subList(0, Math.min(k, indices.size()));

        sol.addAll(leastUsedVars);
    }

    @Override
    public Solution<Integer> intensify() {
        return intensificationMove();
    }

    private Solution<Integer> intensificationMove() {
        Double minDeltaCost;
        Integer bestIn1 = null, bestIn2 = null, bestOut = null;

        minDeltaCost = Double.POSITIVE_INFINITY;
        updateCL();

        for (Integer in1 : CL) {
            for (Integer in2 : CL) {
                for (Integer out : sol) {
                    Double deltaCost = ObjFunction.evaluateDoubleExchangeCost(in1, in2, out, sol);
                    //found an improving move
                    if (deltaCost < 0 && deltaCost < minDeltaCost) {
                        minDeltaCost = deltaCost;
                        bestIn1 = in1;
                        bestIn2 = in2;
                        bestOut = out;
                    }
                }
            }
        }

        //no improve move found
        if (minDeltaCost >= 0) {
            return null;
        }

        applyMoveIntensify(bestIn1, bestIn2, bestOut);

        return sol;
    }

    private Solution<Integer> bestImprovingMove() {
        Double minDeltaCost;
        Integer bestCandIn = null, bestCandOut = null;

        minDeltaCost = Double.POSITIVE_INFINITY;
        updateCL();

        // Evaluate insertions
        for (Integer candIn : CL) {
            Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, sol);
            if (!TL.contains(candIn) || sol.cost+deltaCost < bestSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = candIn;
                    bestCandOut = null;
                }
            }
        }
        // Evaluate removals
        for (Integer candOut : sol) {
            Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, sol);
            if (!TL.contains(candOut) || sol.cost+deltaCost < bestSol.cost) {
                if (deltaCost < minDeltaCost) {
                    minDeltaCost = deltaCost;
                    bestCandIn = null;
                    bestCandOut = candOut;
                }
            }
        }
        // Evaluate exchanges
        for (Integer candIn : CL) {
            for (Integer candOut : sol) {
                Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                if ((!TL.contains(candIn) && !TL.contains(candOut)) || sol.cost+deltaCost < bestSol.cost) {
                    if (deltaCost < minDeltaCost) {
                        minDeltaCost = deltaCost;
                        bestCandIn = candIn;
                        bestCandOut = candOut;
                    }
                }
            }
        }

        applyMoveStd(bestCandIn, bestCandOut);

        return sol;
    }

    private Solution<Integer> firstImprovingMove() {
        updateCL();

        Double bestDelta = Double.POSITIVE_INFINITY;
        Integer bestCandIn = null, bestCandOut = null;

        // Evaluate insertions
        for (Integer candIn : CL) {
            Double deltaCost = ObjFunction.evaluateInsertionCost(candIn, sol);
            if (!TL.contains(candIn) || sol.cost+deltaCost < bestSol.cost) {
                // if this move improves the solution
                //  then apply it
                if (deltaCost < 0) {
                    applyMoveStd(candIn, null);
                    return null;
                }

                // otherwise update the best non-improving move if necessary
                if (deltaCost < bestDelta) {
                    bestDelta = deltaCost;
                    bestCandIn = candIn;
                    bestCandOut = null;
                }
            }
        }
        // Evaluate removals
        for (Integer candOut : sol) {
            Double deltaCost = ObjFunction.evaluateRemovalCost(candOut, sol);
            if (!TL.contains(candOut) || sol.cost+deltaCost < bestSol.cost) {
                //if this move improves the solution
                if (deltaCost < 0) {
                    applyMoveStd(null, candOut);
                    return null;
                }

                // otherwise update the best non-improving move if necessary
                if (deltaCost < bestDelta) {
                    bestDelta = deltaCost;
                    bestCandIn = null;
                    bestCandOut = candOut;
                }
            }
        }
        // Evaluate exchanges
        for (Integer candIn : CL) {
            for (Integer candOut : sol) {
                Double deltaCost = ObjFunction.evaluateExchangeCost(candIn, candOut, sol);
                if ((!TL.contains(candIn) && !TL.contains(candOut)) || sol.cost+deltaCost < bestSol.cost) {
                    //if this move improves the solution
                    if (deltaCost < 0) {
                        applyMoveStd(candIn, candOut);
                        return null;
                    }

                    // otherwise update the best non-improving move if necessary
                    if (deltaCost < bestDelta) {
                        bestDelta = deltaCost;
                        bestCandIn = candIn;
                        bestCandOut = candOut;
                    }
                }
            }
        }

        //if we achieved a local optimal, then we allow a non-improving move
        applyMoveStd(bestCandIn, bestCandOut);

        return null;
    }

    private void applyMoveStd(Integer candIn, Integer candOut) {
        TL.poll();
        if (candOut != null) {
            sol.remove(candOut);
            CL.add(candOut);
            TL.add(candOut);
        } else {
            TL.add(fake);
        }
        TL.poll();
        if (candIn != null) {
            sol.add(candIn);
            CL.remove(candIn);
            TL.add(candIn);
        } else {
            TL.add(fake);
        }
        ObjFunction.evaluate(sol);
    }

    private void applyMoveIntensify(Integer in1, Integer in2, Integer out) {
        TL.poll();
        if (out != null) {
            sol.remove(out);
            CL.add(out);
            TL.add(out);
        } else {
            TL.add(fake);
        }
        TL.poll();
        if (in1 != null) {
            sol.add(in1);
            CL.remove(in1);
            TL.add(in1);
        } else {
            TL.add(fake);
        }
        TL.poll();
        if (in2 != null) {
            sol.add(in2);
            CL.remove(in2);
            TL.add(in2);
        } else {
            TL.add(fake);
        }
        ObjFunction.evaluate(sol);
    }
}
