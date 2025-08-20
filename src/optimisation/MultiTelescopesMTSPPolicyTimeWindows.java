package optimisation;

import astrometrics.HorizonCoordinates;
import observation.*;
import observation.Observable;
import simulation.Clock;

import java.util.*;

import gurobi.*;
import simulation.Simulation;

public class MultiTelescopesMTSPPolicyTimeWindows extends DispatchPolicy {

    // Helper class to store time window information
    private static class TimeWindow {
        public final double startTime;
        public final double endTime;
        public final double serviceTime; // pulsar integration time

        public TimeWindow(double start, double end, double service) {
            this.startTime = start;
            this.endTime = end;
            this.serviceTime = service;
        }
    }

    @Override
    public Connection[] findNextPaths(Pointable[] pointables) {
        List<Pointable> points = new ArrayList<>();
        for(int i = 0; i< Simulation.NUMTELESCOPES; i++)
            points.add(pointables[i]);

        for (Connection conn : pointables[0].getNeighbours()){
            Pointable p = conn.getOtherTarget(pointables[0]);
            points.add(p);
            System.out.print(p);
        }
        System.out.println();

        int p = points.size();
        int n = p+2;
        int m = Simulation.NUMTELESCOPES;
        Connection[] next = new Connection[m];
        // Initialize all connections to null

        double[][] cost = constructCostMatrix(points);
        TimeWindow[] timeWindows = constructTimeWindows(points); // New method

        // Debug output
        System.out.println("Cost matrix dimensions: " + cost.length + "x" + cost[0].length);
        System.out.println("Number of points: " + p + ", n=" + n + ", m=" + m);
        System.out.println("Time windows:");
        for(int i = 0; i < timeWindows.length; i++) {
            System.out.printf("Point %d: [%.2f, %.2f] service=%.2f\n",
                    i, timeWindows[i].startTime, timeWindows[i].endTime, timeWindows[i].serviceTime);
        }

        double ub = 0;
        for(int i = 0; i< n; i++)
            for(int j = 0; j<n; j++)
                ub += cost[i][j];

        try {
            // Create empty environment, set options, and start
            GRBEnv env = new GRBEnv(true);
            env.set(GRB.IntParam.LogToConsole, 0);
            env.start();

            // Create empty model
            GRBModel model = new GRBModel(env);

            // Create variables
            GRBVar[][][] x = new GRBVar[n][n][m];
            for(int k = 0; k<m; k++)
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++)
                        x[i][j][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x" + i + "," + j + "," + k);

            // Auxiliary variables for subtour elimination
            GRBVar[] u = new GRBVar[n];
            for (int i = 0; i < n; i++)
                u[i] = model.addVar(0.0, n, 0.0, GRB.INTEGER, "u" + i);

            // NEW: Time variables for each point and telescope
            GRBVar[][] t = new GRBVar[n][m];
            double maxTime = getCurrentMaxTime(); // You'll need to implement this
            for(int i = 0; i < n; i++) {
                for(int k = 0; k < m; k++) {
                    t[i][k] = model.addVar(0.0, maxTime, 0.0, GRB.CONTINUOUS, "t" + i + "," + k);
                }
            }

            GRBVar Q = model.addVar(0.0, ub, 0.0, GRB.INTEGER, "Q");

            // Set objective: minimize Q
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, Q);
            model.setObjective(expr, GRB.MINIMIZE);

            int cc = 0;

            // Original constraints (keeping your existing logic)
            // Virtual depot and starting points constraints
            for(int k = 0; k<m; k++){
                for(int i = 0; i<=m; i++) {
                    x[i][n - 1][k].set(GRB.DoubleAttr.LB, 0);
                    x[i][n-1][k].set(GRB.DoubleAttr.UB, 0);
                }
            }

            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int i = 1; i<n-1; i++)
                    expr.addTerm(1.0, x[i][n-1][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+ cc++);
            }

