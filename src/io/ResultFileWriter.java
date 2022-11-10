package io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import java.util.TimeZone;

import observation.Position;
import observation.Schedule;
import observation.Target;
import observation.interference.Satellite;
import observation.live.ObservationState;
import util.Utilities;
import evaluation.Results;



public class ResultFileWriter 
{
	
	private PrintWriter printWriter;
	private static String fileEnding = ".csv";
	private String file ;
	private static int fileNumber = 0;
	private String directory;
	
	
	public ResultFileWriter(Results results, Properties props)
	{	
		GregorianCalendar gc = new GregorianCalendar();
		gc.setTimeInMillis(results.getSchedule().getStartTime());
		directory = props.getProperty("outputdir");
		String number =""+ gc.get(GregorianCalendar.HOUR_OF_DAY);
		if(number.length() == 1)
			number = "0"+number;
		directory += "/"+number+"/";
		
		file = "results_"+gc.get(GregorianCalendar.YEAR)+"_"
					+(gc.get(GregorianCalendar.MONTH)+1)+"_"
					+gc.get(GregorianCalendar.DAY_OF_MONTH)+"_"
					+gc.get(GregorianCalendar.HOUR_OF_DAY)+"_"
					+gc.get(GregorianCalendar.MINUTE)+"_"
					+gc.get(GregorianCalendar.SECOND)+"_";
		
		if(fileNumber == 60)
			fileNumber = 0;
		try
		{
			printWriter = new PrintWriter(new FileWriter(directory + file + fileNumber++ + fileEnding, false));
			
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void writeResults(Results results) 
	{
		writeHeader(results);
		writeHeadings();
		writeScheduleStates(results);
		closeWriter();
	}

	

	private void writeHeader(Results results)
	{
		Schedule schedule = results.getSchedule();
		printWriter.println("Start time,  "+ Utilities.getDateAsString(schedule.getStartTime())+ ","+ schedule.getStartTime());
		printWriter.println("End time,  "+ Utilities.getDateAsString(schedule.getEndTime())+ ","+ schedule.getEndTime());
		printWriter.println("Duration,  "+ Utilities.millisecondsToDateFormat(schedule.getScheduleLength())+ ","+ schedule.getScheduleLength());
		//have to change this to milliseconds
		double slewTime = results.getTotalSlewTime() * 1000;
		printWriter.println("Slew time, "+ Utilities.millisecondsToDateFormat((long)slewTime)+","+slewTime);
		double observationTime = results.getTotalObservationTime() * 1000;
		printWriter.println("Integration time, "+ Utilities.millisecondsToDateFormat((long)observationTime)+","+observationTime);
		double waitingTime = results.getTotalWaitingTime() * 1000;
		printWriter.println("Waiting time, "+ Utilities.millisecondsToDateFormat((long)waitingTime)+","+waitingTime);
		printWriter.println("Relative slew time, "+results.getRelativeSlewTime(slewTime));
		printWriter.println("Relative integration time, "+results.getRelativeObservationTime(observationTime));
		printWriter.println("Relative waiting time, "+results.getRelativeWaitingTime(waitingTime));
	}
	
	private void writeHeadings()
	{
		printWriter.print("Start, ");
		printWriter.print("End, ");
		printWriter.print("Name, ");
		printWriter.print("RA, ");
		printWriter.print("DEC, ");
		printWriter.print("Slew (to here), ");
		printWriter.print("Expected Int, ");
		printWriter.print("Actual Int, ");
		printWriter.print("This Int, ");
		printWriter.print("Wait time, ");
		printWriter.print("Scint timescale, ");
		printWriter.print("Attempt no, ");
		printWriter.print("# sats cons, ");
		printWriter.print("Obs result, ");
		printWriter.print("Interruption, ");
		printWriter.println("Comment ");

	}
	
	private void writeScheduleStates(Results results)
	{
		Schedule schedule = results.getSchedule();
		List<ObservationState> states = schedule.getScheduleStates();
		for (ObservationState state : states) 
		{
			if(state.getCurrentTarget() instanceof Position)
				continue;
			Target t = (Target) state.getCurrentTarget();
			printWriter.print(Utilities.getDateAsString(state.getStartTime())+",");
			printWriter.print(Utilities.getDateAsString(state.getEndTime())+",");
			printWriter.print(state.getCurrentObservable().getName()+",");
			printWriter.print(t.getEquatorialCoordinates().getRightAscension()+",");
			printWriter.print(t.getEquatorialCoordinates().getDeclination()+",");
			printWriter.print(state.getLinkToHere().getFinalSlewTime()+",");
			printWriter.print(state.getCurrentObservable().getExpectedIntegrationTime()+",");
			if(state.wasAborted())
			{
				printWriter.print("incomplete,");	
			}
			else //the observable always has the complete observation
			{		
				printWriter.print(state.getCurrentObservable().getActualTimeObserved()+",");
			}
			printWriter.print(state.getIntegrationTime()+",");
			printWriter.print(state.getWaitingTime()+",");
			printWriter.print(state.getCurrentObservable().getScintillationTimescale()+",");
			printWriter.print(state.getHowManiethAttempt()+",");
			printWriter.print(state.getNoSatellites()+",");
			printWriter.print(state.getObsResult()+",");
			printWriter.print(state.getInterruptionResult()+",");
			printWriter.println(state.getComment());
			
//			String s = "";
//			for (Satellite sat : state.getPotentiallyInterfering()) 
//			{
//				s+= sat.getName()+" ";
//			}
//			
//			printWriter.println(s);
		}
	}
	
	
	public void closeWriter()
	{
		printWriter.close();
	}

}
