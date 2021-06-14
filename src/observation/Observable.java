package observation;

import java.util.GregorianCalendar;

public interface Observable 
{

	public String getName();
	public int getExpectedIntegrationTime();
	public void setExpectedIntegrationTime(double time);
	public double getActualTimeObserved();
	//nominal time equivalence of the time actually spent which is moderated
	//by the strength of the signal
	public double getNominalTimeObserved();
	public void addNominalTimeObserved(double time);
	public boolean needsObserving();
	//this records the actual time, which may be shorter or longer
	//depending on the strength of the signal
	public void setActualTimeObserved(double time);
	public boolean isObservationComplete();
	//public void setObservationComplete(boolean observationComplete);
	public int getScintillationTimescale();
	public double getRemainingIntegrationTime();
//	public void addNominalTimeObserved(double elapsed);
	public void addActualTimeObserved(double elapsed);
	public void setDontLookTime();
	public void setDontLookTime(long seconds);
	public boolean doNotLookYet(GregorianCalendar other);
	public void incrementAttempts();
	public int getAttempts();
	public void setScintillationTimescale(int scintillationTimescale);
}
