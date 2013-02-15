/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;

import org.joda.time.Duration;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

//import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Shaza
 *
 */
public class DeliveryTruckInitialBelief {
	
	private Point startLocation;
	private final TimeSlot totalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//only start and endTime will be used
	protected final ArrayList<ExpAnt> explorationAnts;
	protected final ArrayList<IntAnt> intentionAnts;
	
	//for Objective function
	Duration totalTravelTime; //keeps record of the time vehicle kept on traveling
	private int wastedConcrete; // to keep record of wasted concrete
	
	protected ArrayList<TimeSlot> availableSlots;
	private final DeliveryTruckInitial deliveryTruckAgent;
	protected final ArrayList<TruckScheduleUnit> schedule;
	
	
	public DeliveryTruckInitialBelief(DeliveryTruckInitial pDeliveryTruck,  ArrayList<TruckScheduleUnit> pSch) {
		wastedConcrete = 0;
		totalTravelTime = new Duration(0); //to keep record of total travelling time, to be used by objective function latter.
		deliveryTruckAgent = pDeliveryTruck;
		this.schedule = pSch;
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		totalTimeRange = new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME);
		availableSlots = new ArrayList<TimeSlot>();
		availableSlots.add(new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME));
	}
	public ArrayList<TimeSlot> getAvailableSlots() {
		return availableSlots;
	}
	

	public int getWastedConcrete() {
		return wastedConcrete;
	}
	public void setWastedConcrete(int wastedConcrete) {
		this.wastedConcrete = wastedConcrete;
	}
	
	public boolean addWastedConcrete(int amount){
		this.wastedConcrete += amount;
		return true;
	}
	/** @description Returns total time traveled by this truck
	 * 
	 */
	public Duration getTotalTravelTime() {
		return totalTravelTime;
	}

	public Point getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Point startLocation) {
		this.startLocation = startLocation;
	}
	public TimeSlot getTotalTimeRange() {
		return totalTimeRange;
	}
	/**
	 * to check if newSch (explored by expAnt) still contains all the elements of b.schedule and is a valid schedule for truck
	 * @param newSch to be compared with actual b.schedule of truck
	 * @return true if all b.schedule units are still in newSch, and false if not.
	 */ //TODO check do i need to compare sizes of newSch and b.schedule? to validate that newSch also contains new explored stuff
	protected boolean scheduleStillValid(ArrayList<TruckScheduleUnit> oldSch, ArrayList<TruckScheduleUnit> newSch) {
		if (oldSch.isEmpty()) //if already existing schedule is empty, then what ever new schedule is, its ok for truck
			return true;
		boolean foundMatch = false;
		for (TruckScheduleUnit u : oldSch) {
			foundMatch = false;
			for (TruckScheduleUnit newUnit: newSch) {
//				if (((OrderAgentInitial)u.getDelivery().getOrder()).getOrder().getId().equals(((OrderAgentInitial)newUnit.getDelivery().getOrder()).getOrder().getId())  //add timeSlot match as well
//						&& u.getDelivery().getDeliveryNo() == newUnit.getDelivery().getDeliveryNo()) {
				if (u.getDelivery().equalsWithSameTruck(newUnit.getDelivery())) {
					foundMatch = true;
					break;
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
}
