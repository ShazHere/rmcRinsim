/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.rits.cloning.Cloner;
import static com.google.common.base.Preconditions.checkArgument;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters.Weights;

/**
 * @author Shaza
 *
 */
public class ExpAnt extends Ant {

	private ArrayList<TimeSlot> availableSlots;
	private final ArrayList<TruckScheduleUnit> schedule;
	private boolean scheduleComplete;
	private final DateTime creationTime;
	
	
	private final double truckSpeed;
	private final TimeSlot truckTotalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//private TimeSlot currentSlot;
	private TruckScheduleUnit currentUnit;
	private DateTime currentInterestedTime;
	private Duration currentLagTime;

	private final DeliveryTruckInitial originator; //the actual truck agent which initialized the ExpAnt 
	private int scheduleUnitsAdded; //to keep track that exp ant itself added how many units.
	
	/**
	 * Used only for first time Ant creation by the orginator, should never be used for cloning purpose.
	 * @param sender Originator
	 * @param pAvailableSlots available slot of the truck at the moment of creation of expAnt
	 * @param pSchedule schedule of truck at the moment of creation of expAnt
	 * @param pCreateTime creation time
	 * @param pReturnEarlyProbability probability that ant can return early (with one unit in schedule)
	 */
	public ExpAnt(CommunicationUser sender,
			ArrayList<TimeSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;  //ant originator, creater. Sender means the one who is curreently sending ant after cloning
		
		schedule = Utility.getCloner().deepClone(pSchedule);
		availableSlots = Utility.getCloner().deepClone(pAvailableSlots);
		scheduleComplete = false; 		// to keep track if the schedule is explored by ant, now it could return to orignator.
		creationTime = pCreateTime;
		scheduleUnitsAdded = 0; 		//to keep track how many schedule units are added by the current ant. 
		
		truckSpeed = ((DeliveryTruckInitial)originator).getSpeed();
		truckTotalTimeRange = new TimeSlot(new DateTime(creationTime), originator.getTotalTimeRange().getEndTime());
		currentUnit = null; // It should not contain any unit if currently expAnt isn't interested in any order.
		currentInterestedTime = null;
		currentLagTime = null;
	}
	/**
	 * used by clone(sender) method..for hop by hop movement
	 * @param sender
	 * @param pAvailableSlots
	 * @param pSchedule
	 * @param pCreateTime
	 * @param pReturnEarlyProbability
	 * @param pOriginator
	 */
	private ExpAnt(CommunicationUser sender,
			ArrayList<TimeSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime, CommunicationUser pOriginator) {
		super(sender);
		originator = (DeliveryTruckInitial)pOriginator;
		availableSlots = Utility.getCloner().deepClone(pAvailableSlots);
		schedule = Utility.getCloner().deepClone(pSchedule);
		scheduleComplete = false;
		creationTime = pCreateTime;
		truckSpeed = ((DeliveryTruckInitial)originator).getSpeed();
		truckTotalTimeRange = ((DeliveryTruckInitial)originator).getTotalTimeRange();
		
	}
/**
 * similar to int.isScheduleComplete
 * @return
 */
	public boolean isScheduleComplete() {
		if (scheduleUnitsAdded >= GlobalParameters.EXPLORATION_SCHEDULE_SIZE) //TODO: shouldn't i make currentUnit = null now?
			scheduleComplete = true;
		else
			scheduleComplete = false;			
		return scheduleComplete;
	}
	public DateTime getCreationTime() {
		return creationTime;
	}
	public int getNewScheduleSize() {
		return scheduleUnitsAdded;
	}

