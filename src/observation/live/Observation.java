package observation.live;


import java.util.Properties;

import observation.Observable;
import observation.Telescope;
import observation.interference.SkyState;
import simulation.Clock;



public class Observation 
{
	private Environment environment = null;
	private ObservationState currentState;
	//if the integration is longer than the threshold, check scintillation repeatedly
	private final double scintIntervalLimit;
	private Telescope telescope;
	//This is for a five minute sample until the signal to noise ratio can be calculated
	private static final int LEADTIME = 300;

	private Clock clock;
	
	public Observation(Properties props, Telescope scope, SkyState state, Clock c)
	{
		telescope = scope;
		clock = c;
		double rayleighMax = Double.parseDouble(props.getProperty("rayleigh_threshold_max"));
		double rayleighMin = Double.parseDouble(props.getProperty("rayleigh_threshold_min"));
		this.environment =new Environment(rayleighMax, rayleighMin, state, c);
		scintIntervalLimit= Double.parseDouble(props.getProperty("scint_timescale_limit"));
	}
	
	public void observe (ObservationState state)
	{
		currentState = state;
		clock.advanceBy((int)state.getLinkToHere().getFinalSlewTime());
		Observable observable = currentState.getCurrentObservable();
	//	String name = observable.getName();
	//	System.out.println(observable.getName());
		
		//List<Satellite> sats = environment.getSatellitesThatMightInterfere(state, telescope, Clock.getScheduleClock().getTime());
	//	currentState.setPotentiallyInterfering(sats);
		// this is pulsar-specific; can be looked up but is specified uniform randomly here
		// should be multiplied by integration time, but that would make no difference to the outcome

		if (observable.getScintillationTimescale() > scintIntervalLimit * observable.getExpectedIntegrationTime())
			//scintillation changes slowly, need to check only after the first five minutes

			checkScintillationOnce();
		else
		{
			//scintillation changes rapidly, need to re-check repeatedly
			environment.checkScintillationRepeatedly(currentState, telescope);
		}
		currentState.setEndTime(telescope.getLocation(), clock);
	}
	
	
	private void checkScintillationOnce()
	{
		double rayleigh = environment.getScintillation();

		switch (environment.getScintillationRange(rayleigh)) 
		{
		case Environment.SCINTILLATION_RANGE_HIGH:
			//high means visibility is so good, observation time can be shortened (rayleigh above one)			
			environment.applyConditionsHighScint(currentState, telescope, rayleigh);
			break;
		case Environment.SCINTILLATION_RANGE_MEDIUM:
			//medium visibility means observation can be completed in nominal time
//			observeWhileCheckingConditions(observationTime, 1.0, "Scint medium; finished in actual time.", observationTime);
			environment.applyConditions(currentState, telescope);
			break;
		case Environment.SCINTILLATION_RANGE_LOW:

				currentState.setIntegrationTime(LEADTIME);
				currentState.setAborted("Aborted: Scintillation too low within first five minutes.", 0);
				currentState.setObservationResults(ObservationState.OBSERVATION_ABORTED_SCINT_WEAK, ObservationState.OBSERVATION_INTERRUPTION_NONE);
			
			//time spent on the object that was not useful; astronomers spend 5 minutes trying		
			clock.advanceBy(LEADTIME);
			currentState.getCurrentObservable().setDontLookTime(clock);
//			Target t = (Target)currentState.getCurrentTarget();
//			System.out.println("High Scintillation altitude after obs "+t.getHorizonCoordinates(telescope.getLocation(), Clock.getScheduleClock().getTime()).getAltitude());

			break;
		}
	}

	
//	private double getMaxObservationTimeIfObjectSets(double projectedObservationTime)
//	{
//		Target target = (Target)currentState.getCurrentTarget();	
//		GregorianCalendar gcEnd = Utilities.cloneDate(Clock.getScheduleClock().getTime());	
//		gcEnd.add(GregorianCalendar.SECOND, (int) Math.ceil(projectedObservationTime));
//		HorizonCoordinates hc = target.getHorizonCoordinates(telescope.getLocation(), gcEnd);
//	
//		if(hc.getAltitude() > 0)
//			return projectedObservationTime;
//		
//		// how long will it be up for - decrement by five minutes at a time
//		long addition = (long) Math.ceil(projectedObservationTime)*1000;
//		addition -= 300000;
//		
//		do{
//			gcEnd.setTimeInMillis(Clock.getScheduleClock().getTime().getTimeInMillis() + addition);
//			hc = target.getHorizonCoordinates(telescope.getLocation(), gcEnd);
//			if(hc.getAltitude() > 0)
//				return addition;
//			addition -= 300000;
//		}while(addition > 0);
//		//if we got this far, the object is not up from the start
//		return 0;
//
//		
//		
//
////			GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
////			gc.setTimeInMillis(time);
////			System.out.println(Utilities.millisecondsToDateFormat(time));
////			HorizonCoordinates hc = target.getHorizonCoordinates(telescope.getLocation(), gc);
////			System.out.println("Object sets "+Utilities.getDateAsString(gc.getTime())+" alt "+hc.getAltitude());
////			//+ " vs expected "+currentState.getCurrentObservable().getExpectedIntegrationTime());
////		
//			
//	}
	
}
