package db;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bson.Document;

import observation.Connection;
import observation.Observable;
import observation.Target;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;



public class MongoDBWriter 
{

	private  MongoClient mongoClient;

	Block<Document> printBlock = new Block<Document>() {
		@Override
		public void apply(final Document document) {
			System.out.println(document.toJson());
		}
	};

	public void insertTargets( List<Target> targets ) {

		try{   		
			mongoClient = new MongoClient( "localhost" , 27017 );

			MongoDatabase db = mongoClient.getDatabase( "observations" );
	
			MongoCollection<BasicDBObject> collection = db.getCollection("observations.observable", BasicDBObject.class);
			//  MongoCollection<Document> collection = db.getCollection("observations.staticlinks");
			
			for (Target target : targets) 
			{
				collection.insertOne(buildTargetDocument(target));
			}
			

			  //db.createCollection("observations.staticlinks");
			
			//System.out.println(collection.count());


			mongoClient.close();
		}catch(Exception e){
			System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		}
	}
	
	

	private BasicDBObject buildTargetDocument(Target t)
	{

		BasicDBObject object = new BasicDBObject();
		object.put("_id", getNextSequence("targetid"));
		object.put("ra", t.getEquatorialCoordinates().getRightAscension()); 
		object.put("dec", t.getEquatorialCoordinates().getDeclination());

		List<BasicDBObject> targets = new ArrayList<BasicDBObject>();
		List<Observable> o = t.getObservables();

		for (Observable observable : o) 
		{
			targets.add(new BasicDBObject("name", observable.getName()));
		}

		object.put("objects", targets);

		return object;
	}
	
	public void insertStaticLinks(List<Target> targets)
	{
		mongoClient = new MongoClient( "localhost" , 27017 );
		MongoDatabase db = mongoClient.getDatabase( "observations" );
		MongoCollection<BasicDBObject> collection = db.getCollection("observations.staticlinks", BasicDBObject.class);
		for (Target target : targets) 
		{
			for (Connection conn : target.getNeighbours()) 
			{
				collection.insertOne(buildLinkDocument(conn));
			}
			
		}
		mongoClient.close();
	}
	

	private BasicDBObject buildLinkDocument(Connection c)
	{
		BasicDBObject object = new BasicDBObject();
		object.put("distance", c.getDistance()); 
		Target second = (Target) c.getOtherTarget(c.getFirst());
		object.put("objects", Arrays.asList(getId(((Target)c.getFirst())), getId(second))); 
		return object;
	}

	private String getId(Target t)
	{
		MongoDatabase db = mongoClient.getDatabase( "observations" );
		MongoCollection<BasicDBObject> collection = db.getCollection("observations.observable", BasicDBObject.class);
		List<Observable> o = t.getObservables();
		BasicDBObject query = new BasicDBObject("objects.name", new BasicDBObject("$eq", o.get(0).getName()));

		FindIterable<BasicDBObject> cursor = collection.find(query);

		BasicDBObject bdo = cursor.first();

		return ""+ bdo.getLong("_id");
	}

	public Object getNextSequence(String name) {


		// Now connect to your databases
		DB db = mongoClient.getDB("observations");
		DBCollection collection = db.getCollection("counters");
		BasicDBObject find = new BasicDBObject();
		find.put("_id", name);
		BasicDBObject update = new BasicDBObject();
		update.put("$inc", new BasicDBObject("seq", 1));
		DBObject obj =  collection.findAndModify(find, update);
		return obj.get("seq");
	}

	public void addObservations(List<Target> targets)
	{
		mongoClient = new MongoClient( "localhost" , 27017 );
		MongoDatabase db = mongoClient.getDatabase( "observations" );
		MongoCollection<BasicDBObject> collection = db.getCollection("observations.observable", BasicDBObject.class);
		BasicDBObject query;
		BasicDBObject updateFields;
		
		for (Target target : targets) 
		{
			for (Observable observable : target.getObservables()) 
			{
				query = new BasicDBObject("objects.name", observable.getName());
				updateFields = new BasicDBObject();
				updateFields.append("objects.$.integration_time", observable.getExpectedIntegrationTime());
				updateFields.append("objects.$.time_observed", observable.getActualTimeObserved());
				updateFields.append("objects.$.needs_observing", observable.needsObserving());
				updateFields.append("objects.$.scint_timescale", observable.getScintillationTimescale());
				BasicDBObject setQuery = new BasicDBObject();
				setQuery.append("$set", updateFields);
				
				collection.updateOne(query, setQuery);
			}
			 
		}
		
		mongoClient.close();

	
		
	}


	//	   
	//	   Find one document
	//	   
	//	   myDoc = collection.find(eq("i", 71)).first();
	//	   System.out.println(myDoc.toJson());
	//	   
	//	   Document myDoc = collection.find().first();
	//	   System.out.println(myDoc.toJson());
	//	   List<Document> documents = new ArrayList<Document>();
	//	   for (int i = 0; i < 100; i++) {
	//	       documents.add(new Document("i", i));
	//	   }

	//	   
	//	   {
	//		   _id: 0000001,
	//		   ra: "01:34:18.6690",
	//		   dec: "-29:37:16.91",
	//		   objects: [
	//					{
	//						name: "J0134-2937",
	//						type: "Pulsar",
	//						start_observation: ISODate("2017-02-06"),
	//						end_observation: ISODate("2017-06-06"),
	//						time_to_observe: "120",
	//						time_observed: "45"				
	//					}
	//					]
	//		}
}
