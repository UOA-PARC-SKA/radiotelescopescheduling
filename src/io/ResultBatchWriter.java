package io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import evaluation.Results;
import observation.Schedule;
import observation.live.ObservationState;
import util.Utilities;

public class ResultBatchWriter 
{
	private PrintWriter printWriter;
	private static String fileEnding = ".csv";
	private static String file ;
	private List<Results> results;
	private String directory;
	private static int fileNumber = 0;
	
	public ResultBatchWriter(List<Results> results, Properties props)
	{	
		this.results = results;

		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(results.get(0).getSchedule().getStartTime());
		directory = props.getProperty("outputdir");
//		String number =""+ gc.get(GregorianCalendar.HOUR_OF_DAY);
//		if(number.length() == 1)
//			number = "0"+number;
//		directory += "/"+number+"/";
		directory += "/";
		
		file = "batch_"+ fileNumber++ +"_"
					+gc.get(GregorianCalendar.YEAR)+"_"
					+(gc.get(GregorianCalendar.MONTH)+1)+"_"
					+gc.get(GregorianCalendar.DAY_OF_MONTH)+"_"
					+gc.get(GregorianCalendar.HOUR_OF_DAY)+"_"
					+gc.get(GregorianCalendar.MINUTE)+"_"
					+gc.get(GregorianCalendar.SECOND)+"_";
		try
		{
			printWriter = new PrintWriter(new FileWriter(directory + file + fileEnding, false));
			
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void writeResults() 
	{
		writeHeader();
		writeHeadings();
		writeData();
		writeExplanations();
		closeWriter();
	}
	
	private void writeHeader() 
	{
		double [] d = getDurationMeanAndStdDev();
		printWriter.println("Duration mean, "+ d[0]+", std dev, "+d[1]);
		d = getRelSlewMeanAndStdDev();
		printWriter.println("Rel slew mean, "+ d[0]+", std dev, "+d[1]);
		d = getRelWaitMeanAndStdDev();
		printWriter.println("Rel wait mean, "+ d[0]+", std dev, "+d[1]);
		d = getRelObsMeanAndStdDev();
		printWriter.println("Rel obs mean, "+ d[0]+", std dev, "+d[1]);
	}

	private void writeData() 
	{
		for (int i = 0; i < results.size(); i++) 
		{
			Schedule s = results.get(i).getSchedule();
			printWriter.print(i+", ");
			printWriter.print(Utilities.millisecondsToHours(s.getScheduleLength()) +", ");
			long slewTime = (long) results.get(i).getTotalSlewTime() * 1000;
			printWriter.print(Utilities.millisecondsToHours(slewTime) +", ");
			long waitingTime = results.get(i).getTotalWaitingTime() * 1000;
			printWriter.print(Utilities.millisecondsToHours(waitingTime) +", ");
			long observationTime = (long) results.get(i).getTotalObservationTime() * 1000;
			printWriter.print(Utilities.millisecondsToHours(observationTime) +", ");
			printWriter.print(results.get(i).getRelativeSlewTime(slewTime)+", ");
			printWriter.print(results.get(i).getRelativeWaitingTime(waitingTime)+", ");
			printWriter.print(results.get(i).getRelativeObservationTime(observationTime)+", ");
			int maxAttempts = results.get(i).getMaxNoAttempts();
			printWriter.print(maxAttempts+", ");
			printWriter.print(results.get(i).getTimesMaxAttempt(maxAttempts)+", ");
			int[] res = results.get(i).getOutcomesHistogram();
			printWriter.print(res[ObservationState.OBSERVATION_COMPLETE_SCINT_MAX]+", ");
			printWriter.print(res[ObservationState.OBSERVATION_COMPLETE_SCINT_NORMAL]+", ");
			printWriter.print(res[ObservationState.OBSERVATION_COMPLETE_SCINT_REPEAT]+", ");
			printWriter.print(res[ObservationState.OBSERVATION_ABORTED_SCINT_WEAK]+", ");
			res = results.get(i).getInterruptionsHistogram();
			printWriter.print(res[ObservationState.OBSERVATION_ABORTED_OBJECT_SET]+", ");
			printWriter.println(res[ObservationState.OBSERVATION_ABORTED_SATELLITE]+", ");
		} 
	}

	
	private void writeHeadings()
	{
		printWriter.print("No, ");
		printWriter.print("Duration, ");
		printWriter.print("Slew time, ");
		printWriter.print("Wait time, ");
		printWriter.print("Obs time, ");
		printWriter.print("Rel slew, ");
		printWriter.print("Rel wait, ");
		printWriter.print("Rel obs, ");
		printWriter.print("Max attempts, ");
		printWriter.print("Times max att, ");
		printWriter.print("Obs result 1, ");
		printWriter.print("Obs result 2, ");
		printWriter.print("Obs result 3, ");
		printWriter.print("Obs result 4, ");
		printWriter.print("Interruption 1, ");
		printWriter.println("Interruption 2 ");
		

	}
	
	private void writeExplanations()
	{
		printWriter.println();
		printWriter.println("Obs result "+ObservationState.OBSERVATION_COMPLETE_SCINT_MAX+", Max scintillation; shortened observation");
		printWriter.println("Obs result "+ObservationState.OBSERVATION_COMPLETE_SCINT_NORMAL+", Normal scintillation.");
		printWriter.println("Obs result "+ObservationState.OBSERVATION_COMPLETE_SCINT_REPEAT+", Short timescale; repeated check of scintillation.");
		printWriter.println("Obs result "+ObservationState.OBSERVATION_ABORTED_SCINT_WEAK+", Weak scintillation; aborted.");
		
		printWriter.println();
		printWriter.println("Interruption "+ObservationState.OBSERVATION_ABORTED_OBJECT_SET+", Object set.");
		printWriter.println("Interruption "+ObservationState.OBSERVATION_ABORTED_SATELLITE+", Interference from satellite.");
		
	}
	
	private double[] getDurationMeanAndStdDev()
	{
		double[] d = new double[2];
		double[] entries = new double[results.size()];
		for (int i = 0; i < results.size(); i++) 
		{
			entries[i] = Utilities.millisecondsToHours(results.get(i).getSchedule().getScheduleLength());
		}
		d[0] = Utilities.getMean(entries);
		d[1] = Utilities.getStdDev(entries, d[0]);
		return d;
	}
	
	private double[] getRelSlewMeanAndStdDev()
	{
		double[] d = new double[2];
		double[] entries = new double[results.size()];
		
		for (int i = 0; i < results.size(); i++) 
		{
			entries[i] = results.get(i).getRelativeSlewTime(results.get(i).getTotalSlewTime()*1000) ;
		}
		d[0] = Utilities.getMean(entries);
		d[1] = Utilities.getStdDev(entries, d[0]);
		return d;
	}
	
	private double[] getRelWaitMeanAndStdDev()
	{
		double[] d = new double[2];
		double[] entries = new double[results.size()];

		for (int i = 0; i < results.size(); i++) 
		{
			entries[i] = results.get(i).getRelativeWaitingTime(results.get(i).getTotalWaitingTime()*1000);
		}
		d[0] = Utilities.getMean(entries);
		d[1] = Utilities.getStdDev(entries, d[0]);
		return d;
	}
	
	private double[] getRelObsMeanAndStdDev()
	{
		double[] d = new double[2];
		double[] entries = new double[results.size()];
		for (int i = 0; i < results.size(); i++) 
		{
			entries[i] = results.get(i).getRelativeObservationTime(results.get(i).getTotalObservationTime()*1000);
		}
		d[0] = Utilities.getMean(entries);
		d[1] = Utilities.getStdDev(entries, d[0]);
		return d;
	}
	
	public void closeWriter()
	{
		printWriter.close();
	}
}
