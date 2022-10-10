package optimisation;

import astrometrics.HorizonCoordinates;
import observation.*;
import observation.interference.SkyState;
import simulation.Clock;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import gurobi.*;

public class MultiTelescopesMTSPPolicy extends DispatchPolicy {

    @Override
    public void initialise(Properties props, Telescope scope, Telescope scope1, Schedule s, Schedule s1, List<Target> targets, SkyState skyState) {
        super.initialise(props, scope, s, targets, skyState);
        telescope1 = scope1;
        schedule1 = s1;
    }

    @Override
    public Connection[] findNext2Path(Pointable pointable, Pointable pointable1) {
        List<Pointable> points = new ArrayList<Pointable>();
        points.add(pointable);
        points.add(pointable1);

        for (Connection conn : pointable.getNeighbours()){
            Pointable p = conn.getOtherTarget(pointable);
            points.add(p);
        }

        int p = points.size();
        int n = p+2;
        int m = 2;
        Connection[] next = new Connection[m];


        double[][] cost = constructCostMatrix(points);

        double ub = 0;
        for(int i = 0; i< n; i++)
            for(int j = 0; j<n; j++)
                ub += cost[i][j];


        try {
            // Create empty environment, set options, and start
            GRBEnv env = new GRBEnv(true);
            //env.set("logFile", "mip1.log");
            env.set(GRB.IntParam.LogToConsole, 0);
            env.start();

            // Create empty model
            GRBModel model = new GRBModel(env);

            // Create variables
            GRBVar[][][] x = new GRBVar[n][n][m];

            for(int k = 0; k<m; k++)
                for (int i = 0; i < n; i++)
                    for (int j = 0; j < n; j++)
                        //if (i != j)
                        x[i][j][k] = model.addVar(0.0, 1.0, 0.0, GRB.BINARY, "x" + String.valueOf(i) + "," + String.valueOf(j) + "," + String.valueOf(k));

            //Crate auxiliary variables
            GRBVar[] u = new GRBVar[n];
            for (int i = 0; i < n; i++)
                u[i] = model.addVar(0.0, n, 0.0, GRB.INTEGER, "u" + String.valueOf(i));


            GRBVar Q = model.addVar(0.0, ub, 0.0, GRB.INTEGER, "Q");



            // Set objective: minimize Q
            GRBLinExpr expr = new GRBLinExpr();
            expr.addTerm(1.0, Q);
            model.setObjective(expr, GRB.MINIMIZE);


            int cc = 0;
            // Add constraint
            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int i = 1; i<n-1; i++)
                    expr.addTerm(1.0, x[i][n-1][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+String.valueOf(cc++));
            }


            // Add constraint
            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int j = 1; j<n-1; j++)
                    expr.addTerm(1.0, x[0][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+String.valueOf(cc++));
            }

            // Add constraint
            for(int j = 1; j<n-1; j++){
                expr = new GRBLinExpr();
                for(int k = 0; k<m; k++)
                    for(int i = 0; i<n; i++)
                        if(i != j)
                            expr.addTerm(1.0, x[i][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "c"+String.valueOf(cc++));
            }

            // Add constraint
            for(int i = 1; i<n-1; i++){
                expr = new GRBLinExpr();
                for(int k = 0; k<m; k++)
                    for(int j = 0; j<n; j++)
                        if(i != j)
                            expr.addTerm(1.0, x[i][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1.0, "c"+String.valueOf(cc++));
            }


            for(int k = 0; k<m; k++){
                for(int j = 1; j<n-1; j++){
                    expr = new GRBLinExpr();
                    for(int i = 0; i<n; i++){
                        if(i != j){
                            expr.addTerm(1.0, x[i][j][k]);
                            expr.addTerm(-1.0, x[j][i][k]);
                        }
                    }
                    model.addConstr(expr, GRB.EQUAL, 0, "c"+String.valueOf(cc++));
                }
            }


            // Add constraint
            for(int i = 1; i<n-1; i++){
                for(int j = 1; j<n; j++){
                    if(i != j){
                        expr = new GRBLinExpr();
                        expr.addTerm(1.0, u[i]);
                        expr.addTerm(-1.0, u[j]);
                        for(int k = 0; k<m; k++){
                            expr.addTerm(n-m, x[i][j][k]);
                        }
                        model.addConstr(expr, GRB.LESS_EQUAL, n-m-1, "c"+String.valueOf(cc++));
                    }
                }

            }


            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int i = 0; i<n; i++)
                    for(int j = 0; j<n; j++)
                        if(i != j)
                            expr.addTerm(cost[i][j], x[i][j][k]);
                expr.addTerm(-1, Q);
                model.addConstr(expr, GRB.LESS_EQUAL, 0, "c"+String.valueOf(cc++));
            }



