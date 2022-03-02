package optimisation;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import gurobi.*;
import astrometrics.Location;
import com.google.ortools.Loader; // remove
import com.google.ortools.constraintsolver.*; // remove
import observation.*;
import simulation.Clock;
import util.Utilities;
import util.exceptions.WrongTypeException;

import java.awt.*;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;

// TO DO: Find the multiple time windows for each node and correctly allocate to pulsar replications
    // Use the nodes names matrix already generated to name the constraints and may need to get the new time windows
    // Initially do two timme windows for each pulsar...
public class MipPolicy extends DispatchPolicy {

    static class DataModel { // remove
        public final int vehicleNumber = 1;
        public final int depot = 0;
    }

    public static void print2D(long mat[][]) // remove
    {
        // Loop through all rows
        for (long[] row : mat)

            // converting each row as string
            // and then printing in a separate line
            System.out.println(Arrays.toString(row));
    }

    public static boolean isInstanceOf(Pointable figure, Class<?> clazz){
        boolean isInstance = clazz.isAssignableFrom(figure.getClass());
        return isInstance;
    }

    @Override
    public Connection findNextPath(Pointable current) {


        Loader.loadNativeLibraries();

        Clock clock = Clock.getScheduleClock();
        Location loc = telescope.getLocation();

        long maxTime = 0;
        long maxTime2 =0;
        long[][] timeWindows = new long[current.getNeighbours().size()*2 +2][2];

        long[][] distanceMatrix = new long[current.getNeighbours().size()*2+2][current.getNeighbours().size()*2+2]; // extra for current node and dummy demand
        String[] nameMatrix = new String[current.getNeighbours().size()+2];

        Connection tempConn;
        Connection tempConn1;
        Connection tempConn2;
        Pointable p1;
        Pointable p2;

        distanceMatrix[0][0] = 0;

        nameMatrix[0] = "initial";
        nameMatrix[nameMatrix.length-1] = "final";
        if (isInstanceOf(current, Target.class)) {
            Observable tempOb = ((Target) current).findObservableByObservationTime();
            nameMatrix[0] = ((Target) current).getName();
            if (tempOb != null) {
                nameMatrix[0] = tempOb.getName();
            }
        }
        for (int i = 1; i < current.getNeighbours().size() + 1; i++) {
            tempConn = current.getNeighbours().get(i-1);
            Pointable p = tempConn.getOtherTarget(current);
            Observable tempOb = ((Target) p).findObservableByObservationTime();
            nameMatrix[i] = tempOb.getName();
            TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
            distanceMatrix[i][0] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
            distanceMatrix[0][i] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
//            distanceMatrix[i][0] = (long) possState.getSlewTime();
//            distanceMatrix[0][i] = (long) possState.getSlewTime();

            // utiilise the same slew-times for replications
            distanceMatrix[i+current.getNeighbours().size()][0] = distanceMatrix[i][0];
            distanceMatrix[0][i+current.getNeighbours().size()] = distanceMatrix[0][i];
        }

        for (int i = 1; i < current.getNeighbours().size()+1; i++) {

            tempConn1 = current.getNeighbours().get(i-1);

            for (int k = 1; k < current.getNeighbours().size()+1; k++) {
                if (i == k) {
                    distanceMatrix[i][i] = 0;
                    distanceMatrix[i+current.getNeighbours().size()][i+current.getNeighbours().size()]=0;
                }
                else {
                    tempConn2 = current.getNeighbours().get(k-1);
                    p1 = tempConn1.getOtherTarget(current);
                    p2 = tempConn2.getOtherTarget(current);

                    Observable tempOb = ((Target) p2).findObservableByObservationTime();

                    long slewTime = Telescope.calculateShortestSlewTimeBetween(p1.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()), p2.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
                    distanceMatrix[i][k] = slewTime + tempOb.getExpectedIntegrationTime();

                    //utilise the same slew-times for the replications
                    distanceMatrix[i+current.getNeighbours().size()][k+current.getNeighbours().size()] = distanceMatrix[i][k];
                    distanceMatrix[i][k+current.getNeighbours().size()] = 5000; // slew between groups to max slew (abrirary large number)
                    distanceMatrix[i+current.getNeighbours().size()][k] = 5000;
//                    distanceMatrix[i][k] = slewTime;
                }
            }

        }

        timeWindows[0][0] = 0;
        timeWindows[0][1] = 201600;

        int j = 1;
        for (Connection conn : current.getNeighbours())
        {
            Target t = (Target) conn.getOtherTarget(current);
            HorizonCoordinates hc = t.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime());
            //there are generally two ways to slew to a location (one way or the other)
            //this applies the shortest
            TelescopeState possState = telescope.getStateForShortestSlew(hc);
            GregorianCalendar setTime = Utilities.cloneDate(Clock.getScheduleClock().getTime());
            int slewInSeconds = (int) possState.getSlewTime();
            setTime.add(GregorianCalendar.SECOND, slewInSeconds);
            long time = (long) Conversions.getTimeUntilObjectSetsInSeconds(telescope.getLocation(), t, setTime);

            timeWindows[j][0] = 0;
            timeWindows[j][1] = time;


            if (time > maxTime)
            {
                maxTime = time; // 2612350636
            }
            j++;
        }