	/**
	 * Apart from returning true if interested, the method also creates a new currentUnit
	 * @param interestedTime
	 * @param travelDistance
	 * @param currTime
	 * @return true if the ExpAnt is interested at interested time even though it has to travel through 
	 * the travelDistance after loading time (LOADING_MINUTES). 
	 */
	public boolean isInterested(DateTime interestedTime, Double travelDistance,  final DateTime currTime) {
		Duration travelTime = new Duration((long)((travelDistance/truckSpeed)*60*60*1000)); //travel time required to reach order from specific PS
		Utility.getAvailableSlots(this.schedule, this.availableSlots, new TimeSlot (new DateTime(currTime), this.truckTotalTimeRange.getEndTime()));
		if (availableSlots.size() == 0)
			return false;
		TimeSlot currentSlot = availableSlots.get(0);
		DateTime actualInterestedTime = interestedTime.minus(travelTime).minusMinutes(GlobalParameters.LOADING_MINUTES); //means time at which truck should start its slot for current delivery
			if (currentSlot.getStartTime().compareTo(actualInterestedTime)<= 0 //making rough estimation that will order enoughÊ interesting to be visited, one hour added since we donot not know how much could be travel distance from selected PS. PS is selected latter
					&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(70).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0) {
				//if ((new Duration(currentSlot.getStartTime(), actualInterestedTime)).getStandardMinutes() < (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()) {
					currentInterestedTime = actualInterestedTime;
					this.currentLagTime = new Duration(0);
						return true;
				//}
			}
			else if (GlobalParameters.LAG_TIME_ENABLE) {//estimate with lag time
				if (currentSlot.getStartTime().compareTo(actualInterestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0 //making rough estimation that will order enoughÊ interesting to be visited
						&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES).plusMinutes(70).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0) {
//					Duration possibleLagtime = new Duration(actualInterestedTime, currentSlot.getStartTime() );
//					if (currentSlot.getStartTime().compareTo(actualInterestedTime.plus(possibleLagtime)) <= 0) {// this condition is added since, once entered in from outer if, there were chances hat 
						//(new Duration(currentSlot.getStartTime(), actualInterestedTime)).getStandardMinutes() < (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()) {
					checkArgument( ((new Duration(currentSlot.getStartTime(), currentSlot.getEndTime())).getStandardMinutes() >= (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()), true);						
						this.currentLagTime = new Duration (actualInterestedTime, currentSlot.getStartTime() );
						currentInterestedTime = actualInterestedTime.plus(this.currentLagTime);
							return true;
					//}
				}
			}
		return false;
	}
//TODO make it return defensive copy
	public TruckScheduleUnit getCurrentUnit() {
		return Utility.getCloner().deepClone(this.currentUnit);
	}
	
	public Duration getCurrentLagTime(){
		return currentLagTime;
	}
	public DateTime getCurrentInterestedTime() {
		return currentInterestedTime;
	}
	@Override
	public CommunicationUser getSender() {
		return super.getSender();
	}

	public ArrayList<TruckScheduleUnit> getSchedule() {
		return schedule;
	}
	

	/**
	 * @param pSender The current sender from which the ant is sent to the recepient
	 * @return new exp, clone of the current one. 
	 */
	public ExpAnt clone(CommunicationUser pSender) {
		final Cloner cl = Utility.getCloner();
		ExpAnt exp = new ExpAnt(pSender,cl.deepClone(this.availableSlots), cl.deepClone(this.schedule), this.creationTime, this.originator );
		exp.currentUnit = cl.deepClone(this.currentUnit); // i guess even its not deepCloned it will be fine since its immutable object.
		exp.currentInterestedTime = this.currentInterestedTime;
		exp.scheduleComplete = this.scheduleComplete;
		exp.scheduleUnitsAdded = this.scheduleUnitsAdded;
		exp.currentLagTime = this.currentLagTime;
		return exp;
	}
 
	/**
	 * @return score of the proposed schedule 
	 */
	public int getScheduleScore() {
		/*
		 * score concerns: currentLagTime (20), travelDistance(20), ST Delay (10), wasted Concrete (10), preferred station (1)
		 * ????what about adding the factor of most filled schedule, or schedule with maximum allocations
		 *  
		 */
		int travelMin = 0;
		int lagTimeInMin = 0;
		int startTimeDelay = 0; //already included in lag time
		int wastedConcrete = 0;
		int deliveryNoEffect = 0;
		if (!schedule.isEmpty()) {
			for(TruckScheduleUnit u: schedule) {
				travelMin += u.getDelivery().getStationToCYTravelTime().getStandardMinutes();
				travelMin += u.getDelivery().getCYToStationTravelTime().getStandardMinutes();
				lagTimeInMin += u.getDelivery().getLagTime().getStandardMinutes();
				wastedConcrete += u.getDelivery().getWastedVolume();
				if (u.getDelivery().getDeliveryNo() == 0)
					deliveryNoEffect += 1;
			}
			//TODO: add weights as well..
			int score = (Weights.TRAVEL_TIME * travelMin) + (Weights.LAGTIME*lagTimeInMin) + 
					(Weights.STARTTIME_DELAY*startTimeDelay) + (Weights.CONCRETE_WASTAGE*wastedConcrete) + 
					(20 * deliveryNoEffect); 
			
			//add here attraction for the schedule with greater than delivery no. 0
			//return score/schedule.size(); //this is an attempt to normalize score with respect to size of schedule
			return score;
		}
		else 
			return 999999999;
	}
	public DeliveryTruckInitial getOriginator() {
		return originator;
	}
	public double getTruckSpeed() {
		return truckSpeed;
	}
	public ArrayList<TimeSlot> getAvailableSlots() {
		return availableSlots;
	}

	/**
	 * @param unitExistsAfterXTime The schedule has to be checked after this time
	 * @return Unit that exists after unitExistsAfterXTime
	 */
	public TruckScheduleUnit nextAfterCurrentUnit(DateTime unitExistsAfterXTime) {

		if (this.schedule.isEmpty())
			return null;
		for (TruckScheduleUnit u : this.schedule){ //compared with availableSlot.got(0), since normally if an exploration ant is interested in some schedule, it will be
			if (u.getTimeSlot().getStartTime().compareTo(unitExistsAfterXTime) > 0){ //Since there will be no overlaps, 
				return u;  //its assumed that current startTime of next unit should be greater than this parameter
			}
		}
		return null;
	}

	private boolean addCurrentUnitInSchedule() {
		try {
			this.schedule.add(this.currentUnit);//Actual addition in schedule
			scheduleUnitsAdded += 1;
			Utility.getAvailableSlots(this.schedule, this.availableSlots, this.truckTotalTimeRange);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean makeCurrentUnit(Delivery del) {
		DateTime unitEndTime = del.getDeliveryTime().plus(del.getUnloadingDuration()).plus(del.getCYToStationTravelTime());
		TimeSlot ts = new TimeSlot(this.getCurrentInterestedTime(), unitEndTime, del.getLoadingStation().getLocation(), del.getLoadingStation());
		this.currentUnit = new TruckScheduleUnit(originator, ts, del);
		if (this.addCurrentUnitInSchedule())
			return true;
		else
			return false;
	}
	@Override
	public String toString() {
		return this.schedule.toString();
	}
	
	/**
	 * @return commulative lag time of the schedule units of ant's schedule
	 */
	public Duration getScheduleLagTime() {
		Duration lagTime = new Duration (0);
		if (this.schedule.isEmpty())
			return lagTime;
		for(TruckScheduleUnit u: schedule) {
			lagTime = lagTime.plus( u.getDelivery().getLagTime());
		}
		return lagTime;
	}
}
