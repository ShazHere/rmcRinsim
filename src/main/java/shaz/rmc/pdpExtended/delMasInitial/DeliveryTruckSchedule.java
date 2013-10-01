/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.rits.cloning.Cloner;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.Agent;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.ScheduleHelper;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckCostForDelivery;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.TruckStrategyCoalition;

/**
 * @author Shaza
 *
 */
public class DeliveryTruckSchedule {
	
	private final ArrayList<TruckScheduleUnit> schedule; //schedule that is used in truck agent as well as for intentions till 17June, 2013
	private ArrayList<TruckScheduleUnit> practicalSchedule; //should contain only ACCEPTED units and travelUnits (accordingly)
	private final Map<Delivery, Reply> unitStatus;
	private final ArrayList<TruckScheduleUnit> removedUnits;

	DeliveryTruckSchedule(ArrayList<TruckScheduleUnit> pSch) {
		this.schedule = pSch;
		practicalSchedule = new ArrayList<TruckScheduleUnit>();
		unitStatus = new LinkedHashMap<Delivery, Reply>();
		removedUnits = new ArrayList<TruckScheduleUnit>();
	}
	/**
	 * to check if newSch (explored by expAnt) still contains all the elements of b.schedule and is a valid schedule for truck
	 * @param newSch to be compared with actual b.schedule of truck
	 * @return true if all b.schedule units are still in newSch, and false if not.
	 */ 
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
					//if (newUnit.equals(u)){ some how not working
					if (newUnit.getStartLocation().equals(u.getStartLocation()) && newUnit.getEndLocation().equals(u.getEndLocation())
							&& ((TruckTravelUnit)newUnit).getTravelTime().equals(((TruckTravelUnit)u).getTravelTime())) {
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
		ArrayList<TruckScheduleUnit> defensiveCopyArray = new ArrayList<TruckScheduleUnit>();  
        defensiveCopyArray.addAll(schedule);  
        return defensiveCopyArray;  
		//return schedule;
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
	 * @param newUnit
	 * Add element in the schedule and sorts it as well.
	 */
	protected void add(TruckDeliveryUnit newUnit, Reply orderReply) {
		checkArgument(alreadyExist(newUnit) == false, true);
		unitStatus.put(newUnit.getDelivery(), orderReply);
		this.add(newUnit);
		
	}
	/**
	 * @param tdu
	 * removes from schedule truckDeliveryUnit and associated TruckTravelUnits.
	 */
	protected void remove(TruckDeliveryUnit tdu, DeliveryTruckInitial rmcTruck, DateTime currTime) {
		checkArgument(getIndexOf(tdu, this.schedule)> -1 || (getIndexOf(tdu, this.schedule) == -1 && getIndexOf(tdu, removedUnits) > -1), true);
		if (getIndexOf(tdu, removedUnits) > -1)
			return;
		if (schedule.size() == 1){
			schedule.remove(getIndexOf(tdu, this.schedule));
			unitStatus.remove(tdu.getDelivery());
			removedUnits.add(tdu);
			//makePracticalSchedule(rmcTruck);
			beforeReturningRemoveMethod(rmcTruck, currTime);
			checkArgument(schedule.isEmpty(), true);
			checkArgument(practicalSchedule.isEmpty(), true);
			return;
		}
		//means schedule.size>1
		int unitIndex = getIndexOf(tdu, this.schedule);//
		checkArgument(unitIndex != -1, true);
		unitStatus.remove(tdu.getDelivery());
		if (unitIndex == 0) {
			checkArgument(schedule.get(1) instanceof TruckTravelUnit == true, true);
			removedUnits.add(schedule.get(1));
			removedUnits.add(schedule.get(getIndexOf(tdu, this.schedule)));
			schedule.remove(1); //travel Unit after tdu.
			schedule.remove(getIndexOf(tdu, this.schedule));
		}
		else if (unitIndex > 0){
			if (unitIndex == schedule.size()-1){
				checkArgument(schedule.get(unitIndex-1) instanceof TruckTravelUnit == true, true);
				removedUnits.add(schedule.get(unitIndex-1));
				removedUnits.add(schedule.get(getIndexOf(tdu, this.schedule)));
				schedule.remove(unitIndex-1); //travel Unit before tdu.
				schedule.remove(getIndexOf(tdu, this.schedule));
				beforeReturningRemoveMethod(rmcTruck, currTime);
				return;
			}
			else {
				checkArgument(schedule.get(unitIndex-1) instanceof TruckTravelUnit == true, true);
				TruckTravelUnit beforeTdu = (TruckTravelUnit) schedule.get(unitIndex-1);
				checkArgument(schedule.get(unitIndex+1) instanceof TruckTravelUnit == true, true);
				TruckTravelUnit afterTdu = (TruckTravelUnit) schedule.get(unitIndex+1);
				removedUnits.add(schedule.get(getIndexOf(beforeTdu, this.schedule)));
				removedUnits.add(schedule.get(getIndexOf(afterTdu, this.schedule)));
				removedUnits.add(schedule.get(getIndexOf(tdu, this.schedule)));
				schedule.remove(getIndexOf(beforeTdu, this.schedule));
				schedule.remove(getIndexOf(afterTdu, this.schedule));
				schedule.remove(getIndexOf(tdu, this.schedule));
			}	
		}
		beforeReturningRemoveMethod(rmcTruck, currTime);	
	}
	/**
	 * @param rmcTruck
	 */
	private void beforeReturningRemoveMethod(DeliveryTruckInitial rmcTruck, DateTime currTime) {
		//addExtraTravelUnitAccordingToCurrentLocation(rmcTruck, currTime);
		adjustTruckSchedule(rmcTruck);
		makePracticalSchedule(rmcTruck, currTime);
		Utility.sortSchedule(schedule);
	}
	
	/**
	 * @param rmcTruck
	 * @param currTime
	 * This method is added since there is possibility that a truck is on the way to delivery(Oxdi), or doing delivery(Oxdi), 
	 * and suppose that Oxdi is canceled because of failure of Oxd(i-1). Then there might be a travel unit required to reach 
	 * the startLocation of trucks next schedule unit. 
	 *  
	 */
	private void addExtraTravelUnitAccordingToCurrentLocation(DeliveryTruckInitial rmcTruck, DateTime currTime) {
		int index = -1;
		if (schedule.isEmpty())
			return;
		//for (TruckScheduleUnit tsu: schedule) {
		for (int i = 0; i< schedule.size(); i++) {
			if (i < schedule.size() -1) {
				if (currTime.compareTo(schedule.get(i).getTimeSlot().getStartTime()) >= 0  && currTime.compareTo(schedule.get(i+1).getTimeSlot().getStartTime()) < 0) {
					index++;
					break;
				}
			}
			if (currTime.compareTo(schedule.get(i).getTimeSlot().getStartTime()) < 0 && index == -1)
				break;
			index++;
		}
		if (index == 0) { //then its start location and current location shud be start location  also shedule.get(index)'s start
			//location shud be trck start Location if any is NO, then add a travel unit form current location to start location of 
			//schedule.get(index)
			if (!rmcTruck.getRoadModel().getPosition(rmcTruck).equals(schedule.get(index).getStartLocation()) 
					|| !rmcTruck.getStartLocation().equals(schedule.get(index).getStartLocation())) {
				TruckTravelUnit ttu = makeTravelUnitFromCurrentToI(rmcTruck, index);
				this.add(ttu);
			}
		}
		else if (index > 0 && index < schedule.size()-1 && (!schedule.get(index-1).getEndLocation().equals(rmcTruck.getRoadModel().getPosition(rmcTruck)) 
				|| !schedule.get(index-1).getEndLocation().equals(schedule.get(index).getStartLocation()))){
			 //schedule.get(index -1).getEndLocation shud b == current location and  schedule.get(index -1).getEndLocation
		//shud be == schedule.get(index). start location.
			TruckTravelUnit ttu = makeTravelUnitFromCurrentToI(rmcTruck, index);
			this.add(ttu);
		}
		
	}
	
	
	private TruckTravelUnit makeTravelUnitFromCurrentToI( DeliveryTruckInitial rmcTruck, int index) {
		Double distance = Point.distance(rmcTruck.getRoadModel().getPosition(rmcTruck), schedule.get(index).getStartLocation()); //TODO replace by getPStoOrDuration method
		Duration travelDist = new Duration((long)((distance/rmcTruck.getSpeed())*60*60*1000));
		TruckTravelUnit reqUnit = new TruckTravelUnit(rmcTruck, Utility.getTravelUnitTimeSlot(travelDist, schedule.get(index).getTimeSlot().getStartTime(), true),
				rmcTruck.getRoadModel().getPosition(rmcTruck), schedule.get(index).getStartLocation(), travelDist) ; //createNewTravelUnit
		return reqUnit;
	}
	/**
	 * @param tdu
	 * @return index of the tdu unit in this.schedule.
	 * NOTE: initially used 'schedule.indexOf(tdu)' but the default indexOf was not behaving correctly
	 */
	private int getIndexOf(TruckDeliveryUnit tdu, ArrayList<TruckScheduleUnit> sch) {
		int index = 0;
		for (TruckScheduleUnit tsu : sch) {
			if (tsu instanceof TruckDeliveryUnit) {
				if (((TruckDeliveryUnit)tdu).getDelivery().equals(((TruckDeliveryUnit)tsu).getDelivery())
						//&& ((TruckDeliveryUnit)tdu).getLagTime().equals(((TruckDeliveryUnit)tsu).getLagTime()) lag time should not be compared, since OrderPlanInformer ants
						//when inform truck to remove a tdu, they always fill lagtime = 0
						&& ((TruckDeliveryUnit)tdu).getWastedConcrete()==((TruckDeliveryUnit)tsu).getWastedConcrete())
					return index;
			}
			index++;
		}
		return -1;
	}
	/**
	 * @param tdu
	 * @return index of the tdu unit in this.schedule.
	 * NOTE: initially used 'schedule.indexOf(tdu)' but the default indexOf was not behaving correctly
	 */
	private int getIndexOf(TruckTravelUnit tdu, ArrayList<TruckScheduleUnit> sch) {
		int index = 0;
		for (TruckScheduleUnit tsu : sch) {
			if (tsu instanceof TruckTravelUnit) {
				if (((TruckTravelUnit)tdu).getTravelTime().equals(((TruckTravelUnit)tsu).getTravelTime())
						&& tdu.getStartLocation().equals(tsu.getStartLocation())
						&& tdu.getEndLocation().equals(tsu.getEndLocation()))
					return index;
			}
			index++;
		}
		return -1;
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
				//if (u.getTimeSlot().compareTo(un.getTimeSlot()) == 0) //may be check with start time and end time seperately??
				if (u.getStartLocation().equals(un.getStartLocation()) && u.getEndLocation().equals(un.getEndLocation()) 
						&& ((TruckTravelUnit)u).getTravelTime().equals(((TruckTravelUnit)un).getTravelTime())
						&& ((TruckTravelUnit)u).getTimeSlot().getStartTime().equals(((TruckTravelUnit)un).getTimeSlot().getStartTime())
						&& ((TruckTravelUnit)u).getTimeSlot().getEndTime().equals(((TruckTravelUnit)un).getTimeSlot().getEndTime()))
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
	
	public ArrayList<TruckScheduleUnit> getPracticalSchedule() {
		return practicalSchedule;
	}
	/**
	 * @param schedule
	 * @param unitStatus
	 * @return the practical schedule containing truckDeliveryUnit with status of ACCEPT only. The travel units are modified/added 
	 * according to the ACCEPTed truckDeliveryUnits.
	 */
	public void makePracticalSchedule(DeliveryTruckInitial rmcTruck, DateTime currTime) {
		if (schedule.isEmpty() || !unitStatus.containsValue(Reply.ACCEPT)) {
			practicalSchedule = new ArrayList<TruckScheduleUnit>();
			return;
		}
		
		checkArgument(isScheduleValid(schedule) == true, true); //comment only for saving time..
		checkArgument(isUnitStatusValid(schedule, unitStatus) == true, true);
		
		ArrayList<TruckScheduleUnit> pracSchedule = new ArrayList<TruckScheduleUnit>();
		final Cloner cl = Utility.getCloner();
		for (TruckScheduleUnit tsu: schedule) //just put ACCEPTED DeliveryUnits
		{
			if (tsu instanceof TruckDeliveryUnit)
				if (unitStatus.get(((TruckDeliveryUnit) tsu).getDelivery()) == Reply.ACCEPT)
					pracSchedule.add(cl.deepClone(tsu));
		}
		pracSchedule = fillTravelUnitsInSchedule(rmcTruck, pracSchedule, cl);
		
		Utility.sortSchedule(pracSchedule);
		for (TruckScheduleUnit tsu: pracSchedule) {//to make sure that a new unit is not added in practical schedule when its time has already been started.
			if (tsu instanceof TruckDeliveryUnit) {
				if (getIndexOf((TruckDeliveryUnit) tsu, this.practicalSchedule) == -1) // means not present in practicalScedule so far
					checkArgument(tsu.getTimeSlot().getStartTime().compareTo(currTime) >0, true);
			}
			else {
				if (getIndexOf((TruckTravelUnit)tsu, this.practicalSchedule) == -1) // means not present in practicalScedule so far
					checkArgument(tsu.getTimeSlot().getStartTime().compareTo(currTime) >0, true);
			}
			//If above two checks are failing then may be further decrease intervals of int ant
		}
		
		this.practicalSchedule = pracSchedule;
	}
	/**
	 * @param rmcTruck
	 * @param anySchedule
	 * @param cl
	 */
	private ArrayList<TruckScheduleUnit> fillTravelUnitsInSchedule(DeliveryTruckInitial rmcTruck, ArrayList<TruckScheduleUnit> anySchedule, final Cloner cl) {
		//fill the travelUnits between  DeliveryUNits of anySchedule
		ArrayList<TruckTravelUnit> toBeAddedTTU = new ArrayList<TruckTravelUnit>();//partially save in this arrayList, otherwise index of Practical schedule gets distrubed during For loop
		for (int i =getFirstIndexToScanSchedule(schedule);i < anySchedule.size()-1; i+=1) { 
			if (getTravelUnitIfExists(schedule, anySchedule.get(i), anySchedule.get(i+1)) != null)
				toBeAddedTTU.add(cl.deepClone(getTravelUnitIfExists(schedule, anySchedule.get(i), anySchedule.get(i+1))));
			else{
				TruckTravelUnit reqUnit = makeTravelUnitAfterI(rmcTruck,anySchedule, i);
				toBeAddedTTU.add(reqUnit);
			}
		}
		if (schedule.get(0) instanceof TruckTravelUnit)
			toBeAddedTTU.add((TruckTravelUnit) cl.deepClone(schedule.get(0)));
		for (TruckTravelUnit ttu: toBeAddedTTU) {
			anySchedule.add(ttu);
		}
		return anySchedule;
	}
	
	
	/**
	 * @param rmcTruck for getting speed
	 * @param pracSchedule
	 * @param i
	 * @return travelUnit after the i^th DeliveryUnit in pracSchedule of rmcTruck
	 */
	private TruckTravelUnit makeTravelUnitAfterI(DeliveryTruckInitial rmcTruck,
			final ArrayList<TruckScheduleUnit> pracSchedule, int i) {
		Double distance = Point.distance(pracSchedule.get(i).getEndLocation(), pracSchedule.get(i+1).getStartLocation());
		Duration travelDist = new Duration((long)((distance/rmcTruck.getSpeed())*60*60*1000));
		TruckTravelUnit reqUnit = new TruckTravelUnit(rmcTruck, Utility.getTravelUnitTimeSlot(travelDist, pracSchedule.get(i+1).getTimeSlot().getStartTime(), true),
				pracSchedule.get(i).getEndLocation(), pracSchedule.get(i+1).getStartLocation(), travelDist) ; //createNewTravelUnit
		return reqUnit;
	}
	/**
	 * @param rmcTruck for getting speed
	 * @param pracSchedule
	 * @param i
	 * @return travelUnit before the i^th DeliveryUnit in pracSchedule of rmcTruck using truck's start location as starting point
	 */
	private TruckTravelUnit makeTravelUnitBeforeI(DeliveryTruckInitial rmcTruck,
			final ArrayList<TruckScheduleUnit> schedule, int i) {
		Double distance = Point.distance(rmcTruck.getStartLocation(), schedule.get(i).getStartLocation()); //TODO replace by getPStoOrDuration method
		Duration travelDist = new Duration((long)((distance/rmcTruck.getSpeed())*60*60*1000));
		TruckTravelUnit reqUnit = new TruckTravelUnit(rmcTruck, Utility.getTravelUnitTimeSlot(travelDist, schedule.get(i).getTimeSlot().getStartTime(), true),
				rmcTruck.getStartLocation(), schedule.get(i).getStartLocation(), travelDist) ; //createNewTravelUnit
		return reqUnit;
	}

	/**
	 * @param schedule
	 * @param first
	 * @param second
	 * @return the travelUnit form schedule that exists between first and second. Else return null.
	 */
	private TruckTravelUnit getTravelUnitIfExists(ArrayList<TruckScheduleUnit> schedule,
			TruckScheduleUnit first,TruckScheduleUnit second) {
		for (TruckScheduleUnit tsu : schedule) {
			if (tsu instanceof TruckTravelUnit) {
				if  (tsu.getStartLocation().equals(first.getEndLocation()) == true && tsu.getEndLocation().equals(second.getStartLocation()))
					if (tsu.getTimeSlot().getStartTime().compareTo(first.getTimeSlot().getEndTime()) >= 0 && tsu.getTimeSlot().getEndTime().compareTo(second.getTimeSlot().getStartTime()) <= 0)
						return (TruckTravelUnit)tsu;
			}
		}
		return null;
	}
	/**
	 * to fix/add travelUnits if required
	 * Should be called if anything is removed from the truckSchedule
	 */
	protected void adjustTruckSchedule(DeliveryTruckInitial rmcTruck) {
		if (schedule.isEmpty())
			return;
		int i; //as index for the for loop
		if (schedule.get(0) instanceof TruckDeliveryUnit){ //to check travel unit in the beginning
			if (!(schedule.get(0).getStartLocation().equals(rmcTruck.getStartLocation()))) {//seems travel Unit required
				schedule.add(makeTravelUnitBeforeI(rmcTruck, schedule, 0));
				Utility.sortSchedule(schedule);
				i = 1;
			}
			else i=0;
		}
		else i = 1;
		while (i< schedule.size()-1) {
			if (schedule.get(i) instanceof TruckDeliveryUnit && schedule.get(i+1) instanceof TruckTravelUnit)
				; // do nothing, because it is fine
			else if (schedule.get(i) instanceof TruckDeliveryUnit && schedule.get(i+1) instanceof TruckDeliveryUnit) { //means a travel unit is required b/w them
				TruckTravelUnit reqUnit = makeTravelUnitAfterI(rmcTruck, schedule, i);
				schedule.add(reqUnit);
			}
			i = i +2;
		}
		Utility.sortSchedule(schedule);
	}
	/**
	 * @param schedule
	 * @param unitStatus
	 * @return true if schedule and unitStatus are consistent with eachother, otherwise false
	 */
	private boolean isUnitStatusValid(ArrayList<TruckScheduleUnit> schedule,
			Map<Delivery, Reply> unitStatus) {
		if (schedule.size() < unitStatus.size())
			return false;
		for(TruckScheduleUnit tsu: schedule){
			if (tsu instanceof TruckDeliveryUnit)
				if (!unitStatus.containsKey(((TruckDeliveryUnit) tsu).getDelivery()))
					return false;
		}
		return true;
	}
	/**
	 * @param schedule
	 * @return true if schedule is a valid schedule. alternating sequence of travelUnit and DeliverUnit. 
	 */
	private boolean isScheduleValid(ArrayList<TruckScheduleUnit> schedule) {
		if (schedule.size() == 1){
			if (schedule.get(0) instanceof TruckDeliveryUnit)
				return true;
			else
				return false;
		}
		int i = getFirstIndexToScanSchedule(schedule);
		while (i< schedule.size()-1) { 
			if (!(schedule.get(i) instanceof TruckDeliveryUnit && schedule.get(i+1) instanceof TruckTravelUnit))
				return false;
			i = i +2;
		}
		return true;
	}
	/**
	 * @param schedule
	 * @return
	 */
	private int getFirstIndexToScanSchedule(
			ArrayList<TruckScheduleUnit> schedule) {
		int i; //as index for the for loop
		if (schedule.get(0) instanceof TruckDeliveryUnit)
			i=0;
		else i = 1;
		return i;
	}
	public void updateUnitStatus(TruckDeliveryUnit tdu, Reply orderReply) {
		if (getIndexOf(tdu, schedule) >= 0)
			unitStatus.put(tdu.getDelivery(), orderReply);	
	}
	
	/**
	 * @param del
	 * @param timeWindow
	 * @param truck
	 * @param currTime
	 * @return cost that could occur if truck takes to make a delivery in actual interested time.
	 * Basic idea is that when a truck fails in a team, other truck try to take the delivery by estimating 
	 * costs of that delivey. They can leave their UNDER_PROCESS & WEAK_ACCEPT. If still can't find then truk could drop
	 * BOOKED delivery if there is time before loading of the BOOKED delivery. 
	 * Cost is found using following mechanism
	 * At actualInterested Time, 
	 * 		if schedule if free then cost = 0
	 * 		if tsu with delivery-> UNDERPROCESS then cost = 1
	 * 		if tsu with delivery-> WEAK_ACCEPT then cost = 2
	 * 		if tsu with delivery-> ACCEPT  then 
	 * 			if minutes before deliveryTime > GlobalParameters.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED 
	 * 				&& deliveryNo-> 0 then cost = 3
	 * 			else if minutes before deliveryTime < GlobalParameters.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED
	 * 				then cost = 4 //but actual delivery didnt' started
	 * 		else cost = 5
	 * 		 
	 *	TODO: multiply cost with no. of disturbed units, coz may be two deliveries are disturbed. 25/08/2013 
	 *	TODO: add cost for capacity match/missmatch for hetrogenious truck. 26/08/2013
	 */
	public TruckCostForDelivery getCostForMakingDelivery(Delivery del, TimeSlot timeWindow, DeliveryTruckInitial truck, DateTime currTime) {
		checkArgument(truck.getStrategy() instanceof TruckStrategyCoalition,true);
		OrderAgentInitial or = (OrderAgentInitial)del.getOrder();
		ProductionSiteInitial ps= getSelectedPS(or);
		
		DateTime actualInterestedTime = ScheduleHelper.getActualInterestedTime(timeWindow.getStartTime(), getPsToOrderDuration(ps, or, truck));
		
		// now check for each cost value starting form least, which ever found return
		for (int cost = 0; cost < 4; cost++) { // at the moment only cost till 3 is handled at Order side
			AvailableSlot currentSlot = getCurrentSlot(cost, actualInterestedTime, currTime, truck, timeWindow, or);
			if (currentSlot == null)
				continue;
			TruckCostForDelivery tcfd = getDelCostIfPossible(cost, actualInterestedTime, timeWindow.getStartTime(), currentSlot, timeWindow.getStartTime(), ps, or, truck, del);
			if (tcfd != null)
				return tcfd;
		}
		return null;
	}
	/**
	 * @param actualInterestedTime
	 * @param currentSlot
	 * @param startTime
	 * @param ps
	 * @param or
	 * @param truck
	 * @return the TruckCostForDelivery object. This method is structured similar to ExpAnt.isInterested(). so if some change is made there, changes should be made here as well.
	 */
	private TruckCostForDelivery getDelCostIfPossible(int deliveryCost, DateTime actualInterestedTime, DateTime deliveryTimeAtOrder, AvailableSlot currentSlot,
			DateTime interestedTime, ProductionSiteInitial ps, OrderAgentInitial or,DeliveryTruckInitial truck, Delivery previousDel) {
		checkArgument(truck.getStrategy() instanceof TruckStrategyCoalition,true);
		if (ScheduleHelper.isInterestedAtExactTime(truck, interestedTime, getPsToOrderDuration(ps, or, truck), currentSlot,actualInterestedTime, ps, or)) {
				return getTcfdWhenInterested(deliveryCost, actualInterestedTime, deliveryTimeAtOrder, currentSlot, ps, or, truck, new Duration(0), previousDel);
		}
		else if (GlobalParameters.LAG_TIME_ENABLE) {//estimate with lag time
			if (ScheduleHelper.isInterestedWithLagTime(truck, interestedTime, getPsToOrderDuration(ps, or, truck), currentSlot,actualInterestedTime, ps, or)) {
				checkArgument( ((new Duration(currentSlot.getStartTime(), currentSlot.getEndTime())).getStandardMinutes() >= (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()), true);						
					Duration currentLagTime = ScheduleHelper.makeLagTime(currentSlot, actualInterestedTime, ps);//TODO: check this, it should be duraton b/w actualInterestedTime and the induced lag time! should calculate with seperate method..17/07/2013
					DateTime currentInterestedTime = actualInterestedTime.plus(currentLagTime);
					return getTcfdWhenInterested(deliveryCost, currentInterestedTime,deliveryTimeAtOrder, currentSlot, ps, or, truck, currentLagTime, previousDel);
			}
		}
		
		return null;
	}
	/**
	 * @param deliveryCost
	 * @param actualInterestedTime
	 * @param deliveryTimeAtOrder 
	 * @param currentSlot
	 * @param ps
	 * @param or
	 * @param truck
	 * @return
	 */
	private TruckCostForDelivery getTcfdWhenInterested(int deliveryCost, DateTime actualInterestedTime,
			DateTime deliveryTimeAtOrder, AvailableSlot currentSlot, ProductionSiteInitial ps, OrderAgentInitial or, DeliveryTruckInitial truck, 
			Duration currentLagTime, Delivery previousDel) {
		checkArgument(truck.getStrategy() instanceof TruckStrategyCoalition,true);
		Delivery.Builder delBuilder = new Delivery.Builder();
		delBuilder.setOrder(or);
		delBuilder.setDeliveryNo(previousDel.getDeliveryNo());
		delBuilder.setTruck(truck);
		delBuilder.setDeliveredVolume(previousDel.getDeliveredVolume()); //TODO: Shouldn't it be accoriding to remainig volume..?
		//int deliveredVolume = pOrderPlan.getRemainingToBookVolume() > (int)(exp.getOriginator().getCapacity()) ? (int)(exp.getOriginator().getCapacity()) :  pOrderPlan.getRemainingToBookVolume()
		//delBuilder.setDeliveredVolume(deliveredVolume);
	
		delBuilder.setWastedVolume(previousDel.getWastedVolume()); //TODO: may hve to change after hetrogenious trucks
		delBuilder.setUnloadingDuration(previousDel.getUnloadingDuration());
		delBuilder.setLoadingDuration(previousDel.getLoadingDuration());
		
		delBuilder.setStationToCYTravelTime(new Duration (getPsToOrderDuration(ps, or, truck)));

		delBuilder.setDeliveryTime(deliveryTimeAtOrder.plus(currentLagTime));
////			if (this.deliveries.size() == 0 ) //means at the moment decesions are for first delivery so ST shud b included
////				{ 
////					//checkArgument(pDeliveryNo == 0, true);
////					delBuilder.setLagTime(this.delayFromActualInterestedTime.plus(this.delayStartTime));
////				}
////			else
////				delBuilder.setLagTime(this.delayFromActualInterestedTime); //no ST added this time
////	
//		delBuilder.setLagTime(exp.getCurrentLagTime());
		
		delBuilder.setLoadingStation((ProductionSite)ps);
		Delivery del = delBuilder.build();
		TimeSlot ts = new TimeSlot(actualInterestedTime, del.getUnloadingFinishTime());
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(truck, ts, del, del.getWastedVolume(), currentLagTime);
		
		TruckTravelUnit lastOrderTravelUnit = ScheduleHelper.makeLastOrderTravelUnit(truck, actualInterestedTime, currentSlot, ps);
		TruckTravelUnit pS4NextOrderVisited = ScheduleHelper.makeNextOrderTravelUnit(truck, actualInterestedTime, currentSlot, or);
		
		int travelCost = lastOrderTravelUnit == null ? 0 : (int)lastOrderTravelUnit.getTravelTime().getStandardMinutes();
		travelCost +=  pS4NextOrderVisited == null ? 0 : (int)pS4NextOrderVisited.getTravelTime().getStandardMinutes();
		
		return new TruckCostForDelivery(deliveryCost, travelCost, tdu);
	}
	/**
	 * @param possibleCost
	 * @param actualInterestedTime
	 * @param currTime
	 * @param timeWindow 
	 * @param or 
	 * @return the current available slot, in which the timeWindow fits. If not found then return Null.
	 * 				
	 */
	private AvailableSlot getCurrentSlot(int possibleCost, DateTime actualInterestedTime, DateTime currTime, DeliveryTruckInitial truck, TimeSlot timeWindow, OrderAgentInitial or) {
		ArrayList<AvailableSlot> availableSlots = new ArrayList<AvailableSlot>();
		ArrayList<TruckScheduleUnit> usableSchedule = new ArrayList<TruckScheduleUnit>();
		
		long slotSizeInMim =  new Duration (timeWindow.getStartTime(), timeWindow.getEndTime()).getStandardMinutes();
		usableSchedule = putInScheduleAccordingToCostValue(possibleCost, currTime, schedule, truck, or);
		Utility.adjustAvailableSlotInBeginning(currTime, availableSlots);
		availableSlots = Utility.getAvailableSlots(usableSchedule, availableSlots, new TimeSlot (new DateTime(currTime), truck.getTotalTimeRange().getEndTime()), slotSizeInMim);
		if (availableSlots.size() == 0)
			return null;
		else {
			for (AvailableSlot av : availableSlots) {
				if (av.getStartTime().compareTo(actualInterestedTime)< 0 && av.getEndTime().compareTo(actualInterestedTime) > 0)
					return av;
			}
			return null;
		}
	}
	/**
	 * @param cost
	 * @param currTime
	 * @param truckSchedule
	 * @param truck
	 * @param or 
	 * @return Put the DSU in a returnSchedule, according to cost value given as parameter. So if this method is called with cost == 0,
	 * then returnSchedule would have all the DSU's form original schedule. If cost is 1, then all DSU's would be in returnScehdule 
	 * except 'UNDERPROCESS' ones. and so on
	 */
	private ArrayList<TruckScheduleUnit> putInScheduleAccordingToCostValue(int cost, DateTime currTime, 
			ArrayList<TruckScheduleUnit> truckSchedule, DeliveryTruckInitial truck, OrderAgentInitial or ) {
		ArrayList<TruckScheduleUnit> returnSchedule = new ArrayList<TruckScheduleUnit>();
		switch (cost) {//if same order delivery then never compromised
			case 0:// for checking if schedule is free at interested time
			{
				for(TruckScheduleUnit tsu : truckSchedule) {
					returnSchedule.add(tsu);
				}
				break;
			}
			case 1: //if found available slot in case1, but didn't found in case0, then it means an UNDER_PROCESS delivery will be compromised
			{//if same order delivery then never compromised
				for(TruckScheduleUnit tsu : truckSchedule) {
					if (tsu instanceof TruckDeliveryUnit) {
						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
						if (del.getOrder() == or)
							returnSchedule.add(tsu);
						else if ( unitStatus.get(del) == Reply.ACCEPT || unitStatus.get(del) == Reply.WEEK_ACCEPT ) 
							returnSchedule.add(tsu);
					}
				}
				break;
			}
			case 2: // if found available slot in case2 but not in earlier cases then means a WEEK_ACCEPTED	delivery would be compromised 
			{
				for(TruckScheduleUnit tsu : truckSchedule) {
					if (tsu instanceof TruckDeliveryUnit) {
						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
						if (del.getOrder() == or)
							returnSchedule.add(tsu);
						else if ( unitStatus.get(del) == Reply.ACCEPT ) 
							returnSchedule.add(tsu);
					}
				}
				break;
			}
			case 3: //if found available slot in case3 but not earlier means ACCEPT deliveries with delNo==0 && 
				//curTime+(MIMINUTES_BEFOR_DELIVERY_CREATED*3)< del.deliveryTime is compromised 
			{
				for(TruckScheduleUnit tsu : truckSchedule) {
					if (tsu instanceof TruckDeliveryUnit) {
						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
						if (del.getOrder() == or)
							returnSchedule.add(tsu);
						else if ( unitStatus.get(del) == Reply.ACCEPT && !( del.getDeliveryNo() == 0 
								&& currTime.plusMinutes(GlobalParameters.MINUTES_BEFOR_DELIVERY_CREATED*3).compareTo(del.getDeliveryTime()) <= 0))
							//if it is an accept but not like above deliery
							returnSchedule.add(tsu);
					}
				}
				break;
			}
//			case 4: //if found availableSlot in case4 and not earlier, means ACCEPT with (delNo==0 || delNo>0 &&
//				//curTime+MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED < del.deliveryTime)
//			{
//				for(TruckScheduleUnit tsu : truckSchedule) {
//					if (tsu instanceof TruckDeliveryUnit) {
//						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
//						if (del.getOrder() == or)
//							returnSchedule.add(tsu);
//						else if ( unitStatus.get(del) == Reply.ACCEPT &&
//								!(currTime.plusMinutes(GlobalParameters.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED).compareTo(del.getDeliveryTime()) <= 0))
//							returnSchedule.add(tsu);
//					}
//				}
//				break;
//			}
//			case 5:
//			{
//				for(TruckScheduleUnit tsu : truckSchedule) {
//					if (tsu instanceof TruckDeliveryUnit) {
//						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
//						if (del.getOrder() == or)
//							returnSchedule.add(tsu);
//						else if ( unitStatus.get(del) == Reply.ACCEPT &&
//								!(currTime.plusMinutes(GlobalParameters.MINUTES_BEFOR_DELIVERY_CREATED*4).compareTo(del.getDeliveryTime()) <= 0))
//							returnSchedule.add(tsu);
//					}
//				}
//				break;
//			}
//			case 6:
//			{
//				for(TruckScheduleUnit tsu : truckSchedule) {
//					if (tsu instanceof TruckDeliveryUnit) {
//						Delivery del = ((TruckDeliveryUnit) tsu).getDelivery();
//						if (del.getOrder() == or)
//							returnSchedule.add(tsu);
//						else if ( unitStatus.get(del) == Reply.ACCEPT &&
//								!(currTime.plusMinutes(GlobalParameters.MINUTES_BEFOR_DELIVERY_CREATED*3).compareTo(del.getDeliveryTime()) <= 0))
//							returnSchedule.add(tsu);
//					}
//				}
//				break;
//			}
			
		}
		checkArgument(returnSchedule.isEmpty() == false,  true);
		returnSchedule = fillTravelUnitsInSchedule(truck, returnSchedule, Utility.getCloner());//Well means TTU wold be cloned but TDU wound't. 
		//But doesnt matter since usable schedule is at the end would only be used to get available slots!
		Utility.sortSchedule(returnSchedule);
		
		return returnSchedule;
		
	}
	private Duration getPsToOrderDuration(ProductionSiteInitial ps, OrderAgentInitial or, DeliveryTruckInitial truck) {
		Double psToOrderDistance = Point.distance(ps.getLocation(), or.getPosition());
		Duration psToOrderTravelTime = new Duration((long)((psToOrderDistance/truck.getSpeed())*60*60*1000)); //travel time required to reach order from specific PS
		return psToOrderTravelTime;
	}
	private ProductionSiteInitial getSelectedPS(OrderAgentInitial order) {
		return order.getPossibleSites().get(0); //TODO: after first checking, i should check for all possible prodcution sites, 
		//coz currently they aren't ordered at all.
	}
	public void getClashingTSUWith(TruckDeliveryUnit tunit) {
//		for (TruckScheduleUnit tsu : schedule) {
//			if (tsu instanceof TruckDeliveryUnit) {
//				if (((TruckDeliveryUnit) tsu).)
//			}
//		}
//		
	}
	/**
	 * @param tunit with respect to which overlapped has to be found in this.schedule
	 * Should be called only if this.isOverlapped(TruckDeliveryUnit tunit) is true
	 */
	public void removeOverlappedUnit(TruckDeliveryUnit tunit, DeliveryTruckInitial rmcTruck, DateTime currTime) {
		int indexOfOverlappingUnit = 0;
		for (TruckScheduleUnit u : schedule) {
			if (tunit.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getStartTime()) >= 0) {
				if (tunit.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getEndTime()) <= 0){
					indexOfOverlappingUnit = getIndexOfOverlappedUnit(u);
					break;
				}
			}
			else { //means startTime is less
				if (tunit.getTimeSlot().getEndTime().compareTo(u.getTimeSlot().getStartTime()) >= 0){
				 //means overlap exists 
					indexOfOverlappingUnit = getIndexOfOverlappedUnit(u);
					break;
				}
			}
		}
		checkArgument(schedule.get(indexOfOverlappingUnit) instanceof TruckDeliveryUnit, true);
		TruckDeliveryUnit removableDelivery = (TruckDeliveryUnit) schedule.get(indexOfOverlappingUnit);
		this.remove(removableDelivery, rmcTruck, currTime);
		
	}
	/**
	 * @param u
	 * @return
	 */
	private int getIndexOfOverlappedUnit(TruckScheduleUnit u) {
		int indexOfOverlappingUnit;
		if (u instanceof TruckDeliveryUnit)
			indexOfOverlappingUnit = getIndexOf((TruckDeliveryUnit) u, schedule);
		else //means u instance of TTU, so the TDU after it should be removed.
			indexOfOverlappingUnit = getIndexOf((TruckTravelUnit)u,schedule) + 1;
		return indexOfOverlappingUnit;
	}
	public void fillTravelUnitsInSchedule(DeliveryTruckInitial truckAgent) {
		this.fillTravelUnitsInSchedule(truckAgent, schedule, Utility.getCloner());
		Utility.sortSchedule(schedule);
	}
	
	/**
	 * @param inputSchedule
	 * @return the portion of inputSchedule, that is extended form this.schedule
	 * 
	 * //exp selection decesio should be based on newly explored schedule, not previous schedule
	 */
	protected ArrayList<TruckScheduleUnit> getExtendedSchedule(ArrayList<TruckScheduleUnit> inputSchedule) {
		ArrayList<TruckScheduleUnit> extendedSchedule = new ArrayList<TruckScheduleUnit>();
		boolean alreadyExist;
		for (TruckScheduleUnit tsu: inputSchedule) {
			alreadyExist = false;
			for (TruckScheduleUnit existingTsu: this.schedule) {
				if (tsu instanceof TruckDeliveryUnit && existingTsu instanceof TruckDeliveryUnit) {
					if (((TruckDeliveryUnit)tsu).getDelivery().equals(((TruckDeliveryUnit)existingTsu).getDelivery())) {
						alreadyExist = true;
						break;
					}
				}
				else if (tsu instanceof TruckTravelUnit && existingTsu instanceof TruckTravelUnit) {
					//if (newUnit.equals(u)){ some how not working
					if (tsu.getStartLocation().equals(existingTsu.getStartLocation()) && tsu.getEndLocation().equals(existingTsu.getEndLocation())
							&& ((TruckTravelUnit)tsu).getTravelTime().equals(((TruckTravelUnit)existingTsu).getTravelTime()) 
							&& ((TruckTravelUnit)tsu).getStartLocation().equals(((TruckTravelUnit)existingTsu).getStartLocation())
							&& ((TruckTravelUnit)tsu).getEndLocation().equals(((TruckTravelUnit)existingTsu).getEndLocation())){
						alreadyExist = true;
						break;
					}
				}
			}
			if (!alreadyExist)
				extendedSchedule.add(tsu);
		}
		checkArgument (inputSchedule.size() >= extendedSchedule.size(), true );
		return extendedSchedule; 
	}
	public Reply getUnitStatus(Delivery delivery) {
		return this.unitStatus.get(delivery);
		
	}
	public TruckScheduleUnit getScheduleUnitCurrentlyActive(DateTime currTime) {
		for (TruckScheduleUnit tsu : this.practicalSchedule) {
			if (currTime.compareTo(tsu.getTimeSlot().getStartTime())>= 0 
					&& currTime.compareTo(tsu.getTimeSlot().getEndTime()) < 0)
			return tsu;
		}
		return null;
	}
}
