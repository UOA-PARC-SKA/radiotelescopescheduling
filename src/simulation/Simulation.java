package simulation;



import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.swing.JFrame;

import evaluation.Results;
import io.ResultFileWriter;
import observation.Position;
import observation.Target;
import observation.live.ObservationState;
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
	public static int NUMTELESCOPES;
	private Telescope telescope;
	private Telescope[] telescopes;
	private Results[] results;
	private boolean printResults = true;
	private boolean showGui = true;
	private Properties props;

	
	private int simulationIntervalInSeconds;
	
	
	public Simulation (Properties props) throws Exception
	{
		this.props = props;
		NUMTELESCOPES = Integer.parseInt(props.getProperty("teles_num"));
		telescopes = new Telescope[NUMTELESCOPES];
		for(int i=0; i<NUMTELESCOPES; i++)
			telescopes[i] = Telescope.getTelescope(props.getProperty("telescope"));

		scheduler = new Scheduler(props, telescopes);
		this.clock = Clock.getSimulationClock();
		startSimulationClock(props.getProperty("observation_start"));
		this.simulationIntervalInSeconds = Integer.parseInt(props.getProperty("simulation_speed"));
		this.clock.setSimulationSpeed(simulationIntervalInSeconds);
		this.printResults= Boolean.parseBoolean(props.getProperty("print_results"));
		this.showGui= false;//Boolean.parseBoolean(props.getProperty("show_gui"));
		if(printResults){
			results = new Results[NUMTELESCOPES];
			for(int i=0; i<NUMTELESCOPES; i++)
				results[i] = new Results();
		}

	}
/*
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

 */

	public void run() 
	{	
		scheduler.buildSchedule(props.getProperty("preoptimisation"));
		List<ObservationState>[] states = new List[NUMTELESCOPES];
		for(int i=0; i<NUMTELESCOPES; i++){
			states[i] = scheduler.getSchedule(i).getScheduleStates();
		}

		if(printResults)
		{
			for(int i=0; i<NUMTELESCOPES; i++)
				results[i].setSchedule(scheduler.getSchedule(i));
			printResults(props);
		}
		
		if(!showGui)
			return;
/*
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
 */
	}

	/*
	public List<Connection> getPath()
	{
		return scheduler.getFinalPath();
	}
	 */

	public void startSimulationClock(String property) 
	{
		Date startDate = Utilities.stringToDate(property);
		Clock.getSimulationClock().startAt(startDate);
	}
	
	public void printResults(Properties props)
	{
		for(int i=0; i<NUMTELESCOPES; i++){
			ResultFileWriter fw = new ResultFileWriter(results[i], props);
			fw.writeResults(results[i]);
			fw.closeWriter();
		}
	}

	public Results[] getResults() {
		return results;
	}
}
