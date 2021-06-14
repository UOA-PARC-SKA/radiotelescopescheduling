package dataprocessing;

import io.TargetLocationReader;

import java.util.List;
import java.util.Properties;

import optimisation.triangulations.NNPreoptimisation;
import db.MongoDBReader;
import db.MongoDBWriter;
import observation.Target;

public class DataProcessor 
{
	private List<Target> targets;
	
	public void processPulsarLocationData(Properties props)
	{
		TargetLocationReader fr = new TargetLocationReader();
		targets = fr.getPulsarData(props.getProperty("dataset"));
		
		NNPreoptimisation preop = new NNPreoptimisation();
		preop.createLinksByTriangles(targets, Double.parseDouble(props.getProperty("nn_distance_ratio")), null, null);
		MongoDBWriter mdw = new MongoDBWriter();
		mdw.insertTargets(targets);
		mdw.insertStaticLinks(targets);
	}
	
	public void processNewObservationData(Properties props)
	{
		MongoDBReader mdr = new MongoDBReader();
		targets = mdr.retrieveAllTargets();
		//need the existing targets to add new observation times
		TargetLocationReader fr = new TargetLocationReader();
		fr.addObservationData(targets, props.getProperty("observations_dataset"));
		MongoDBWriter mdw = new MongoDBWriter();
		mdw.addObservations(targets);
	}
}
