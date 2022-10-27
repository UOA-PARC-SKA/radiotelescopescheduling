package optimisation;

import astrometrics.HorizonCoordinates;
import observation.Pointable;
import observation.Target;
import observation.Telescope;
import simulation.Clock;
import simulation.Simulation;

import java.util.List;

public class MultiTelescopesMTSPPolicyIntegrationTime extends MultiTelescopesMTSPPolicy{
    @Override
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
                cost[i][n-1] = 100000;
            }

            else{
                cost[i][0] = 100000;
                cost[i][n-1] = 100000;
            }
        }
        for(int k = 1; k<=m; k++) // Give a much larger to the cost of the path that from start to the end directly
            cost[k][n-1]=10000000; // in case one telescope do nothing.

        for(int i = 1; i< n-1; i++)
            for(int j = 1; j<n-1; j++){
                Target target = (Target) points.get(i-1);
                cost[i][j] = target.findObservableByObservationTime().getRemainingIntegrationTime();
            }


        return cost;
    }


}
