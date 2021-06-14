package db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import astrometrics.EquatorialCoordinates;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import observation.Connection;
import observation.Observable;
import observation.Pulsar;
import observation.Target;

public class MongoDBReader 
{
	public List<Target> retrieveAllTargets()
	{
		List<Target> targets = Collections.synchronizedList(new ArrayList<Target>());
		
		MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
		MongoDatabase db = mongoClient.getDatabase( "observations" );

		MongoCollection<BasicDBObject> collection = db.getCollection("observations.observable", BasicDBObject.class);
		
		FindIterable<BasicDBObject> cursor = collection.find();

		for (BasicDBObject document : cursor) 
		{
			targets.add(documentToTarget(document));
		}

		mongoClient.close();
		return targets;
	}
	
	private Target documentToTarget(BasicDBObject bdo)
	{
		Target t = new Target(new EquatorialCoordinates(bdo.getDouble("ra"), bdo.getDouble("dec")));
		t.setId(bdo.getLong("_id"));
		BasicDBList obj = (BasicDBList) bdo.get("objects");
		BasicDBObject[] objArr = obj.toArray(new BasicDBObject[0]);
		for(BasicDBObject dbObj : objArr) 
		{
			Observable o = new Pulsar(dbObj.getString("name"));
			o.setExpectedIntegrationTime(dbObj.getDouble("integration_time"));
			o.setActualTimeObserved(dbObj.getDouble("time_observed"));
			o.setScintillationTimescale(dbObj.getInt("scint_timescale"));
			t.addObservable(o);	
		}	
		return t; 
	}
	
	public void retrieveLinksForTarget(Target t, List<Target> targets)
	{
		List<Connection> conns = new ArrayList<Connection>();
		
		MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
		MongoDatabase db = mongoClient.getDatabase( "observations" );

		MongoCollection<BasicDBObject> collection = db.getCollection("observations.staticlinks", BasicDBObject.class);
		
		BasicDBObject query = new BasicDBObject("objects", new BasicDBObject("$eq", ""+t.getId()));
		FindIterable<BasicDBObject> cursor = collection.find(query);
		
		for (BasicDBObject document : cursor) 
		{
			conns.add(documentToConnection(document, targets));
		}
		mongoClient.close();
	}
	
	private Connection documentToConnection(BasicDBObject bdo, List<Target> targets)
	{
		Connection c = new Connection(bdo.getDouble("distance"));
		BasicDBList obj = (BasicDBList) bdo.get("objects");
		long one= (long) Double.parseDouble(obj.get(0).toString());
		long two= (long) Double.parseDouble(obj.get(1).toString());
		System.out.println();
		for (Target target : targets) 
		{
			if(target.getId() == one )
			{
				c.setTarget1(target);
				target.addNeighbour(c);
				System.out.println("First "+target);
			}
			if ( target.getId() == two)
			{
				c.setTarget2(target);
				
				System.out.println("Second "+target);
			}
		}
	

//		BasicDBObject[] objArr = obj.toArray(new BasicDBObject[0]);
//		for(BasicDBObject dbObj : objArr) 
//			t.addObservable(new Pulsar(dbObj.getString("name")));
		
		return c; 
	}
	
	//How to find an object having a particular attribute:
	// db.observations.observable.find({"objects.time_observed": {$exists: false}}).count()
}
