package optimisation;

import astrometrics.HorizonCoordinates;
import observation.*;
import observation.interference.SkyState;
import simulation.Clock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import gurobi.*;
import simulation.Simulation;

public class MultiTelescopesMTSPPolicy extends DispatchPolicy {


    @Override
    public Connection[] findNextPaths(Pointable[] pointables) {
        List<Pointable> points = new ArrayList<Pointable>();
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

            //model.set(GRB.IntParam.NumericFocus, 1);
            //model.set(GRB.IntParam.MIPFocus, 1);
            //model.set(GRB.DoubleParam.NoRelHeurTime, 1);

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

            //virtual depot and starting points of telescopes should not go to the end directly
            for(int k = 0; k<m; k++){
                for(int i = 0; i<=m; i++) {
                    x[i][n - 1][k].set(GRB.DoubleAttr.LB, 0);
                    x[i][n-1][k].set(GRB.DoubleAttr.UB, 0);
                }
            }

            // Add constraint
            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int i = 1; i<n-1; i++)
                    expr.addTerm(1.0, x[i][n-1][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+String.valueOf(cc++));
            }
            //no out degree of the end point
            for(int k = 0; k<m; k++){
                for(int i = 0; i<n; i++) {
                    x[n - 1][i][k].set(GRB.DoubleAttr.LB, 0);
                    x[n - 1][i][k].set(GRB.DoubleAttr.UB, 0);
                }
            }


            // Add constraint
            for(int k = 0; k<m; k++){
                expr = new GRBLinExpr();
                for(int j = 1; j<n-1; j++)
                    expr.addTerm(1.0, x[0][j][k]);
                model.addConstr(expr, GRB.EQUAL, 1, "c"+String.valueOf(cc++));
            }
            // no in degree of the virtual depot
            for(int k = 0; k<m; k++){
                for(int j = 0; j<n; j++) {
                    x[j][0][k].set(GRB.DoubleAttr.UB, 0);
                    x[j][0][k].set(GRB.DoubleAttr.LB, 0);
                }
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



            if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL) {
                System.out.println("The status code is:");
                System.out.println(model.get(GRB.IntAttr.Status));
            }
            if (model.get(GRB.IntAttr.SolCount) > 0) {
                int[][] tour = new int[m][];
                double[][] x_for_each_k = new double[n][n];
                for(int k = 0; k<m; k++){
                    for(int i = 0; i<n; i++)
                        for(int j = 0; j<n; j++)
                            x_for_each_k[i][j] = x[i][j][k].get(GRB.DoubleAttr.X);

                    tour[k] = findsubtour(x_for_each_k);

                    System.out.print("Tour: ");
                    for (int j = 0; j < tour[k].length; j++) {
                        System.out.print(String.valueOf(tour[k][j]) + " ");
                    }
                    System.out.println();

                    Connection conn = pointables[tour[k][1]-1].getNeighbours().get(tour[k][2]-m-1);
                    currentTelescopeStates[tour[k][1]-1] = telescopes[tour[k][1]-1].getStateForShortestSlew(points.get(tour[k][2]-1).getHorizonCoordinates(telescopes[tour[k][1]-1].getLocation(), Clock.getScheduleClock()[tour[k][1]-1].getTime()));
                    next[tour[k][1]-1]=conn;

                    ArrayList<Pointable> buffer = new ArrayList<Pointable>();
                    for(int i = 2; i<tour[k].length-1; i++) {
                        buffer.add(points.get(tour[k][i] - 1));
                        //System.out.print(tour[k][i] - 1);
                        //System.out.print(points.get(tour[k][i] - 1));
                        //System.out.print(" ");
                    }
                    //System.out.println();
                    targetBuffer.set(tour[k][1]-1, buffer);
                }
            }else{
                System.out.print("Con't find proper solution");
                System.exit(1);
            }

            model.dispose();
            env.dispose();
            // added this here in hope to fix the JVM error
            System.gc();

        }catch (GRBException e) {
            System.out.println("Error code: " + e.getErrorCode() + ". " +
                    e.getMessage());
        }

        for(int i=0; i< Simulation.NUMTELESCOPES; i++){
            telescopes[i].applyNewState(currentTelescopeStates[i]);
            schedules[i].addLink(next[i], currentTelescopeStates[i]);
        }

        return next;
    }

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
                cost[i][j] = Telescope.calculateShortestSlewTimeBetween(current, next)/100.0;// divided by 100 in order to make the solver running faster
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

        int result[] = new int[bestlen];
        for (int i = 0; i < bestlen; i++)
            result[i] = tour[i];
        return result;
    }


}