        /// Create a second-time window:
        for (int i= current.getNeighbours().size()+1; i< current.getNeighbours().size()*2 +1;++i){
            // utilise the end of the origin node as wehen the next time starts
            timeWindows[i][0] = maxTime;
            long time = maxTime + timeWindows[i - current.getNeighbours().size()][1];
            timeWindows[i][1] = time;

            if (time > maxTime2) {
                maxTime2 = time;
            }
        }

        timeWindows[timeWindows.length-1][0] = 0;
        timeWindows[timeWindows.length-1][1] = maxTime2;


        // Adapt this loop to go up throguh all replciations
        long[][] scaledTimeWindows = new long[current.getNeighbours().size()*2+2][2];
        //long[][] scaledTimeWindows = new long[1][2];
        for (int i = 1; i < scaledTimeWindows.length; i++)
        {
            scaledTimeWindows[i][0] = timeWindows[i][0];
            scaledTimeWindows[i][1] = timeWindows[i][1];
            //scaledTimeWindows[i][1] = 30*(timeWindows[i][1]/maxTime);
//            if (timeWindows[i][1] > 60000) {
//                scaledTimeWindows[i][1] = 60000;
//            } else {
//                scaledTimeWindows[i][1] = timeWindows[i][1];
//                //scaledTimeWindows[i][1] = 60000;
//            }
//            //scaledTimeWindows[i][1] = 30;
        }


        final TravellingSalesmanTimeWindowPolicy.DataModel data = new TravellingSalesmanTimeWindowPolicy.DataModel();
        RoutingIndexManager manager =
                new RoutingIndexManager(distanceMatrix.length, data.vehicleNumber, data.depot);

        long penalty = 100000;


