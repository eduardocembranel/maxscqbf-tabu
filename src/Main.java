import problems.scqbf.solvers.TSSCQBF;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Wrong number of arguments, should provide the instanceName and methodName, e.g: exact_n25 std");
            return;
        }
        System.out.println("Press ctrl+c to cancel the execution and see the result so far in the output file");

        var instance = args[0];
        var method = args[1];
        var fileName = "instances/" + instance + ".txt";

        int t1 = 20, t2 = 5;
        int maxTimeSecs = 1800;

        var stdOut = System.out;

        //redirect the output to a file
        var outputPath = "results/" + method + "/" + instance + ".txt";
        PrintStream out = new PrintStream(new FileOutputStream(outputPath, false)); // true = append
        System.setOut(out);

        try {
            if (method.equals("std")) {
                printHeader(fileName, method);
                var solver = new TSSCQBF(t1, maxTimeSecs, false, fileName, false, false);
                solver.solve();
            } else if (method.equals("std+t2")) {
                printHeader(fileName, method);
                var solver = new TSSCQBF(t2, maxTimeSecs, false, fileName, false, false);
                solver.solve();
            } else if (method.equals("std+best")) {
                printHeader(fileName, method);
                var solver = new TSSCQBF(t1, maxTimeSecs, true, fileName, false, false);
                solver.solve();
            } else if (method.equals("std+div")) {
                printHeader(fileName, method);
                var solver = new TSSCQBF(t1, maxTimeSecs, false, fileName, true, false);
                solver.solve();
            } else if (method.equals("std+int")) {
                printHeader(fileName, method);
                var solver = new TSSCQBF(t1, maxTimeSecs, false, fileName, false, true);
                solver.solve();
            }
        } catch (FileNotFoundException e) {
            System.setOut(stdOut);
            System.out.println("Wrong instance name");
        }
    }

    private static void printHeader(String instance, String method) {
        System.out.printf("instance=%s method=%s\n", instance, method);
    }
}