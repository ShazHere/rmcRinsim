/**
 * 
 */
package shaz.rmc.core;

import org.joda.time.DateTime;

import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;

/**
 * @author Shaza
 * @date 15/07/2013
 * I think it should be immutable ,but at the moment since timeSlot is not immutable, AvailableSlot is also not immutable
 * The purpose is to use it as an arrayList object to represent available slots in the schedule of truckAgent.
 */
public class AvailableSlot {
	
	private  TimeSlot timeSlot;

	private  Agent lastOrderVisited; //could be null, if schedule=empty or available slot is before an existing schedule unit
	private Agent pS4NextOrderVisited; //could be null, if schedule=empty or available slot is after an existing schedule unit
	
	public AvailableSlot() {
		super();
		timeSlot = new TimeSlot();
	}
	public AvailableSlot(TimeSlot timeSlot) {
		this.timeSlot = timeSlot;
		this.lastOrderVisited= null;
		this.pS4NextOrderVisited = null;
	}

	public AvailableSlot(TimeSlot timeSlot, Agent lastOrderVisited, Agent pS4NextOrderVisited) {
		super();
		this.timeSlot = timeSlot;
		this.lastOrderVisited = lastOrderVisited;
		this.pS4NextOrderVisited = pS4NextOrderVisited;
	}
	public DateTime getStartTime() {
		return timeSlot.getStartTime();
	}
	public DateTime getEndTime() {
		return timeSlot.getEndTime();
	}
	public void setStartTime(DateTime start) {
		timeSlot.setStartTime(start) ;
	}
	public void setEndtime(DateTime end) {
		timeSlot.setEndtime(end);
	}
	public Agent getLastOrderVisited() {
		return lastOrderVisited;
	}
	public void setLastOrderVisited(Agent lastOrderVisited) {
		this.lastOrderVisited = lastOrderVisited;
	}

	public Agent getPS4NextOrderVisitedd() {
		return pS4NextOrderVisited;
	}

	public void setpS4NextOrderVisited(Agent pPS4NextOrderVisited) {
		this.pS4NextOrderVisited = pPS4NextOrderVisited;
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(timeSlot.toString()).append("\n");
		if (lastOrderVisited == null)
			sb.append("LastOrderVisited=").append("null").append(", ");
		else
			sb.append("LastOrderVisited=").append(((OrderAgentInitial)lastOrderVisited).getOrder().getId()).append(", ");
		if (pS4NextOrderVisited == null)
			sb.append("PS4NextOrderVisited=").append("null").append("\n");
		else
			sb.append("PS4NextOrderVisited=").append(((ProductionSiteInitial)pS4NextOrderVisited).getStation().getId()).append("\n");
		return sb.toString();
	}
}
