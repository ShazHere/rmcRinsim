/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;

import com.rits.cloning.Cloner;

import shaz.rmc.core.Reply;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;

/**
 * @author Shaza
 *
 */
public class DeliveryTruckSchedule {
	
	protected final ArrayList<TruckScheduleUnit> schedule; //schedule that is used in truck agent as well as for intentions till 17June, 2013

	DeliveryTruckSchedule(ArrayList<TruckScheduleUnit> pSch) {
		this.schedule = pSch;
	}
	/**
	 * to check if newSch (explored by expAnt) still contains all the elements of b.schedule and is a valid schedule for truck
	 * @param newSch to be compared with actual b.schedule of truck
	 * @return true if all b.schedule units are still in newSch, and false if not.
	 */ //TODO check do i need to compare sizes of newSch and b.schedule? to validate that newSch also contains new explored stuff
	protected boolean scheduleStillValid( ArrayList<TruckScheduleUnit> newSch) {
		ArrayList<TruckScheduleUnit> oldSch = this.schedule;
		//if (isNewScheduleConsistent(newSch) == false)
			//return false;
		if (oldSch.isEmpty()) //if already existing schedule is empty, then what ever new schedule is, its ok for truck
			return true;
		boolean foundMatch = false;
		for (TruckScheduleUnit u : oldSch) {
			foundMatch = false;
			for (TruckScheduleUnit newUnit: newSch) {
				if (newUnit instanceof TruckDeliveryUnit && u instanceof TruckDeliveryUnit) {
					if (((TruckDeliveryUnit)u).getDelivery().equals(((TruckDeliveryUnit)newUnit).getDelivery())) {
						foundMatch = true;
						break;
					}
				}
				else if (newUnit instanceof TruckTravelUnit && u instanceof TruckTravelUnit) {
					if (newUnit.equals(u)){
						foundMatch = true;
						break;
					}
				}
			}
			if (!foundMatch)
				return false;
		}
		if (foundMatch && (newSch.size() > oldSch.size()))
			return true;
		else 
			return false;
	}
	//TODO:There is some bug in exploration ants, due to which for a bigger2 file, a schedule was generated with overalpping units. So this method is 
	//to ensure that such schedule isn't selected as bestAnt. But infact, exp ants shouldn't have any bug at all!Remove the bug!!..23/07/2013...Update= bug removed still keeping method (31/07/2013)
	private boolean isNewScheduleConsistent(ArrayList<TruckScheduleUnit> newSch) {
		if (newSch.size() == 1) //if size is only 1 then one unit is always consistent with itself
			return true;
		for (int i = 0; i< newSch.size()-1; i++) {
			if (newSch.get(i).getTimeSlot().getEndTime().compareTo(newSch.get(i+1).getTimeSlot().getStartTime()) > 0
					){
				return false;
			}
		}
		return true;
	} 
	protected boolean isEmpty() {
		return this.schedule.isEmpty();
	}
	/**
	 * @return the schedule
	 * TODO: consider returning defensive copy?
	 */
	protected ArrayList<TruckScheduleUnit> getSchedule() {
		return schedule;
	}
	/**
	 * @param newUnit
	 * Add element in the schedule and sorts it as well.
	 */
	protected void add(TruckScheduleUnit newUnit) {
		this.schedule.add(newUnit);
		Utility.sortSchedule(schedule);
	}
	/**
	 * @param tdu
	 * removes from schedule truckDeliveryUnit and associated TruckTravelUnits.
	 * TODO: can write independent tests for it..
	 */
	protected void remove(TruckDeliveryUnit tdu) {
		if (schedule.size() == 1){
			schedule.remove(tdu);
			checkArgument(schedule.isEmpty(), true);
			return;
		}
		//means schedule.size>1
		int unitIndex = schedule.indexOf(tdu);
		if (unitIndex == 0) {
			checkArgument(schedule.get(1) instanceof TruckTravelUnit == true, true);
			schedule.remove(1); //travel Unit after tdu.
			schedule.remove(tdu);
		}
		else if (unitIndex > 0){
			if (unitIndex == schedule.size()-1){
				checkArgument(schedule.get(unitIndex-1) instanceof TruckTravelUnit == true, true);
				schedule.remove(unitIndex-1); //travel Unit before tdu.
				schedule.remove(tdu);
				return;
			}
			else {
				checkArgument(schedule.get(unitIndex-1) instanceof TruckTravelUnit == true, true);
				TruckTravelUnit beforeTdu = (TruckTravelUnit) schedule.get(unitIndex-1);
				checkArgument(schedule.get(unitIndex+1) instanceof TruckTravelUnit == true, true);
				TruckTravelUnit afterTdu = (TruckTravelUnit) schedule.get(unitIndex+1);
				schedule.remove(beforeTdu);
				schedule.remove(afterTdu);
				schedule.remove(tdu);
			}	
		}
		Utility.sortSchedule(schedule);	
	}
	protected int size() {
		return this.schedule.size();
	}
	/**
	 * to check if the current truck unit overlaps with any unit already existing in the truck's schedule
	 * @param un The schedule that need to be checked with existing schedule of truck
	 * @return true if there is overlap with schedule, false if there is no overlap.
	 */ //TODO add test cases to test  TODO a lot of cases are yet not checked
	protected boolean isOverlapped(TruckScheduleUnit un) {
		for (TruckScheduleUnit u : schedule) {
			if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getStartTime()) >= 0) {
				if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getEndTime()) <= 0)
					return true; //if overlap with any unit, then return true
			}
			else { //means startTime is less
				if (un.getTimeSlot().getEndTime().compareTo(u.getTimeSlot().getStartTime()) >= 0)
					return true; //overlap exists so return true
			}
		}
		return false; //found no overlap with any unit so return false
	}
	
	/**
	 * The schedule unit already exists in trucks schedule?
	 * @param un
	 * @return
	 */	//TODO add test cases to test
	protected boolean alreadyExist(TruckScheduleUnit un) {
		for (TruckScheduleUnit u : schedule) { 
			if (u instanceof TruckDeliveryUnit && un instanceof TruckDeliveryUnit){
				if(((TruckDeliveryUnit)u).getDelivery().equals(((TruckDeliveryUnit)un).getDelivery())) //is it oke, or check further details inside delivery?
					return true;
			}
			else if (u instanceof TruckTravelUnit && un instanceof TruckTravelUnit){// u instanceof TruckTravelUnit 
				if (u.getTimeSlot().compareTo(un.getTimeSlot()) == 0) //may be check with start time and end time seperately??
					return true;
			} 
		}
		return false;
	}
	protected ArrayList<TruckScheduleUnit> makeOriginalSchedule(ArrayList<TruckScheduleUnit> schedule) {
		ArrayList<TruckScheduleUnit> fullSchedule = new ArrayList<TruckScheduleUnit>();
		final Cloner cl = Utility.getCloner();
		for (TruckScheduleUnit u : schedule) {
			TruckScheduleUnit newUnit = cl.deepClone(u);		
			fullSchedule.add(newUnit);
		}		
		return fullSchedule;
	}
	protected ArrayList<communicateAbleUnit> makeCommunicateAbleSchedule(ArrayList<TruckScheduleUnit> schedule) {
		checkArgument(schedule.isEmpty() == false, true);
		ArrayList<communicateAbleUnit> communicateAbleSchedule = new ArrayList<communicateAbleUnit>();
		final Cloner cl = Utility.getCloner();
		for (TruckScheduleUnit u : schedule) {
			if (u instanceof TruckDeliveryUnit) { // no need to have any communicatable unit for a travel unit..though travel unit should remain part of schedule..
				TruckDeliveryUnit newUnit = cl.deepClone((TruckDeliveryUnit)u);
				communicateAbleUnit cUnit = new communicateAbleUnit(newUnit, Reply.NO_REPLY, Reply.NO_REPLY, false);
				if (alreadyExist(u) == true)
					cUnit.setAddedInTruckSchedule(true);
				communicateAbleSchedule.add(cUnit);
			}
		}
		checkArgument (communicateAbleSchedule.isEmpty() == false, true);
		return communicateAbleSchedule;
	}
}