            // Optimize model
            model.optimize();



            if (model.get(GRB.IntAttr.SolCount) > 0) {
                int[][] tour = new int[m][];
                double[][] x_for_each_k = new double[n][n];
                for(int k = 0; k<m; k++){
                    for(int i = 0; i<n; i++)
                        for(int j = 0; j<n; j++)
                            x_for_each_k[i][j] = x[i][j][k].get(GRB.DoubleAttr.X);

                    tour[k] = findsubtour(x_for_each_k);

                    System.out.print("Tour: ");
                    for (int j = 0; j < tour[k].length; j++)
                        System.out.print(String.valueOf(tour[k][j]) + " ");
                    System.out.println();



                    switch (tour[k][1]){
                        case 1:
                            Connection conn = pointable.getNeighbours().get(tour[k][2]-m-1);
                            currentTelescopeState = telescope.getStateForShortestSlew(points.get(tour[k][2]-1).getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
                            next[0]=conn;
                            break;
                        case 2:
                            Connection conn1 = pointable1.getNeighbours().get(tour[k][2]-m-1);
                            currentTelescopeState1 = telescope1.getStateForShortestSlew(points.get(tour[k][2]-1).getHorizonCoordinates(telescope1.getLocation(), Clock.getScheduleClock().getTime()));
                            next[1]=conn1;
                    }


                }
            }else{
                System.out.print("Con't find proper solution");
                System.exit(1);
            }

        }catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }


        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next[0], currentTelescopeState);

        telescope1.applyNewState(currentTelescopeState1);
        schedule1.addLink(next[1], currentTelescopeState1);

        return next;
    }

    private double[][] constructCostMatrix(List<Pointable> points){
        int p = points.size();
        int n = p+2;
        int m = 2;

        double[][] cost = new double[n][n];

        for(int j = 0; j < n; j++){
            if(j<=m){
                cost[0][j] = 0.0;
                cost[n-1][j] = 10000.0;
            }
            else{
                cost[0][j] = 10000.0;
                cost[n-1][j] = 10000.0;
            }
        }
        for(int i = 0; i < n-1; i++){
            if(i<=m){
                cost[i][0] = 0;
                cost[i][n-1] = 10000;
            }

            else{
                cost[i][0] = 10000;
                cost[i][n-1] = 10000;
            }
        }
        for(int k = 1; k<=m; k++) // Give a much larger to the cost of the path that from start to the end directly
            cost[k][n-1]=1000000; // in case one telescope do nothing.

        for(int i = 1; i< n-1; i++)
            for(int j = 1; j<n-1; j++){
                HorizonCoordinates current = points.get(i-1).getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
                HorizonCoordinates next = points.get(j-1).getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
                cost[i][j] = Telescope.calculateShortestSlewTimeBetween(current, next);
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
                if(sol[tour[k-1]][j] == 1){
                    tour[k] = j;
                    break;
                }
            bestlen++;
            if(tour[k] == n-1) break;
        }

        int result[] = new int[bestlen];
        for (int i = 0; i < bestlen; i++)
            result[i] = tour[i];
        return result;
    }




    @Override
    public Connection findNextPath(Pointable pointable){ return null;}
}
