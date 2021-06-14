package optimisation.triangulations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;


import astrometrics.Location;
import observation.Connection;
import observation.Target;
import util.exceptions.WrongTypeException;


//This class prepares the data set based on all static constraints.
//This should then be added to a database and the dynamic optimisation should pick it up from there.

public class NNPreoptimisation extends NNOptimisation
{
	//they are helper objects, only here to avoid recreating them
	HashMap<Double, Target> hm;
	ArrayList<Double> sortedDist ;

	public NNPreoptimisation()
	{
		hm = new HashMap<Double, Target>();
		sortedDist = new ArrayList<Double>();

	}
	// the ratio dictates the cutoff when an object is a neighbour of another 
	// It is considered a neighbour if there is no other neighbour on a reasonably straight line between them
	// The reasonableness is captured by the ratio



	public void createLinksByTriangles(List<Target> targets, double ratio, Location loc, GregorianCalendar gc)
	{
		// exclude everything that's not within 90 degrees
		double cutoff = Math.PI/2;
		
		Target target2;
		
		for (int i = 0; i < targets.size(); i++) 
		{
			Target target1 = targets.get(i);
//			if(!target1.getName().equals("J0134-2937"))
//				continue;
			sortedDist.clear();
			hm.clear();
			
			for (int j = 0; j < targets.size(); j++) 
			{
				target2 = targets.get(j);
				if (target1 == target2)
					continue;
				double dist = 0;
				try {
					dist = target1.angularDistanceTo(target2, loc, gc);
				} catch (WrongTypeException e) {
					// means we have to do this the other way - one of them is a position object
					try {
						dist = target2.angularDistanceTo(target1, loc, gc);
					} catch (WrongTypeException e1) {
						e1.printStackTrace();
					}
				}
				if(dist > cutoff)
					continue;
				hm.put(dist, target2);
				sortedDist.add(dist);

			}
			Collections.sort(sortedDist);

			//make connection to closest
			Connection c = new Connection(target1, hm.get(sortedDist.get(0)), sortedDist.get(0));
			target1.addNeighbour(c);
			
			for (int j = 1; j < sortedDist.size(); j++) 
			{
				target2 = hm.get(sortedDist.get(j));
				workOutTriangle(target1, target2, sortedDist.get(j), ratio, loc, gc);
				
			}
		//	System.out.println("Pulsar "+ target1.getName()+" neighbour list "+target1.getNeighbours().size());
			
		}
	}




	
//	private void workOutTriangle(Pulsar pulsar1, Pulsar candidate, double distance, double ratio) 
//	{
//		//0 = mainToClosest, 1 = closestToCandidate, 2 = candidateToMain;
//		double[] dists = new double[3];
//		double dist;
//		double maxDist;
//		int largest = -1;
//		System.out.println("Investigating "+candidate.getPulsarName()+" dist "+distance);
//		Target p, closest = null;
//		double minDist = Double.POSITIVE_INFINITY;
//		
//		
//		//existing neighbours
//		List<Connection> conns1 = pulsar1.getNeighbours();
//		
//		//find existing neighbour closest to candidate
//		for (Connection connection : conns1) 
//		{
//			p = connection.getOtherTarget(pulsar1);
//			dist = p.angularDistanceTo(candidate);
//			if (minDist > dist)
//			{
//				minDist = dist;
//				closest = p;
//			}	
//		}
//
//		maxDist = dists[0] = pulsar1.angularDistanceTo(closest);
//		largest = 0;
//		dists[1] = candidate.angularDistanceTo(closest);
//		dists[2] = pulsar1.angularDistanceTo(candidate);
//		for (int j = 1; j < dists.length; j++) 
//			if(dists[j] > maxDist)
//			{
//				maxDist = dists[j];
//				largest = j;
//			}
//		switch (largest) 
//		{// mainToClosest (very unlikely?)
//		case 0:
//			System.out.println("Closest "+closest+" cand "+candidate);
//			System.out.println("Main to closest "+dists[0]);
//			System.out.println("closest to cand "+dists[1]);
//			System.out.println("Main to cand "+dists[2]);
//			break;
//			// closestToCandidate; candidate far away from closest included, so need a link between main and cand
//		case 1:
//			if(!pulsar1.hasTarget(candidate))
//			{
//				if(!pulsar1.hasTarget(candidate))
//				{
//					Connection co;
//					//if the connection object already exists, don't make a new onw
//					if(candidate.hasTarget(pulsar1))
//						co = candidate.getConnectionForTarget(pulsar1);
//					else
//						co = new Connection(pulsar1, candidate, dists[2]);
//					pulsar1.addNeighbour(co);
//				}
//			}
//			break;
//			// candidateToMain
//		case 2:
//			if (dists[1] < 1.5* dists[0])
//			{
//				if (dists[2] < (dists[1] + dists[0]) * ratio)
//				{
//					if(!pulsar1.hasTarget(candidate))
//					{
//						Connection co;
//						//if the connection object already exists, don't make a new onw
//						if(candidate.hasTarget(pulsar1))
//							co = candidate.getConnectionForTarget(pulsar1);
//						else
//							co = new Connection(pulsar1, candidate, dists[2]);
//						pulsar1.addNeighbour(co);		
//					}
//				}
//			}
//			break;
//		}
//
//	}



	private void makeConnectionToClosest(Target p, List<Target> targets, double cutoff, Location loc, GregorianCalendar gc)
	{
		double minDist = Double.POSITIVE_INFINITY;
		double dist = 0;
		Target closest = null;

		for (Target target : targets) 
		{
			if(target == p)
				continue;

			try {
				dist = target.angularDistanceTo(p, loc, gc);
			} catch (WrongTypeException e) {
				// This won't happen, they are all target objects in this pre-processing stage
				e.printStackTrace();
			}		

			if(minDist > dist)
			{
				minDist = dist;
				closest = target;
			}
		}
		Connection c = new Connection(closest, p, minDist);
		closest.addNeighbour(c);
		p.addNeighbour(c);
	}

//	public static List<Target> createLinks(List<Target> targets, int neighbours)
//	{
//		//List<Pulsar> pulsars2 = new ArrayList<Pulsar>(50);
//		//		Random r = new Random(2222);
//		//		for (int i = 0; i < 20; i++) 
//		//		{
//		//			Pulsar p = pulsars.get(r.nextInt(pulsars.size()));
//		//			if (!pulsars2.contains(p))
//		//				pulsars2.add(p);
//		//		}
//		double d = 0;
//
//		ArrayList<Double> list = new ArrayList<Double>();
//		for (Target target : targets) 
//		{
//			HashMap<Double, Target> hm = new HashMap<Double, Target>();
//			for (Target target2 : targets) 
//			{
//				if(target == target2)
//					continue;
//				d = target.angularEquatorialDistanceTo(target2);
//				hm.put(d, target2);
//			}
//			list.clear();
//			list.addAll(hm.keySet());
//
//			Collections.sort(list);
//			//			System.out.println("New list");
//			//			for (int i = 0; i < list.size(); i++) {
//			//				System.out.println(list.get(i));
//			//			}
//
//			for (int i = 0; i < neighbours; i++) 
//			{
//				Connection c = new Connection(target, hm.get(list.get(i)), list.get(i));
//				target.addNeighbour(c);
//			}
//		}
//		return targets;
//	}
}
