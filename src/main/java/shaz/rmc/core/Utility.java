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

import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;


/**
 * @author Shaza
 *
 */
public class Utility {
	
	/**
	 * @param pSchedule the schedule units between which slots need to be found
	 * @param availableSlots the reference of found arrayList of slots
	 * @param truckTotalTimeRange the range (boundries) between which slots should be found
	 * @return An array of available slots of more than GLOBALPARAMETERS.AVAILABLE_SLOT_SIZE found between the units of pSchedule. 
	 * Also param availableSlot contains the same returning arrayList 
	 */
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
			//for (TruckScheduleUnit u: pSchedule) {
			for (int i = 0; i< pSchedule.size(); i++) {
				if (initialTime.compareTo(pSchedule.get(i).getTimeSlot().getStartTime()) < 0) {
					Duration d = new Duration (initialTime, pSchedule.get(i).getTimeSlot().getStartTime());
					if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {  //more than 2 hour slot
						av.setStartTime(initialTime);
						av.setEndtime(pSchedule.get(i).getTimeSlot().getStartTime()); //chk start time or end time are inclusive or not
						availableSlots.add(av);
						av = new TimeSlot();
//						if (i > 0) 
//							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
						//System.out.println("added slot = " + av.toString());
						//initialTime = u.getTimeSlot().getEndTime();
					}
				}
				initialTime = pSchedule.get(i).getTimeSlot().getEndTime().plusMinutes(1);
			}
			//after last unit
			//initialTime = pSchedule.get(pSchedule.size()-1).getTimeSlot().getEndTime();
			if (initialTime.compareTo(truckTotalTimeRange.getEndTime()) < 0) {
				Duration d = new Duration (initialTime, truckTotalTimeRange.getEndTime());
				if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {
					//System.out.println("last slot added");
					av.setStartTime(initialTime);
					av.setEndtime(truckTotalTimeRange.getEndTime()); //chk start time or end time are inclusive or not
					availableSlots.add(av);
				}
			}
			if (!availableSlots.isEmpty()) {
				
			}
			return availableSlots;
		}
	}
}
