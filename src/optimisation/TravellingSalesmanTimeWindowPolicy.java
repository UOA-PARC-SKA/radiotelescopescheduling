package optimisation;

import astrometrics.Conversions;
import astrometrics.HorizonCoordinates;
import astrometrics.Location;
import observation.*;
import simulation.Clock;
import util.Utilities;
import util.exceptions.OutOfObservablesException;

import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.Assignment;
import com.google.ortools.constraintsolver.FirstSolutionStrategy;
import com.google.ortools.constraintsolver.IntVar;
import com.google.ortools.constraintsolver.RoutingDimension;
import com.google.ortools.constraintsolver.RoutingIndexManager;
import com.google.ortools.constraintsolver.RoutingModel;
import com.google.ortools.constraintsolver.RoutingSearchParameters;
import com.google.ortools.constraintsolver.main;

import java.awt.*;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Logger;

import util.exceptions.WrongTypeException;


import java.util.logging.Logger;

public class TravellingSalesmanTimeWindowPolicy extends DispatchPolicy {

    static class DataModel {
        public final int vehicleNumber = 1;
        public final int depot = 0;
    }

    public static boolean isInstanceOf(Pointable figure, Class<?> clazz){
        boolean isInstance = clazz.isAssignableFrom(figure.getClass());
        return isInstance;
    }

    public static void print2D(long mat[][])
    {
        // Loop through all rows
        for (long[] row : mat)

            // converting each row as string
            // and then printing in a separate line
            System.out.println(Arrays.toString(row));
    }

    @Override
    public Connection findNextPath(Pointable current) {

        Loader.loadNativeLibraries();

        Clock clock = Clock.getScheduleClock();
        Location loc = telescope.getLocation();

        long maxTime = 0;
        long[][] timeWindows = new long[current.getNeighbours().size()+1][2];

        long[][] distanceMatrix = new long[current.getNeighbours().size()+1][current.getNeighbours().size()+1];
        String[] nameMatrix = new String[current.getNeighbours().size()+1];

        Connection tempConn;
        Connection tempConn1;
        Connection tempConn2;
        Pointable p1;
        Pointable p2;

        distanceMatrix[0][0] = 0;

        nameMatrix[0] = "initial";
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
            // ---------
            Observable tempOb = ((Target) p).findObservableByObservationTime();
            nameMatrix[i] = tempOb.getName();
            //nameMatrix[i] = ((Target) tempConn.getOtherTarget(current)).getName();
            //----------
            TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
//            distanceMatrix[i][0] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
//            distanceMatrix[0][i] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
            distanceMatrix[i][0] = (long) possState.getSlewTime();
            distanceMatrix[0][i] = (long) possState.getSlewTime();
        }

        for (int i = 1; i < current.getNeighbours().size()+1; i++) {

            tempConn1 = current.getNeighbours().get(i-1);

            for (int k = 1; k < current.getNeighbours().size()+1; k++) {
                if (i == k) {
                    distanceMatrix[i][i] = 0;
                }
                else {
                    tempConn2 = current.getNeighbours().get(k-1);
                    p1 = tempConn1.getOtherTarget(current);
                    p2 = tempConn2.getOtherTarget(current);

                    Observable tempOb = ((Target) p2).findObservableByObservationTime();

                    long slewTime = Telescope.calculateShortestSlewTimeBetween(p1.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()), p2.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));

//                    distanceMatrix[i][k] = slewTime + tempOb.getExpectedIntegrationTime();
                    distanceMatrix[i][k] = slewTime;
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
                maxTime = time;
            }
            j++;
        }

        long[][] scaledTimeWindows = new long[current.getNeighbours().size()+1][2];
        //long[][] scaledTimeWindows = new long[1][2];
        for (int i = 1; i < scaledTimeWindows.length; i++)
        {
            scaledTimeWindows[i][0] = 0;
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

        final DataModel data = new DataModel();

        RoutingIndexManager manager =
                new RoutingIndexManager(distanceMatrix.length, data.vehicleNumber, data.depot);

        RoutingModel routing = new RoutingModel(manager);

//        System.out.println("1111111111");
        print2D(distanceMatrix);

        final int transitCallbackIndex =
                routing.registerTransitCallback((long fromIndex, long toIndex) -> {
                    int fromNode = manager.indexToNode(fromIndex);
                    int toNode = manager.indexToNode(toIndex);
//                    if (scaledTimeWindows[toNode][1] < 0) {
//                        return Long.MAX_VALUE;
//                    }
                    return distanceMatrix[fromNode][toNode];
                });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        routing.addDimension(transitCallbackIndex, // transit callback
                Long.MAX_VALUE, // allow waiting time
                Long.MAX_VALUE, // vehicle maximum capacities
                false, // start cumul to zero
                "Time");

        long penalty = 1000;
        for (int i = 1; i < distanceMatrix.length; ++i) {
            routing.addDisjunction(new long[] {manager.nodeToIndex(i)}, penalty);
        }

        RoutingDimension timeDimension = routing.getMutableDimension("Time");

//        System.out.println("2222222222");
        // Add time window constraints for each location except depot.
        for (int i = 1; i < scaledTimeWindows.length; ++i) {
            long index = manager.nodeToIndex(i);
//            System.out.println(scaledTimeWindows[i][1]);
//            long tempTimeWindow = Math.max(0, scaledTimeWindows[i][1]);
            long tempTimeWindow = Math.abs(scaledTimeWindows[i][1]);
//            timeDimension.cumulVar(index).setRange(scaledTimeWindows[i][0], Math.abs(scaledTimeWindows[i][1]));
            timeDimension.cumulVar(index).setRange(scaledTimeWindows[i][0], tempTimeWindow);
//            try {
//                timeDimension.cumulVar(index).setRange(scaledTimeWindows[i][0], scaledTimeWindows[i][1]);
//            } catch (Exception ex) {
//                timeDimension.cumulVar(index).setRange(scaledTimeWindows[i][0], 208800);
//            }
            //System.out.println(scaledTimeWindows[i][1]);
        }
//        System.out.println("333333333");
        // Add time window constraints for each vehicle start node.
        for (int i = 0; i < data.vehicleNumber; ++i) {
            long index = routing.start(i);
            timeDimension.cumulVar(index).setRange(scaledTimeWindows[0][0], scaledTimeWindows[0][1]);
        }
//        System.out.println("4444444444444");
        // Instantiate route start and end times to produce feasible times.
        for (int i = 0; i < data.vehicleNumber; ++i) {
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.start(i)));
            routing.addVariableMinimizedByFinalizer(timeDimension.cumulVar(routing.end(i)));
        }

//        System.out.println("5555555555");

        RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .build();

//        System.out.println("55555.....5555555");
        Assignment solution = routing.solveWithParameters(searchParameters);

//        System.out.println("6666666666666");

        long index = routing.start(0);
//        System.out.println(index);
        index = solution.value(routing.nextVar(index));
        int nextIndex = manager.indexToNode(index);

        Connection next;

        Connection conn;

        if (nextIndex == 0) {
            conn = current.getNeighbours().get(0);
        } else {
            conn = current.getNeighbours().get(nextIndex-1);
        }

        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);

        return next;
    }
}