/**
 *
 */
package metaheuristics.tabusearch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Random;

import problems.Evaluator;
import solutions.Solution;

/**
 * Abstract class for metaheuristic Tabu Search. It consider a minimization problem.
 *
 * @author ccavellucci, fusberti
 * @param <E>
 *            Generic type of the candidate to enter the solution.
 */
public abstract class AbstractTS<E> {

    /**
     * flag that indicates whether the code should print more information on
     * screen
     */
    public static int verboseLevel = 1;

    /**
     * a random number generator
     */
    static Random rng = new Random(0);

    /**
     * the objective function being optimized
     */
    protected Evaluator<E> ObjFunction;

    /**
     * the best solution cost
     */
    protected Double bestCost;

    /**
     * the incumbent solution cost
     */
    protected Double cost;

    /**
     * the best solution
     */
    protected Solution<E> bestSol;

    /**
     * the incumbent solution
     */
    protected Solution<E> sol;

    /**
     * the number of seconds allowed for the solve method
     */
    protected Integer maxTimeSeconds;

    /**
     * the tabu tenure.
     */
    protected Integer tenure;

    /**
     * the Candidate List of elements to enter the solution.
     */
    protected ArrayList<E> CL;

    /**
     * the Restricted Candidate List of elements to enter the solution.
     */
    protected ArrayList<E> RCL;

    /**
     * the Tabu List of elements to enter the solution.
     */
    protected ArrayDeque<E> TL;

    protected Boolean enableDiversification;

    protected Boolean enableIntensification;

    /**
     * the frequency each variable appeared in the solution
     */
    protected int[] varfrequency;

    /**
     * Creates the Candidate List, which is an ArrayList of candidate elements
     * that can enter a solution.
     *
     * @return The Candidate List.
     */
    public abstract ArrayList<E> makeCL();

    /**
     * Creates the Restricted Candidate List, which is an ArrayList of the best
     * candidate elements that can enter a solution.
     *
     * @return The Restricted Candidate List.
     */
    public abstract ArrayList<E> makeRCL();

    /**
     * Creates the Tabu List, which is an ArrayDeque of the Tabu
     * candidate elements. The number of iterations a candidate
     * is considered tabu is given by the Tabu Tenure {@link #tenure}
     *
     * @return The Tabu List.
     */
    public abstract ArrayDeque<E> makeTL();

    /**
     * Updates the Candidate List according to the incumbent solution
     * {@link #sol}. In other words, this method is responsible for
     * updating the costs of the candidate solution elements.
     */
    public abstract void updateCL();

    /**
     * Creates a new solution which is empty, i.e., does not contain any
     * candidate solution element.
     *
     * @return An empty solution.
     */
    public abstract Solution<E> createEmptySol();

    /**
     * The TS local search phase is responsible for repeatedly applying a
     * neighborhood operation while the solution is getting improved, i.e.,
     * until a local optimum is attained. When a local optimum is attained
     * the search continues by exploring moves which can make the current
     * solution worse. Cycling is prevented by not allowing forbidden
     * (tabu) moves that would otherwise backtrack to a previous solution.
     *
     * @return An local optimum solution.
     */
    public abstract Solution<E> neighborhoodMove();

    public abstract void updateVarFrequency();

    public abstract void diverfisyByRestart(double factor);

    public abstract Solution<E> intensify();

    /**
     * Constructor for the AbstractTS class.
     *
     * @param objFunction
     *            The objective function being minimized.
     * @param tenure
     *            The Tabu tenure parameter.
     * @param maxTimeSeconds
     *            The number of seconds allowed for the solve method
     */
    public AbstractTS(
            Evaluator<E> objFunction,
            Integer tenure,
            Integer maxTimeSeconds,
            Boolean enableDiversification,
            Boolean enableIntensification
            ) {
        this.ObjFunction = objFunction;
        this.tenure = tenure;
        this.maxTimeSeconds = maxTimeSeconds;
        this.varfrequency = new int[objFunction.getDomainSize()];
        this.enableDiversification = enableDiversification;
        this.enableIntensification = enableIntensification;
    }

