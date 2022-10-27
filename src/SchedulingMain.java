

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import evaluation.Results;
import io.IoUtils;
import io.ResultBatchWriter;
import simulation.Simulation;



public class SchedulingMain {

	public static Properties props = null;
	public static List<Results[]> results = null;
	public static boolean sweep24hours = false;
	

	public static void main(String[] args) throws Exception {
		//Schedule a job for the event dispatch thread:
		//creating and showing this application's GUI.
		props = IoUtils.loadProps("config");
		
		sweep24hours = Boolean.parseBoolean(props.getProperty("24hoursweep"));
		/*
		if(sweep24hours)
			run24Hours();
		else
		{
			int batches = Integer.parseInt(props.getProperty("batch"));
			if (batches > 1)
			{
				results = new ArrayList<>();
				runRepeats(batches);
			}
			else
				runOnce();
		}
		 */

		int batches = Integer.parseInt(props.getProperty("batch"));
		if (batches > 1){
			//results = new ArrayList<>();
			runRepeats(batches);
		}
		else
			runOnce();
	}
	
	public static void runOnce() throws Exception
	{
		Simulation sim = new Simulation(props);
		sim.run();
	}

	public static void runRepeats(int noRepeats) throws Exception
	{
		for (int i = 0; i < noRepeats; i++) 
		{
			System.out.println("BATCH NO:");
			System.out.println(i);
			Simulation sim = new Simulation(props);
			sim.run();

			ResultBatchWriter rbw = new ResultBatchWriter(new ArrayList<>(Arrays.asList(sim.getResults())), props);
			rbw.writeResults();

		}
	}

/*
	public static void run24Hours() throws Exception
	{
		String timeString;
		String[] timeStrings;
		int batches = Integer.parseInt(props.getProperty("batch"));
		for (int i = 0; i < 24; i++) 
		{
			timeString = props.getProperty("observation_start");
			timeStrings = timeString.split(" ");
			timeString = timeStrings[0]+ " "+i+ timeStrings[1].substring(timeStrings[1].indexOf(":"), timeStrings[1].length());
			props.setProperty("observation_start", timeString);
			if (batches > 1)
			{
				System.out.println(batches);
				results = new ArrayList<>();
				runRepeats(batches);
			}
			else
				runOnce();
		}
	}

 */

}
