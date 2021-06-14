import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import astrometrics.EquatorialCoordinates;
import io.IoUtils;
import observation.Observable;
import observation.Pulsar;
import observation.Target;
import util.Utilities;
import dataprocessing.DataProcessor;
import db.MongoDBReader;


public class ProcessingMain 
{
//	public static void main(String[] args) 
//	{
//		Properties props = IoUtils.loadProps("config");
//		DataProcessor dp = new DataProcessor();
//		//dp.processPulsarLocationData(props); //done at the start only
//		dp.processNewObservationData(props);
//	}
	
	//to collect the duration, slew times and interruptions into one file for correlations
	public static void main(String[] args) 
	{
		String directory = "C:/Data/research/telescope/results/dataset1/byEarliestSet/March17/";
		String fileStart = "batch_2017_3_21_";
		String fileEnd = "_0_0_.csv";

			String temp ;
			String parameters[];
			
			ArrayList <String> lines = new ArrayList<>();
			String line;

			Scanner sc;
			int sum;
					
			for (int i = 0; i < 24; i++) 
			{
				sum = 0;
				try {
					sc = new Scanner(new File(directory + fileStart + i + fileEnd));
					
					do{
						temp = sc.nextLine();
						parameters  = temp.split(",");
					}while(!parameters[0].equals("No"));
					
					
					while(true)
					{
						temp = sc.nextLine();
						parameters  = temp.split(",");
						if(parameters.length < 10)
							break;
						sum += Integer.parseInt(parameters[15].trim());
					}
					
					lines.add(""+sum);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try {
			PrintWriter printWriter = new PrintWriter(new FileWriter("collection.csv", false));

			for (int i = 0; i < lines.size(); i++) 
			{
				printWriter.println(lines.get(i));
			}
			printWriter.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	//to collect the duration, slew times and interruptions into one file for correlations
//	public static void main(String[] args) 
//	{
//		String directory = "C:/Data/research/telescope/results/dataset1/byEarliestSet/March17/";
//		String fileStart = "batch_2017_3_21_";
//		String fileEnd = "_0_0_.csv";
//
//
//
//			String temp ;
//			String parameters[];
//			
//			ArrayList <String> lines = new ArrayList<>();
//			String line;
//
//			Scanner sc;
//			
//			
//
//			for (int i = 0; i < 24; i++) 
//			{
//				try {
//					sc = new Scanner(new File(directory + fileStart + i + fileEnd));
//					
//					do{
//						temp = sc.nextLine();
//						parameters  = temp.split(",");
//					}while(!parameters[0].equals("No"));
//					
//					//headings only needed the first time
//					if(i > 0)
//					{
//						temp = sc.nextLine();
//						parameters  = temp.split(",");
//					}
//						
//					
//					do
//					{
//
//						line = parameters[1]+", "+parameters[2]+", "+parameters[3]+", "+parameters[4]+", "+parameters[8]
//								+", "+parameters[10]+", "+parameters[11]+", "+parameters[12]+", "+parameters[13]
//										+", "+parameters[14]+", "+parameters[15];
//						
//						lines.add(line);
//						temp = sc.nextLine();
//						parameters  = temp.split(",");
//						
//					}while(parameters.length > 10);
//
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			try {
//			PrintWriter printWriter = new PrintWriter(new FileWriter("collection.csv", false));
//
//			for (int i = 0; i < lines.size(); i++) 
//			{
//				printWriter.println(lines.get(i));
//			}
//			printWriter.close();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}
	
	//to process results - collect the main stats into one file
//	public static void main(String[] args) 
//	{
//		String directory = "C:/Data/research/telescope/results/dataset1/byEarliestSet/March17/";
//		String fileStart = "batch_2017_3_21_";
//		String fileEnd = "_0_0_.csv";
//
//
//
//			String temp ;
//			String parameters[];
//			
//			String[] lines = new String[4];
//
//			Scanner sc;
//			try {
//				sc = new Scanner(new File(directory + fileStart + 0 + fileEnd));
//				for (int i = 0; i < lines.length; i++) 
//				{
//					temp = sc.nextLine();
//					parameters  = temp.split(",");
//					lines[i] = parameters[0] + "," + parameters[1] ;
//				}
//
//
//
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			for (int i = 1; i < 24; i++) 
//			{
//				try {
//					sc = new Scanner(new File(directory + fileStart + i + fileEnd));
//					
//					for (int j = 0; j < lines.length; j++) 
//					{
//						temp = sc.nextLine();
//						parameters  = temp.split(",");
//						lines[j] += "," + parameters[1];
//					}
//				} catch (FileNotFoundException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//			try {
//			PrintWriter printWriter = new PrintWriter(new FileWriter("collection.csv", false));
//
//			for (int i = 0; i < lines.length; i++) 
//			{
//				printWriter.println(lines[i]);
//			}
//			printWriter.close();
//		} catch (IOException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//	}
	
	// this is for adding to the basic pulsar file, hopefully not needed again
//	public static void main(String[] args) 
//	{

//	List<String> targets = new ArrayList<>();
//		try
//		{
//			Scanner sc = new Scanner(new File("data/allpulsars.txt"));
//
//			String temp ;
//			String parameters[];
//		
//
//			while(sc.hasNextLine())
//			{
//				temp = sc.nextLine();
//				targets.add(temp);
//			//	firstPart = temp.substring(0, temp.indexOf(" "));
//
//			}
//			sc.close();
//			sc = new Scanner(new File("data/name_ra_dec_minP_minS.txt"));
//			PrintWriter printWriter = new PrintWriter(new FileWriter("data/allpulsars.txt", true));
//	
//	
//			
//			String[] existing;
//			boolean found = false;
//			while(sc.hasNextLine())
//			{
//				found = false;
//				temp = sc.nextLine();
//				parameters = temp.split(" ");
//				for (String target : targets) 
//				{
//					existing = target.split(" ");
//					if(existing[0].equals(parameters[0]))
//						found = true;
//				}
//				if(!found)
//				{
//					temp = parameters[0] + " "+parameters[1] + " "+parameters[2] + " ";
//					temp += Utilities.getScintillationTimescale();
//					printWriter.println(temp);
//				}
//				
//				
//				
//			}
//			printWriter.close();
//			sc.close();
//			
//		} catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
//	public static void main(String[] args) 
//	{
//
//	List<String> targets = new ArrayList<>();
//	List<String> times = new ArrayList<>();
//		try
//		{
//			Scanner sc = new Scanner(new File("data/psrs.txt"));
//			PrintWriter printWriter = new PrintWriter(new FileWriter("data/dataset3.txt", false));
//			String temp ;
//			String parameters[];
//		
//
//			while(sc.hasNextLine())
//			{
//				temp = sc.nextLine();
//				targets.add(temp);
//			//	firstPart = temp.substring(0, temp.indexOf(" "));
//
//			}
//			sc.close();
//			sc = new Scanner(new File("data/name_ra_dec_minP_minS.txt"));
//			
//			while(sc.hasNextLine())
//			{
//				temp = sc.nextLine();
//				parameters = temp.split(" ");
//				times.add(" "+parameters[3]+" "+parameters[4]);
//
//			}
//			sc.close();
//			Random random = new Random(System.currentTimeMillis());
//			int lat;
//			int j;
//			String number = "";
//			int index1;
//			for (int i = 0; i < 300; i++) 
//			{
//				do{
//					number = "";
//					lat = -1;
//					j = random.nextInt(targets.size());
//					temp = targets.get(j);
//					
//					index1 = temp.indexOf("+");
//					if(index1 > -1)
//					{
//						number = temp.substring(index1, index1+3);
//						System.out.println(number);
//						lat = Integer.parseInt(number);
//					}
//				}while ( lat <= 0 || lat > 55);
//				targets.remove(j);
//				temp += times.get(random.nextInt(times.size()));
//				System.out.println(temp);
//				printWriter.println(temp);
//			}
//			printWriter.close();
//			
//			
//		} catch (IOException e)
//		{
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
}