            for(int k = 0; k<m; k++){
                for(int i = 0; i<n; i++) {
                    x[n - 1][i][k].set(GRB.DoubleAttr.LB, 0);
                    x[n - 1][i][k].set(GRB.DoubleAttr.UB, 0);
                }
            }

            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int j = 1; j<n-1; j++)
                    expr.addTerm(1.0, x[0][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+ cc++);
            }

            for(int k = 0; k<m; k++){
                for(int j = 0; j<n; j++) {
                    x[j][0][k].set(GRB.DoubleAttr.UB, 0);
                    x[j][0][k].set(GRB.DoubleAttr.LB, 0);
                }
            }

            // Each point visited exactly once
            for(int j = 1; j<n-1; j++){
                expr = new GRBLinExpr();
                for(int k = 0; k<m; k++)
                    for(int i = 0; i<n; i++)
                        if(i != j)
                            expr.addTerm(1.0, x[i][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "c"+ cc++);
            }

            for(int i = 1; i<n-1; i++){
                expr = new GRBLinExpr();
                for(int k = 0; k<m; k++)
                    for(int j = 0; j<n; j++)
                        if(i != j)
                            expr.addTerm(1.0, x[i][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "c"+ cc++);
            }

            // Flow conservation
            for(int k = 0; k<m; k++){
                for(int j = 1; j<n-1; j++){
                    expr = new GRBLinExpr();
                    for(int i = 0; i<n; i++){
                        if(i != j){
                            expr.addTerm(1.0, x[i][j][k]);
                            expr.addTerm(-1.0, x[j][i][k]);
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, 0, "c"+ cc++);
                }
            }

            // Subtour elimination
            for(int i = 1; i<n-1; i++){
                for(int j = 1; j<n; j++){
                    if(i != j){
                        expr = new GRBLinExpr();
                        expr.addTerm(1.0, u[i]);
                        expr.addTerm(-1.0, u[j]);
                        for(int k = 0; k<m; k++){
                            expr.addTerm(n-m, x[i][j][k]);
                        }
                        model.addConstr(expr, GRB.LESS_EQUAL, n-m-1, "c"+ cc++);
                    }
                }
            }

            // Objective constraint
            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int i = 0; i<n; i++)
                    for(int j = 0; j<n; j++)
                        if(i != j)
                            expr.addTerm(cost[i][j], x[i][j][k]);
                expr.addTerm(-1, Q);
                model.addConstr(expr, GRB.LESS_EQUAL, 0, "c"+ cc++);
            }

            // NEW: TIME WINDOW CONSTRAINTS

            // 1. Initialize telescope starting times
            for(int k = 0; k < m; k++) {
                t[0][k].set(GRB.DoubleAttr.LB, getCurrentTime(k)); // Set to current time of telescope k
                t[0][k].set(GRB.DoubleAttr.UB, getCurrentTime(k));
            }

            // 2. Time progression constraints: if telescope k goes from i to j, then t[j][k] >= t[i][k] + travel_time + service_time
            for(int k = 0; k < m; k++) {
                for(int i = 0; i < n; i++) {
                    for(int j = 1; j < n; j++) { // j cannot be depot (0)
                        if(i != j) {
                            // If x[i][j][k] = 1, then t[j][k] >= t[i][k] + cost[i][j] + serviceTime[i]
                            expr = new GRBLinExpr();
                            expr.addTerm(1.0, t[j][k]);
                            expr.addTerm(-1.0, t[i][k]);
                            expr.addTerm(-cost[i][j] - getServiceTime(i, timeWindows), x[i][j][k]);
                            model.addConstr(expr, GRB.GREATER_EQUAL, -maxTime, "time_prog_" + i + "_" + j + "_" + k);

                            // Big-M constraint version:
                            // t[j][k] >= t[i][k] + (cost[i][j] + serviceTime[i]) * x[i][j][k]
                            expr = new GRBLinExpr();
                            expr.addTerm(1.0, t[j][k]);
                            expr.addTerm(-1.0, t[i][k]);
                            expr.addTerm(-(cost[i][j] + getServiceTime(i, timeWindows)), x[i][j][k]);
                            model.addConstr(expr, GRB.GREATER_EQUAL, -(cost[i][j] + getServiceTime(i, timeWindows)), "time_bigM_" + i + "_" + j + "_" + k);
                        }
                    }
                }
            }

            // 3. Time window constraints for each point
            for(int i = 1; i < n-1; i++) { // Skip depot and end point
                for(int k = 0; k < m; k++) {
                    // If telescope k visits point i, it must arrive within the time window
                    // This is enforced by setting bounds on t[i][k] when the point is visited

                    // We need to use indicator constraints or big-M
                    // If point i is visited by telescope k, then timeWindow[i].startTime <= t[i][k] <= timeWindow[i].endTime - serviceTime[i]

                    // Check if point i is visited by telescope k
                    GRBVar visited = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "visited_" + i + "_" + k);

                    expr = new GRBLinExpr();
                    for(int j = 0; j < n; j++) {
                        if(i != j) {
                            expr.addTerm(1.0, x[i][j][k]);
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, visited, "visit_def_" + i + "_" + k);

                    // If visited = 1, then startTime <= t[i][k] <= endTime - serviceTime
                    // Using big-M: t[i][k] >= startTime * visited + 0 * (1-visited)
                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, t[i][k]);
                    expr.addTerm(-timeWindows[i].startTime, visited);
                    model.addConstr(expr, GRB.GREATER_EQUAL, 0, "tw_start_" + i + "_" + k);

                    // t[i][k] <= endTime - serviceTime + maxTime * (1-visited)
                    expr = new GRBLinExpr();
                    expr.addTerm(1.0, t[i][k]);
                    expr.addTerm(maxTime, visited);
                    model.addConstr(expr, GRB.LESS_EQUAL, timeWindows[i].endTime - timeWindows[i].serviceTime + maxTime, "tw_end_" + i + "_" + k);
                }
            }

            // Optimize model
            model.optimize();

            if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
                System.out.println("The status code is:");
                System.out.println(model.get(GRB.IntAttr.Status));
            }
            if (model.get(GRB.IntAttr.SolCount) > 0) {
                int[][] tour = new int[m][];
                double[][] x_for_each_k = new double[n][n];
                boolean validSolution = true;

                for(int k = 0; k<m; k++){
                    for(int i = 0; i<n; i++)
                        for(int j = 0; j<n; j++)
                            x_for_each_k[i][j] = x[i][j][k].get(GRB.DoubleAttr.X);

                    tour[k] = findsubtour(x_for_each_k);

                    System.out.print("Tour " + k + ": ");
                    for (int j = 0; j < tour[k].length; j++) {
                        System.out.print(tour[k][j] + " ");
                    }
                    System.out.println();

                    // Print timing information
                    System.out.print("Times " + k + ": ");
                    for (int j = 0; j < tour[k].length; j++) {
                        if(tour[k][j] < n) {
                            System.out.printf("%.2f ", t[tour[k][j]][k].get(GRB.DoubleAttr.X));
                        }
                    }
                    System.out.println();

                    // Validate tour structure before processing
                    if(tour[k].length < 3) {
                        System.out.println("Warning: Tour " + k + " too short, skipping");
                        validSolution = false;
                        continue;
                    }

                    // Check if we have valid indices
                    int telescopeIndex = tour[k][1] - 1;
                    int targetIndex = tour[k][2] - m - 1;

                    if(telescopeIndex < 0 || telescopeIndex >= Simulation.NUMTELESCOPES) {
                        System.out.println("Warning: Invalid telescope index " + telescopeIndex + " in tour " + k);
                        validSolution = false;
                        continue;
                    }

                    if(tour[k][2] <= m || targetIndex < 0 || targetIndex >= pointables[telescopeIndex].getNeighbours().size()) {
                        System.out.println("Warning: Invalid target index " + targetIndex + " for telescope " + telescopeIndex + " in tour " + k);
                        System.out.println("tour[k][2]=" + tour[k][2] + ", m=" + m + ", neighbours.size()=" + pointables[telescopeIndex].getNeighbours().size());
                        validSolution = false;
                        continue;
                    }

                    try {
                        Connection conn = pointables[telescopeIndex].getNeighbours().get(targetIndex);
                        currentTelescopeStates[telescopeIndex] = telescopes[telescopeIndex].getStateForShortestSlew(
                                points.get(tour[k][2]-1).getHorizonCoordinates(
                                        telescopes[telescopeIndex].getLocation(),
                                        Clock.getScheduleClock()[telescopeIndex].getTime()));
                        next[telescopeIndex] = conn;

                        ArrayList<Pointable> buffer = new ArrayList<>();
                        for(int i = 2; i<tour[k].length-1; i++) {
                            if(tour[k][i] - 1 >= 0 && tour[k][i] - 1 < points.size()) {
                                buffer.add(points.get(tour[k][i] - 1));
                            }
                        }
                        targetBuffer.set(telescopeIndex, buffer);
                    } catch (Exception e) {
                        System.out.println("Error processing tour " + k + ": " + e.getMessage());
                        validSolution = false;
                    }
                }

                if(!validSolution) {
                    System.out.println("Warning: Solution has issues, falling back to simple assignment");
                    // Fallback: assign each telescope to its first available neighbor if possible
                    for(int k = 0; k < m; k++) {
                        if(next[k] == null && !pointables[k].getNeighbours().isEmpty()) {
                            try {
                                next[k] = pointables[k].getNeighbours().getFirst();
                                currentTelescopeStates[k] = telescopes[k].getStateForShortestSlew(
                                        next[k].getOtherTarget(pointables[k]).getHorizonCoordinates(
                                                telescopes[k].getLocation(),
                                                Clock.getScheduleClock()[k].getTime()));
                                // Empty the target buffer for this telescope
                                targetBuffer.set(k, new ArrayList<>());
                                System.out.println("Fallback: Telescope " + k + " assigned to " + next[k].getOtherTarget(pointables[k]));
                            } catch (Exception e) {
                                System.out.println("Fallback failed for telescope " + k + ": " + e.getMessage());
                            }
                        }
                    }
                }
            }else{
                System.out.print("Can't find proper solution");
                System.exit(1);
            }

            model.dispose();
            env.dispose();
            System.gc();

        }catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " + e.getMessage());
        }

        for(int i=0; i< Simulation.NUMTELESCOPES; i++){
            if(next[i] != null) {
                telescopes[i].applyNewState(currentTelescopeStates[i]);
                schedules[i].addLink(next[i], currentTelescopeStates[i]);
            } else {
                System.out.println("Warning: No connection assigned to telescope " + i);
            }
        }

        return next;
    }

    // NEW: Method to construct time windows for each point
    protected TimeWindow[] constructTimeWindows(List<Pointable> points) {
        TimeWindow[] windows = new TimeWindow[points.size() + 2];

        // Depot and end point have no time constraints
        windows[0] = new TimeWindow(0, Double.MAX_VALUE, 0);
        windows[points.size() + 1] = new TimeWindow(0, Double.MAX_VALUE, 0);

        for(int i = 0; i < points.size(); i++) {
            Pointable point = points.get(i);

            // You'll need to implement these methods based on your pulsar data structure
            double startTime = getPulsarObservationStartTime(point);
            double endTime = getPulsarObservationEndTime(point);
            double integrationTime = getPulsarIntegrationTime(point);

            windows[i + 1] = new TimeWindow(startTime, endTime, integrationTime);
        }

        return windows;
    }

    // Helper methods you'll need to implement based on your data structure
    private double getCurrentTime(int telescopeIndex) {
        // Return current time for telescope k (convert to seconds)
        return Clock.getScheduleClock()[telescopeIndex].getTime().getTimeInMillis() / 1000.0;
    }

    private double getCurrentMaxTime() {
        // Return a reasonable upper bound for time variables
        double maxTime = 0;
        for(int i = 0; i < Simulation.NUMTELESCOPES; i++) {
            maxTime = Math.max(maxTime, Clock.getScheduleClock()[i].getTime().getTimeInMillis() / 1000.0);
        }
        return maxTime + 24 * 3600; // Add 24 hours as buffer
    }

    private double getServiceTime(int pointIndex, TimeWindow[] windows) {
        if(pointIndex < windows.length) {
            return windows[pointIndex].serviceTime;
        }
        return 0;
    }

    // Methods to extract pulsar information from Pointable
    private double getPulsarObservationStartTime(Pointable pointable) {
        // If this pointable wraps a pulsar, get its observation window start time
        if (pointable instanceof Observable obs) {
            // Check if the pulsar has a "do not look until" time
            if (obs instanceof Pulsar pulsar) {
                // Check against current time to see if there's a "do not look until" constraint
                double currentTime = getCurrentTime(0);
                GregorianCalendar currentCal = new GregorianCalendar();
                currentCal.setTimeInMillis((long)(currentTime * 1000));

                if (pulsar.doNotLookYet(currentCal)) {
                    // There's a constraint, but we can't access doNotLookUntil directly
                    // We'll need to estimate or use a different approach
                    // For now, add scintillation timescale to current time as estimate
                    return currentTime + pulsar.getScintillationTimescale();
                }
                return currentTime;
            }
        }
        return getCurrentTime(0); // Default to current time
    }

    private double getPulsarObservationEndTime(Pointable pointable) {
        // For pulsars, you might want to set a reasonable end time based on:
        // 1. End of observing session
        // 2. When the pulsar becomes unobservable (elevation constraints)
        // 3. Some maximum time horizon

        // For now, set to a reasonable horizon (e.g., 8 hours from now)
        double currentTime = getCurrentTime(0);
        return currentTime + (8 * 3600); // 8 hours in seconds
    }

    private double getPulsarIntegrationTime(Pointable pointable) {
        // Extract required integration time from the Observable
        if (pointable instanceof Observable obs) {
            return obs.getRemainingIntegrationTime(); // Use remaining integration time
        }
        return 0; // Default for non-observable pointables (telescopes)
    }

    // Keep your existing methods unchanged
    protected double[][] constructCostMatrix(List<Pointable> points){
        int p = points.size();
        int n = p+2;
        int m = Simulation.NUMTELESCOPES;

        double[][] cost = new double[n][n];

        for(int j = 0; j < n; j++){
            if(j<=m){
                cost[0][j] = 0.0;
                cost[n-1][j] = 100000.0;
            }
            else{
                cost[0][j] = 100000.0;
                cost[n-1][j] = 100000.0;
            }
        }
        for(int i = 0; i < n-1; i++){
            if(i<=m){
                cost[i][0] = 0;
                cost[i][n-1] = 100000.0;
            }
            else{
                cost[i][0] = 100000.0;
                cost[i][n-1] = 100000.0;
            }
        }

        for(int i = 1; i< n-1; i++)
            for(int j = 1; j<n-1; j++){
                HorizonCoordinates current = points.get(i-1).getHorizonCoordinates(telescopes[0].getLocation(), Clock.getScheduleClock()[0].getTime());
                HorizonCoordinates next = points.get(j-1).getHorizonCoordinates(telescopes[0].getLocation(), Clock.getScheduleClock()[0].getTime());
                cost[i][j] = Telescope.calculateShortestSlewTimeBetween(current, next)/100.0;
            }

        return cost;
    }

    protected static int[] findsubtour(double[][] sol)
    {
        int n = sol.length;
        int[] tour = new int[n];
        int bestlen = 1;

        tour[0] = 0;
        for(int k = 1; k<tour.length; k++){
            for(int j = 0; j<n; j++)
                if(sol[tour[k-1]][j] > 0.5){
                    tour[k] = j;
                    break;
                }
            bestlen++;
            if(tour[k] == n-1) break;
        }

        int[] result = new int[bestlen];
        System.arraycopy(tour, 0, result, 0, bestlen);
        return result;
    }
}