/**
 * 
 */
package shaz.rmc.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.joda.time.Duration;


/**
 * @author Shaza
 *
 */
public class Utility {
	
	public static ArrayList<TimeSlot> getAvailableSlots(ArrayList<TruckScheduleUnit> pSchedule , ArrayList<TimeSlot> availableSlots, final TimeSlot truckTotalTimeRange) {
		if (pSchedule.isEmpty()) {
			checkArgument(availableSlots.size() == 1 , true);
			availableSlots.get(0).setStartTime(new DateTime(truckTotalTimeRange.getStartTime()));
			return availableSlots;
		}
		else
		{
			Collections.sort(pSchedule, new Comparator<TruckScheduleUnit>(){
		        public int compare( TruckScheduleUnit a, TruckScheduleUnit b ){
		            return (int)((a.getTimeSlot().getStartTime().getMillis()/1000)- (b.getTimeSlot().getStartTime().getMillis()/1000));
		        }
			});
			//checkArgument(availableSlots.size()>0, "T Unexpectedly Available slots empty"); //there shud b one slot any way..
			TimeSlot av = new TimeSlot();
			av.setLocationAtStartTime(availableSlots.get(0).getLocationAtStartTime(), availableSlots.get(0).getProductionSiteAtStartTime());
			availableSlots.clear();
			DateTime initialTime = truckTotalTimeRange.getStartTime();
			for (TruckScheduleUnit u: pSchedule) {
				if (initialTime.compareTo(u.getTimeSlot().getStartTime()) < 0) {
					Duration d = new Duration (initialTime, u.getTimeSlot().getStartTime());
					if (d.compareTo(new Duration(2l*60l*60l*1000l)) >= 0) { 
						av.setStartTime(initialTime);
						av.setEndtime(u.getTimeSlot().getStartTime()); //chk start time or end time are inclusive or not
						availableSlots.add(av);
						av = new TimeSlot();
						//System.out.println("added slot = " + av.toString());
						//initialTime = u.getTimeSlot().getEndTime();
					}
				}
				initialTime = u.getTimeSlot().getEndTime().plusMinutes(1);
			}
			//after last unit
			//initialTime = pSchedule.get(pSchedule.size()-1).getTimeSlot().getEndTime();
			if (initialTime.compareTo(truckTotalTimeRange.getEndTime()) < 0) {
				Duration d = new Duration (initialTime, truckTotalTimeRange.getEndTime());
				if (d.compareTo(new Duration(2*60*60*1000)) >= 0) {
					//System.out.println("last slot added");
					av.setStartTime(initialTime);
					av.setEndtime(truckTotalTimeRange.getEndTime()); //chk start time or end time are inclusive or not
					availableSlots.add(av);
				}
			}
			return availableSlots;
		}
	}
}
