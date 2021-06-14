package io;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import observation.interference.Satellite;

public class NoradDataReader 
{
	private List<String> allFiles;
	private String noradFilePath;
	
	public NoradDataReader(String noradPath) 
	{
		noradFilePath = noradPath;
	}

	public void findAllFilesInDirectory()
	{
		allFiles = new ArrayList<String>();

		File[] files = new File(noradFilePath).listFiles();
		//If this pathname does not denote a directory, then listFiles() returns null. 

		for (File file : files) {
		    if (file.isFile()) {
		    	allFiles.add(noradFilePath+"/"+file.getName());
		    }
		}
	}
	
	public List<Satellite> readSatellites()
	{
		List<Satellite> satellites = new ArrayList<Satellite>();
		String temp ;

		Satellite sat = null;

		for (String string : allFiles) 
		{
			String type = string.substring(string.lastIndexOf("/")+1, string.indexOf("."));
			
			try {
				Scanner sc = new Scanner(new File(string));
				
				while(sc.hasNextLine())
				{
					temp = sc.nextLine();
					if(temp.startsWith("1"))
						sat.setFirstLine(temp);
					else if(temp.startsWith("2"))
						sat.setSecondLine(temp);
					else
					{
						temp = temp.trim();
						sat = new Satellite(temp);
						sat.setType(type);
						satellites.add(sat);
					}
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		return satellites;
	}
}
