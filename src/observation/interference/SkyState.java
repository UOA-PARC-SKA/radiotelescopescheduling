package observation.interference;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import astrometrics.HorizonCoordinates;
import ext.NoradLink;
import ext.NovasLink;
import io.NoradDataReader;
import observation.Target;
import observation.Telescope;
import observation.live.ObservationState;
import simulation.Clock;
import util.Utilities;

public class SkyState 
{
	private List<Satellite> satellites;
	private List<CelestialBody> celestialBodies;
	private NoradDataReader ndr;
	private NoradLink nl;
	private NovasLink nv;
	
	
	public SkyState(String noradFilePath)
	{
		nl = new NoradLink();
		ndr = new NoradDataReader(noradFilePath);
		ndr.findAllFilesInDirectory();
		nv = new NovasLink();
	}
	
	public void createAllBadThingsThatMove(Telescope scope)
	{
		satellites = ndr.readSatellites();
		for (Satellite satellite : satellites) 
		{
			satellite.initialise(Clock.getScheduleClock().getTime(), nl, scope);
			//finding average speeds for satellites; 60 second intervals and 30 of them
			double [] angDist = nl.getDistancesForInterval(satellite, scope, 60, 30);
			satellite.calculateSpeed(angDist);
		}
		createCelestialBodies(scope);

	}
	
	private void createCelestialBodies(Telescope scope) 
	{
		Date date = Clock.getScheduleClock().getTime().getTime();
		celestialBodies = new ArrayList<>();
		celestialBodies.add(new CelestialBody("Sun", date, nv, scope));
		celestialBodies.add(new CelestialBody("Moon", date, nv, scope));
		celestialBodies.add(new CelestialBody("Jupiter", date, nv, scope));
	}

	public List<Satellite> getSatellites()
	{
		return satellites;
	}
	
	//work out which satellites could travel as far as interfering with this observation
	public List<Satellite> getSatellitesThatMightInterfere(ObservationState state, Telescope scope, GregorianCalendar scheduledDate)
	{
		HorizonCoordinates targetHc = state.getCurrentTarget().getHorizonCoordinates(scope.getLocation(), scheduledDate);
		List<Satellite> seriouslyBad = new ArrayList<>();
		double dist;
		for (Satellite satellite : satellites) 
		{
			HorizonCoordinates satHc = satellite.getHorizonCoordinates(scheduledDate);
			dist = satHc.calculateAngularDistanceTo(targetHc);
			// 2 std devs should be a safe distance
			double distCanTravel = (satellite.getAverageSpeed() + 2*satellite.getStDevSpeed())
					* state.getCurrentObservable().getExpectedIntegrationTime();
			if(distCanTravel >= dist)
				seriouslyBad.add(satellite);

		}
		return seriouslyBad;
	}

	public List<CelestialBody> getCelestialBodies() {
		
		return this.celestialBodies;
	}

	public long willCollideWithSatelliteAt(GregorianCalendar currentTime, ObservationState state, Telescope scope, long totalObsTime)
	{
		int increment = 10; //check every ten seconds
		int total = 0;
		long actualTime = totalObsTime;
		GregorianCalendar tempDate;
		double earlierDist ;
		double dist, distCanCover;
		
		HorizonCoordinates satHc,observableHc;
		List<Satellite> allSatellites = this.getSatellites();
		for (Satellite satellite : allSatellites) 
		{
			tempDate = Utilities.cloneDate(currentTime);
			satHc = satellite.getHorizonCoordinates(tempDate);
			observableHc = state.getCurrentTarget().getHorizonCoordinates(scope.getLocation(), tempDate);
			earlierDist = dist = satHc.calculateAngularDistanceTo(observableHc);
			total = 0;
			distCanCover = satellite.getAverageSpeed()*2*actualTime;
			//if the satellite can't get anywhere near, forget it.
			if(dist >  distCanCover)
				continue;
			total += increment;
			while (total < actualTime) 
			{
				tempDate.add(GregorianCalendar.SECOND, increment);
				satHc = satellite.getHorizonCoordinates(tempDate);
				observableHc = state.getCurrentTarget().getHorizonCoordinates(scope.getLocation(), tempDate);
				dist = satHc.calculateAngularDistanceTo(observableHc);
				//if the distance is getting bigger, no need to worry about this satellite
				if(dist > earlierDist)
					break;
				
				if(dist <= Satellite.getMinAngDist()+(satellite.getStDevSpeed()))
				{
					actualTime = total;
					state.setCollisionSat(satellite);
					break;
				}
				total += increment;
				earlierDist = dist;
				
			}
		}
		
		return actualTime;
	}
	
	public long willCollideWithSatelliteAt(GregorianCalendar currentTime, Target target, Telescope scope, long totalObsTime)
	{
		int increment = 10; //check every ten seconds
		int total = 0;
		long actualTime = totalObsTime;
		GregorianCalendar tempDate;
		double earlierDist ;
		double dist, distCanCover;
		
		HorizonCoordinates satHc,observableHc;
		List<Satellite> allSatellites = this.getSatellites();
		for (Satellite satellite : allSatellites) 
		{
			tempDate = Utilities.cloneDate(currentTime);
			satHc = satellite.getHorizonCoordinates(tempDate);
			observableHc = target.getHorizonCoordinates(scope.getLocation(), tempDate);
			earlierDist = dist = satHc.calculateAngularDistanceTo(observableHc);
			total = 0;
			distCanCover = satellite.getAverageSpeed()*2*actualTime;
			//if the satellite can't get anywhere near, forget it.
			if(dist >  distCanCover)
				continue;
			total += increment;
			while (total < actualTime) 
			{
				tempDate.add(GregorianCalendar.SECOND, increment);
				satHc = satellite.getHorizonCoordinates(tempDate);
				observableHc = target.getHorizonCoordinates(scope.getLocation(), tempDate);
				dist = satHc.calculateAngularDistanceTo(observableHc);
				//if the distance is getting bigger, no need to worry about this satellite
				if(dist > earlierDist)
					break;
				
				if(dist <= Satellite.getMinAngDist()+(satellite.getStDevSpeed()))
				{
					actualTime = total;
					break;
				}
				total += increment;
				earlierDist = dist;
				
			}
		}
		
		return actualTime;
	} 

}
