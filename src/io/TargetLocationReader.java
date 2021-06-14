package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import astrometrics.Conversions;
import astrometrics.EquatorialCoordinates;
import observation.Observable;
import observation.Pulsar;
import observation.Target;
import util.Utilities;


public class TargetLocationReader 
{
	

	public List<Target> getPulsarData(String filename)
	{
		try 
		{
			Scanner sc = new Scanner(new File(filename));
			List<Target> targets = new ArrayList<Target>();
			Pulsar pulsar = null;
			Target target = null;
			EquatorialCoordinates ec = null;
			boolean added = false;

			String temp ;
			String[] parameters ;


			while(sc.hasNextLine())
			{
				temp = sc.nextLine();
				temp = temp.trim();

				parameters = temp.split(" ");
				
				ec = new EquatorialCoordinates(Conversions.hoursToRadians(parameters[1]), Conversions.degreesToRadians(parameters[2]));
				pulsar = new Pulsar(parameters[0]);
				pulsar.setScintillationTimescale(Integer.parseInt(parameters[3]));
				for (Target t : targets) 
				{
					if(t.hasLocation(ec))
					{
						t.addObservable(pulsar);
						added = true;
					}
				}
				if(!added)
				{
					target = new Target(ec);
					target.addObservable(pulsar);
					targets.add(target);
				}
				added=false;
			}

			sc.close();
			return targets;

		} catch (FileNotFoundException e) {

			e.printStackTrace();
			return null;
		}	
	}

	public void addObservationData(List<Target> targets, String filename) 
	{
		
		Scanner sc;
		try {
			sc = new Scanner(new File(filename));
			boolean added = false;

			String temp ;
			String[] parameters ;


			while(sc.hasNextLine())
			{
				temp = sc.nextLine();
				temp = temp.trim();
				added = false;
				parameters = temp.split(" ");
				
				for (Target target : targets) 
				{
					for (Observable o : target.getObservables()) 
					{
						if(parameters[0].equals(o.getName()))
						{
							double integrationMinLength = Double.parseDouble(parameters[3]);
							double integration1000Turns = Double.parseDouble(parameters[4]);
							o.setExpectedIntegrationTime(integrationMinLength > integration1000Turns ? integrationMinLength : integration1000Turns);
							added = true;
						}
							
					}
				}
				if(!added)
				{
					System.err.println(parameters[0]+" not found");
				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


}
