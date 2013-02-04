package shaz.rmc.core.domain;

import java.io.Serializable;

import org.joda.time.Duration;

public class ConstructionYard implements Location, Serializable {

	private final String id;
	private final Duration waitingDuration;
	
	
	//private ArrayList<DateTime[]> availabilityList; 
	
	public ConstructionYard(final String id, final Duration waitingDuration) {
		this.id = id;
		this.waitingDuration = waitingDuration;
		
		//availabilityList = new ArrayList<DateTime[]>();
	}
	
	public String getId() {
		return id;
	}
	
	public Duration getWaitingDuration() {
		return waitingDuration;
	}
	
	/*public ArrayList<DateTime[]> getAvailabilityList() {
		return availabilityList;
	}
*/
	@Override
	public String toString() {
		return getClass().getSimpleName()+"[id="+id+",waitingDuration="+waitingDuration+"]";
	}
}