        // RUN MIP MODEL
        int index = 0;
        try{
            // Initilise nodes
            int nNodes = distanceMatrix.length;

            // Initlise pulsars
            int nPulsars = distanceMatrix.length/2 +1;
            int P[][] = new int[nPulsars][]; /// ------------ currently set to be each node is a unique pulsar
            P[0] = new int[] {0};
            P[nPulsars-1] =new int [] {nNodes-1};
            for (int i=1; i<nPulsars-1; ++i) {
                P[i] = new int[] {i, i+(distanceMatrix.length/2)-1};
            }

            // Create time zones
            long temp [] = new long[2*nNodes];
            for (int i =0; i<nNodes;++i) {
                int idx = i*2;
                temp[idx] = scaledTimeWindows[i][0];
                temp[idx+1] = scaledTimeWindows[i][1]; // add all elements
            }
            Arrays.sort(temp); // sort and remove duplicates
            long[] temp2 = new long[temp.length];
            int tidx=0;
            for (int i=0; i<temp.length -1; ++i) {
                if (temp[i] != temp[i + 1]) {
                    temp2[tidx++] = temp[i];
                }
            }
            temp2[tidx++] = temp[temp.length -1];
            int tlen = temp2.length;
            while ((temp2[tlen-1] == 0) && (tlen>1)) {
                --tlen;
            }
            long TW [] = new long[tlen];
            for (int i =1; i< tlen; ++i){
                TW[i] = temp2[i];
            }

            int nTimes = TW.length ;

            // Create environment
            GRBEnv env = new GRBEnv();
//            env.set(GRB.IntParam.OutputFlag, 0);
            env.set(GRB.StringParam.LogFile,"");
            env.set(GRB.IntParam.LogToConsole, 0);

            // Create initial model
            GRBModel model = new GRBModel(env);
            model.set(GRB.StringAttr.ModelName, "MipPolicy");

            // Initialize flow variable
            GRBVar[][][] x = new GRBVar[nNodes][nNodes][nTimes];
            for (int i = 0; i < nNodes; ++i) {
                for (int h = 0; h < nNodes; ++h) {
                    for (int k=0; k< nTimes; ++k){
                        x[i][h][k] =
                                model.addVar(0, 1,0, GRB.BINARY,
                                        String.valueOf(i)+ "." + String.valueOf(h) + "." + String.valueOf(k));
                    }
                }
            }

            // Time that each node is visited at
            GRBVar[] t = new GRBVar[nNodes];
            for (int i = 0; i < nNodes; ++i) {
                t[i] =
                        model.addVar(0, GRB.INFINITY, 0, GRB.CONTINUOUS,
                                String.valueOf(i) + "Time");
            }

            // Time is gone from i to j in window k
            GRBVar tijk[][][] = new GRBVar[nNodes][nNodes][nTimes];
            for (int i = 0; i < nNodes; ++i) {
                for (int h = 0; h < nNodes; ++h) {
                    for (int k=0; k< nTimes; ++k){
                        tijk[i][h][k] =
                                model.addVar(0, GRB.INFINITY,0, GRB.CONTINUOUS,
                                        "tijk" + String.valueOf(i)+ "." + String.valueOf(h) + "." + String.valueOf(k) );
                    }
                }
            }

            // if node is visited
            GRBVar[] y = new GRBVar[nNodes];
            for (int i = 0; i < nNodes; ++i) {
                y[i] = model.addVar(0, 1, 0, GRB.BINARY,
                        String.valueOf(i) + "Visited");
            }

            GRBLinExpr lhs; // initilise
            GRBLinExpr rhs; // initilise


            // Constraint: Start at origin
            lhs = new GRBLinExpr();
            for (int h=1; h<nNodes; ++h){
                for (int k=0; k< nTimes; ++k){
                    lhs.addTerm(1.0, x[0][h][k]);
                }
            }
            model.addConstr(lhs, GRB.EQUAL, 1.0, "Origin");

            // Constraint: End at destination
            lhs = new GRBLinExpr();
            for (int i=0; i<nNodes; ++i){
                for (int k=0; k< nTimes; ++k){
                    lhs.addTerm(1, x[i][nNodes-1][k]);
                }
            }
            model.addConstr(lhs, GRB.EQUAL, 1.0, "Destination");


            // Constraint: Track which nodes are visited
            for (int h=1; h<nNodes;++h){  // for each node
                lhs = new GRBLinExpr();
                for (int i=0; i < (nNodes-1);++i){
                    for (int k=0; k<nTimes ;++k){
                        lhs.addTerm(1, x[i][h][k]);
                    }
                }
                model.addConstr(y[h], GRB.EQUAL, lhs, String.valueOf(h) + "Visit");
            }

            // Constraint: Flow conservation (excluding origin and desinaiton)
            for (int l=1; l<(nNodes-1); ++l) {
                lhs = new GRBLinExpr();
                rhs = new GRBLinExpr();

                for (int i = 0; i < (nNodes -1); ++i) {
                    for (int k = 0; k < nTimes; ++k) {
                        lhs.addTerm(1, x[i][l][k]);
                    }
                }

                for (int h = 1; h < nNodes; ++h) {
                    for (int k = 0; k < nTimes; ++k) {
                        rhs.addTerm(1.0, x[l][h][k]);
                    }
                }
                model.addConstr(lhs, GRB.EQUAL, rhs, String.valueOf(l) + "Conservation");
            }

            // Constraint: flow i to i =0
            for (int i=0; i<nNodes; ++i){
                lhs = new GRBLinExpr();

                for ( int k=0; k<nTimes;++k){
                    lhs.addTerm(1.0, x[i][i][k]);
                }
                model.addConstr(lhs, GRB.EQUAL, 0, "Flowitoi" + String.valueOf(i));
            }

            // Constraint: Time formulation
            for (int h=1; h< nNodes; ++h){
                for (int i=0; i<(nNodes-1); ++i){
                    rhs = new GRBLinExpr();

                    for (int k=0; k< nTimes; ++k){
                        rhs.addTerm(1.0, tijk[i][h][k]);
                        rhs.addTerm(distanceMatrix[i][h], x[i][h][k]);
                    }

                    model.addConstr(t[h], GRB.GREATER_EQUAL, rhs,
                            String.valueOf(h)+ "TimeForm"+ String.valueOf(i) );
                }
            }

            // Constraint: Time calculation
            for (int i=0; i< (nNodes-1);++i){
                lhs = new GRBLinExpr();
                for (int h=1; h<nNodes;++h){
                    for (int k=0; k< nTimes; ++k){
                        lhs.addTerm(1.0, tijk[i][h][k]);
                    }
                }
                model.addConstr(t[i], GRB.EQUAL, lhs, String.valueOf(i) +"TimeCalc");
            }

            // Constraint: Time window - lower & upper
            for (int i=0; i< nNodes ;++i){
                lhs = new GRBLinExpr();
                rhs = new GRBLinExpr();
                lhs.addTerm(scaledTimeWindows[i][0], y[i]);
                rhs.addTerm(scaledTimeWindows[i][1], y[i]);

                model.addConstr(t[i], GRB.LESS_EQUAL, rhs, "TW-upper" + String.valueOf(i));
                model.addConstr(lhs, GRB.LESS_EQUAL, t[i], "TW-lower" + String.valueOf(i));
            }

            // Constraint: Time zone - lower & upper
            for (int i=0; i<nNodes;++i){
                for (int h=1; h<nNodes;++h){
                    for (int k=0;k<nTimes-1 ;++k) {
                        lhs = new GRBLinExpr();
                        rhs = new GRBLinExpr();

                        lhs.addTerm(TW[k], x[i][h][k]);
                        rhs.addTerm(TW[k+1], x[i][h][k]);

                        model.addConstr(tijk[i][h][k], GRB.LESS_EQUAL, rhs,
                                String.valueOf(i)+"TZ" + String.valueOf(k) + "upper" + String.valueOf(h));
                        model.addConstr(lhs, GRB.LESS_EQUAL, tijk[i][h][k],
                                String.valueOf(i)+ "TZ" + String.valueOf(k)+ "lower" + String.valueOf(h));

                    }
                }
            }

            // Constraint: Cant visit the same pulsar multiple times
            for (int p=0; p<nPulsars;++p){
                lhs = new GRBLinExpr();
                for (int i=0; i<P[p].length; ++i){
                    lhs.addTerm(1.0, y[P[p][i]]);
                }
                model.addConstr(lhs, GRB.LESS_EQUAL,1.0, "Pulsar"+p);
            }

            // Set Objective
            GRBLinExpr obj = new GRBLinExpr();
            // finishing time minus start time
            obj.addTerm(1.0, t[nNodes-1]);
            obj.addTerm(-1.0, t[0]);

            // minus reward for each node visited
            for (int i=0; i<nNodes ;++i){
                obj.addTerm(-penalty,y[i]);
            }

            model.setObjective(obj, GRB.MINIMIZE);//
            // save
            model.write("MipPolicy.lp");

            model.optimize();

            // if not at optimality - return
            if (model.get(GRB.IntAttr.Status) != GRB.Status.OPTIMAL ) {
                System.out.println("Optimization was stopped with status " + model.get(GRB.IntAttr.Status));
                index=0;
            }

            // Extract results
            int currentIndex = 0; // index of the current node in the model
            for (int h=0; h<nNodes-1; ++h){ // not feasible to be dummy node
                for (int k=0; k< nTimes;++k) {
                    if (x[currentIndex][h][k].get(GRB.DoubleAttr.X) == 1) {
                        index = h;
                        System.out.println(index);
                    }
                }
            }

            // Dispose of model and environment
            model.dispose();
            env.dispose();

        } catch (GRBException e){
                System.out.println("Error code: " + e.getErrorCode() + ". " +
                        e.getMessage());
        }

        int nextIndex = manager.indexToNode(index);

        Connection next;
        Connection conn;

        if (nextIndex == 0) {
            conn = current.getNeighbours().get(0);
        } else {
            System.out.println(current.getNeighbours().size());
            if (nextIndex > current.getNeighbours().size()){
                conn = current.getNeighbours().get(nextIndex- current.getNeighbours().size()-1);
            } else{
                conn = current.getNeighbours().get(nextIndex-1);
            }
        }

        // current: A, conn: A->B, p: B
        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);

        return next;
    }
}
