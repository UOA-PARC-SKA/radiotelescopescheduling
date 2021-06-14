package io;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class IoUtils 
{
	public static Properties loadProps(String filename)
	{
		Properties props = new Properties();

		try
		{
			props.load(new FileInputStream(filename));
		} catch (FileNotFoundException e)
		{
			System.err.println("Missing config file!");
		} catch (IOException e)
		{
			System.err.println("Config file is gibberish to me!");
		}
		return props;
	}
}
