package optimisation;

import astrometrics.HorizonCoordinates;
import observation.Pointable;
import observation.Position;
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

        for(int i = 1; i< n-1; i++)
            for(int j = 1; j<n-1; j++){
                if (j - 1<m) {
                    cost[i][j]=0;
                    continue;
                }
                Target target = (Target) points.get(j-1);
                HorizonCoordinates current = points.get(i-1).getHorizonCoordinates(telescopes[0].getLocation(), Clock.getScheduleClock()[0].getTime());
                HorizonCoordinates next = points.get(j-1).getHorizonCoordinates(telescopes[0].getLocation(), Clock.getScheduleClock()[0].getTime());
                double observeTime = target.findObservableByObservationTime().getRemainingIntegrationTime();
                double slewTime = Telescope.calculateShortestSlewTimeBetween(current, next);
                cost[i][j] = (observeTime+slewTime)/1000.0;
            }


        return cost;
    }


}
