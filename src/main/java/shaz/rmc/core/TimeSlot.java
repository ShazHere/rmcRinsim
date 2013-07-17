/**
 * 
 */
package shaz.rmc.core;

import org.joda.time.DateTime;

import rinde.sim.core.graph.Point;
import static com.google.common.base.Preconditions.checkArgument;

//import shaz.rmc.core.domain.Location;

/**
 * @author Shaza
 *
 */
public class TimeSlot {
	private DateTime startTime;
	private DateTime endTime;
	
//	boolean hasLocationAtStartTime;
//	private Point locationAtStartTime;
//	private ProductionSite productionSiteAtStartTime;
	
//public ProductionSite getProductionSiteAtStartTime() { //there should be no separate setter for productionSite since i
//	//it should be setted at the time when LocationAtStartTime is Set.
//		return productionSiteAtStartTime;
//	}

public TimeSlot() {
		
	}
	public TimeSlot(DateTime startTime, DateTime endTime) {
		super();
		if (startTime != null && endTime != null)
			checkArgument(startTime.compareTo(endTime) <= 0, "StartTime is greater than end time");
		this.startTime = startTime;
		this.endTime = endTime;
//		this.hasLocationAtStartTime = false;
//		this.locationAtStartTime = null;
	}
//	public TimeSlot(DateTime startTime, DateTime endTime, Point loc, ProductionSite pProductionSite) {
//		super();
//		if (startTime != null && endTime != null)
//			checkArgument(startTime.compareTo(endTime) <= 0, "StartTime is greater than end time");
//		this.startTime = startTime;
//		this.endTime = endTime;
////		this.hasLocationAtStartTime = true;
////		this.locationAtStartTime = loc;
////		productionSiteAtStartTime = pProductionSite;
//	}
	
//	public boolean isHasLocationAtStartTime() {
//		return hasLocationAtStartTime;
//	}
//	private void setHasLocationAtStartTime(boolean hasLastLocation) {
//		this.hasLocationAtStartTime = hasLastLocation;
//	}

//	public boolean setLocationAtStartTime (Point loc, ProductionSite pProductionSite) {
//		setHasLocationAtStartTime(true);
//		productionSiteAtStartTime = pProductionSite;
//		locationAtStartTime = loc;
//		return true;
//	}
//	public Point getLocationAtStartTime () {
//		return this.locationAtStartTime;
//	}
	public DateTime getStartTime() {
		return startTime;
	}
	public DateTime getEndTime() {
		return endTime;
	}
	public void setStartTime(DateTime start) {
		this.startTime = start;
	}
	public void setEndtime(DateTime end) {
		this.endTime = end;
	}
	
	/**
	 * @param compareWith
	 * @return 0 if both object values are equal and 1 if they are not equal
	 */
	public int compareTo (TimeSlot compareWith) {
		if (this.startTime.compareTo(compareWith.getStartTime()) == 0 && this.endTime.compareTo(compareWith.getEndTime()) == 0)
			return 0;
		else
			return 1;
		
	}
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("TimeSlot<start,end> = ");
		sb.append("<" + startTime + ", " + endTime + ">");
		return sb.toString();
	}
}
