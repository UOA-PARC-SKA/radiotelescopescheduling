package optimisation;

import astrometrics.Location;
import com.google.ortools.Loader;
import com.google.ortools.constraintsolver.*;
import observation.*;
import simulation.Clock;
import util.exceptions.WrongTypeException;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class TravellingSalesmanSlewTime extends DispatchPolicy {

    static class DataModel {
        public final int vehicleNumber = 1;
        public final int depot = 0;
    }

    public static void print2D(long mat[][])
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

    private static final List<RoutingModel> workaround = new ArrayList<>();

    @Override
    public Connection findNextPath(Pointable current) {

        Loader.loadNativeLibraries();

        Clock clock = Clock.getScheduleClock();
        Location loc = telescope.getLocation();

        final DataModel data = new DataModel();

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

        for (int i = 1; i < current.getNeighbours().size()+1; i++) {
            tempConn = current.getNeighbours().get(i-1);
            Pointable p = tempConn.getOtherTarget(current);
            // ---------
            Observable tempOb = ((Target) p).findObservableByObservationTime();
            nameMatrix[i] = tempOb.getName();
            //nameMatrix[i] = ((Target) tempConn.getOtherTarget(current)).getName();
            //----------
            TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
            distanceMatrix[i][0] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
            distanceMatrix[0][i] = (long) possState.getSlewTime() + tempOb.getExpectedIntegrationTime();
//            distanceMatrix[i][0] = (long) possState.getSlewTime();
//            distanceMatrix[0][i] = (long) possState.getSlewTime();

//            System.out.println("SLEW TIME:");
//            System.out.println(possState.getSlewTime());
//
//            System.out.println("INTEGRATION TIME:");
//            System.out.println(tempOb.getExpectedIntegrationTime());

        }

        for (int i = 1; i < current.getNeighbours().size()+1; i++) {

            tempConn1 = current.getNeighbours().get(i-1);

            for (int j = 1; j < current.getNeighbours().size()+1; j++) {
                if (i == j) {
                    distanceMatrix[i][i] = 0;
                }
                else {
                    tempConn2 = current.getNeighbours().get(j-1);
                    p1 = tempConn1.getOtherTarget(current);
                    p2 = tempConn2.getOtherTarget(current);

                    Observable tempOb = ((Target) p2).findObservableByObservationTime();

                    long slewTime = Telescope.calculateShortestSlewTimeBetween(p1.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()), p2.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));

                    distanceMatrix[i][j] = slewTime + tempOb.getExpectedIntegrationTime();
                }
            }

        }

        RoutingIndexManager manager =
                new RoutingIndexManager(distanceMatrix.length, data.vehicleNumber, data.depot);

        RoutingModel routing = new RoutingModel(manager);
        workaround.add(routing);

        System.out.println(Arrays.toString(nameMatrix));
        print2D(distanceMatrix);

        final int transitCallbackIndex =
                routing.registerTransitCallback((long fromIndex, long toIndex) -> {
                    int fromNode = manager.indexToNode(fromIndex);
                    int toNode = manager.indexToNode(toIndex);
                    return distanceMatrix[fromNode][toNode];
                });

        routing.setArcCostEvaluatorOfAllVehicles(transitCallbackIndex);

        RoutingSearchParameters searchParameters =
                main.defaultRoutingSearchParameters()
                        .toBuilder()
                        .setFirstSolutionStrategy(FirstSolutionStrategy.Value.PATH_CHEAPEST_ARC)
                        .build();

        Assignment solution = routing.solveWithParameters(searchParameters);

        long index = routing.start(0);
//        System.out.println("current: " + manager.indexToNode(index));
//        System.out.println("current index: " + index);

        index = solution.value(routing.nextVar(index));

//        System.out.println("next: " + manager.indexToNode(index));
//        System.out.println("next index: " + index);

        int nextIndex = manager.indexToNode(index);

        Connection next;

        Connection conn = current.getNeighbours().get(nextIndex-1);
        Pointable p = conn.getOtherTarget(current);
        TelescopeState possState = telescope.getStateForShortestSlew(p.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()));
        currentTelescopeState = possState;
        next = conn;

        telescope.applyNewState(currentTelescopeState);
        schedule.addLink(next, currentTelescopeState);

        return next;
    }
}
