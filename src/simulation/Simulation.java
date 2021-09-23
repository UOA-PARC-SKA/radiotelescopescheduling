package simulation;



import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.swing.JFrame;

import evaluation.Results;
import io.ResultFileWriter;
import optimisation.Scheduler;
import observation.Connection;
import observation.Telescope;
import simulation.gui.MainWindow;
import util.Utilities;



public class Simulation extends java.util.Observable
{
	private Scheduler scheduler;
	private MainWindow frame;
	private Clock clock;
	private Telescope telescope;
	private Results results = null;
	private boolean printResults = true;
	private boolean showGui = true;
	private Properties props;

	
	private int simulationIntervalInSeconds;
	
	
	public Simulation (Properties props) throws Exception
	{
		this.props = props;
		telescope = Telescope.getTelescope(props.getProperty("telescope"));
		scheduler = new Scheduler(props, telescope);
		this.clock = Clock.getSimulationClock();
		startSimulationClock(props.getProperty("observation_start"));
		this.simulationIntervalInSeconds = Integer.parseInt(props.getProperty("simulation_speed"));
		this.clock.setSimulationSpeed(simulationIntervalInSeconds);
		this.printResults= Boolean.parseBoolean(props.getProperty("print_results"));
		this.showGui= Boolean.parseBoolean(props.getProperty("show_gui"));
		if(printResults)
			results = new Results();
	}

	public void createAndShowGUI() 
	{
		//Create and set up the window.
		frame = new MainWindow("Radio Observations");
		frame.initialiseMainWindow( scheduler.getAllTargets(), scheduler.getSchedule(), telescope, scheduler.getSkyState());
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addObserver(frame.getIllustrationPN());
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	}

	public void run() 
	{	
		scheduler.buildSchedule();
		if(printResults)
		{
			results.setSchedule(scheduler.getSchedule());
			printResults(props);
		}
		
		if(!showGui)
			return;

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});

//		long endTime = Clock.getSimulationClock().getTime().getTimeInMillis() + 864400000;
		while (scheduler.getSchedule().getEndTime()+6000 > Clock.getSimulationClock().getTime().getTimeInMillis()) 
//		while (endTime > Clock.getSimulationClock().getTime().getTimeInMillis()) 

		{
			setChanged();
			notifyObservers();
			try {
				Thread.currentThread();
				Thread.sleep(simulationIntervalInSeconds);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Clock.getSimulationClock().advanceBy(10);
		}
	}
	
	public List<Connection> getPath()
	{
		return scheduler.getFinalPath();
	}

	public void startSimulationClock(String property) 
	{
		Date startDate = Utilities.stringToDate(property);
		Clock.getSimulationClock().startAt(startDate);
	}
	
	public void printResults(Properties props)
	{
		ResultFileWriter fw = new ResultFileWriter(results, props);
		fw.writeResults(results);
		fw.closeWriter();
	}

	public Results getResults() {
		return results;
	}
}