    /**
     * The TS constructive heuristic, which is responsible for building a
     * feasible solution by selecting in a greedy fashion, candidate
     * elements to enter the solution.
     *
     * @return A feasible solution to the problem being minimized.
     */
    public Solution<E> constructiveHeuristic() {

        CL = makeCL();
        RCL = makeRCL();
        sol = createEmptySol();
        cost = Double.POSITIVE_INFINITY;

        /* Main loop, which repeats until the stopping criteria is reached. */
        while (!constructiveStopCriteria()) {

            Double maxCost = Double.NEGATIVE_INFINITY, minCost = Double.POSITIVE_INFINITY;
            cost = sol.cost;
            updateCL();

            /*
             * Explore all candidate elements to enter the solution, saving the
             * highest and lowest cost variation achieved by the candidates.
             */
            for (E c : CL) {
                Double deltaCost = ObjFunction.evaluateInsertionCost(c, sol);
                if (deltaCost < minCost)
                    minCost = deltaCost;
                if (deltaCost > maxCost)
                    maxCost = deltaCost;
            }

            /*
             * Among all candidates, insert into the RCL those with the highest
             * performance.
             */
            for (E c : CL) {
                Double deltaCost = ObjFunction.evaluateInsertionCost(c, sol);
                if (deltaCost <= minCost) {
                    RCL.add(c);
                }
            }

            /* Choose a candidate randomly from the RCL */
            int rndIndex = rng.nextInt(RCL.size());
            E inCand = RCL.get(rndIndex);
            CL.remove(inCand);
            sol.add(inCand);
            ObjFunction.evaluate(sol);
            RCL.clear();

        }

        return sol;
    }

    /**
     * The TS mainframe. It consists of a constructive heuristic followed by
     * a loop, in which each iteration a neighborhood move is performed on
     * the current solution. The best solution is returned as result.
     *
     * @return The best feasible solution obtained throughout all iterations.
     */
    public Solution<E> solve() {
        var start = Instant.now();

        bestSol = createEmptySol();
        var initialSolution = constructiveHeuristic();
        updateVarFrequency();

        if (ObjFunction.isFeasible(initialSolution)) {
            System.out.println("Solution from CH:");
            System.out.println("t=" + getElapsedSecs(start) + " " + initialSolution);
        }
        System.out.println("Solutions from TS:");

        bestSol = new Solution<>(sol);
        TL = makeTL();
        int lastImproveIteration = 0;
        int countDiversifications = 0;
        int it = 1;
        while (getElapsedSecs(start) < maxTimeSeconds) {

            neighborhoodMove();
            updateVarFrequency();

            if (bestSol.cost > sol.cost) {
                bestSol = new Solution<>(sol);

                if (verboseLevel == 2) {
                    System.out.printf("it=%d t=%.2f bestSol=%s\n", it, getElapsedSecs(start), bestSol);
                } else if (verboseLevel == 1) {
                    System.out.printf("it=%d t=%.2f cost=%.2f size=%d\n", it, getElapsedSecs(start), bestSol.cost, bestSol.size());
                }

                if (enableIntensification && it - lastImproveIteration > 1) {
                    var solAfterIntensify = intensify();
                    if (solAfterIntensify != null && solAfterIntensify.cost < bestSol.cost) {
                        bestSol = new Solution<>(solAfterIntensify);
                        System.out.println("improved sol after intensification:");
                        System.out.printf("it=%d t=%.2f bestSol=%s\n", it, getElapsedSecs(start), bestSol);
                    }
                }

                lastImproveIteration = it;
            }

            var iterationsSinceLastImprove = it - lastImproveIteration;

            if (enableDiversification) {
                var diversified = checkDiversificationTrigger(iterationsSinceLastImprove, countDiversifications);
                if (diversified) {
                    countDiversifications++;
                }
            }

            it++;
        }

        return bestSol;
    }

    private boolean checkDiversificationTrigger(int iterationsSinceLastImprove, int countDiversifications) {
        int[] diversifyAt = {50, 150, 500};
        double[] diversifyPercents = {0.05, 0.05, 0.1};

        if (countDiversifications >= diversifyAt.length) {
            return false;
        }

        int triggerThreshold = diversifyAt[countDiversifications];
        if (iterationsSinceLastImprove >= triggerThreshold) {
            double percent = diversifyPercents[countDiversifications];
            System.out.printf("%d iterations without improvement, diversifying...\n", iterationsSinceLastImprove);
            diverfisyByRestart(percent);
            return true;
        }
        return false;
    }

    /**
     * Stops the constructive heuristic when we have a feasible solution
     *
     * @return true if the criteria is met.
     */
    public Boolean constructiveStopCriteria() {
        return ObjFunction.isFeasible(sol);
    }

    private double getElapsedSecs(Instant start) {
        Instant end = Instant.now();
        var d = Duration.between(start, end).toMillis() / 1000.0;
        return new BigDecimal(d).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
