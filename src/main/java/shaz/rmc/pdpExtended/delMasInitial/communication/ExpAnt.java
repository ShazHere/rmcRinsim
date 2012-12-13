/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Random;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;

/**
 * @author Shaza
 *
 */
public class ExpAnt extends Ant {

//	public ExpAnt(CommunicationUser sender) {
//		super(sender);
//		// TODO Auto-generated constructor stub
//	}
	private ArrayList<TimeSlot> availableSlots;
	private final ArrayList<TruckScheduleUnit> schedule;
	private boolean scheduleComplete;
	private final DateTime creationTime;
	
	
	private final double truckSpeed;
	private final TimeSlot truckTotalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//private TimeSlot currentSlot;
	private TruckScheduleUnit currentUnit;
	
	private final int returnEarlyProbability; //the probability that exp should return having only one order booking in its schedule..
	//this was introduced because otherwise ants wont return at all, if there are less orders in environment. range is 0 to 9.
	private final Random returnEarlyProbabilityGen;
	private final DeliveryTruckInitial originator; //the actual truck agent which initialized the ExpAnt 
	
	
	/**
	 * used only for first time Ant creation by the orginator, should never be used for cloning purpose.
	 * @param sender Originator
	 * @param pAvailableSlots available slot of the truck at the moment of creation of expAnt
	 * @param pSchedule schedule of truck at the moment of creation of expAnt
	 * @param pCreateTime creation time
	 * @param pReturnEarlyProbability probability that ant can return early (with one unit in schedule)
	 */
	public ExpAnt(CommunicationUser sender,
			ArrayList<TimeSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime, int pReturnEarlyProbability) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;
		availableSlots = new ArrayList<TimeSlot>(pAvailableSlots);
		schedule = new ArrayList<TruckScheduleUnit>(pSchedule);
		scheduleComplete = false;
		creationTime = pCreateTime;
		
		returnEarlyProbability = pReturnEarlyProbability;
		returnEarlyProbabilityGen = new Random(250);
		
		truckSpeed = ((DeliveryTruckInitial)originator).getSpeed();
		truckTotalTimeRange =new TimeSlot(new DateTime(creationTime), originator.getTotalTimeRange().getEndTime());
		if (!schedule.isEmpty())
			currentUnit = schedule.get(0);
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
			ArrayList<TimeSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime, int pReturnEarlyProbability, CommunicationUser pOriginator) {
		super(sender);
		originator = (DeliveryTruckInitial)pOriginator;
		availableSlots = new ArrayList<TimeSlot>(pAvailableSlots);
		schedule = new ArrayList<TruckScheduleUnit>(pSchedule);
		scheduleComplete = false;
		creationTime = pCreateTime;
		
		returnEarlyProbability = pReturnEarlyProbability;
		returnEarlyProbabilityGen = new Random(250);
		
		truckSpeed = ((DeliveryTruckInitial)originator).getSpeed();
		truckTotalTimeRange = ((DeliveryTruckInitial)originator).getTotalTimeRange();
		if (!schedule.isEmpty())
			currentUnit = schedule.get(0);
	}

	public boolean isScheduleComplete() {
		if (schedule.size() >= 3)
			scheduleComplete = true;
		else
			scheduleComplete = false;			
		return scheduleComplete;
	}

//	public void setScheduleComplete(boolean scheduleComplete) {
//		this.scheduleComplete = scheduleComplete;
//	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public boolean isInterested(DateTime interestedTime, Double travelDistance, final DateTime currTime) {
		Duration travelTime = new Duration((long)((travelDistance/truckSpeed)*60*60*1000));
		//System.out.println("travel time =" +travelTime);
		Utility.getAvailableSlots(this.schedule, this.availableSlots, new TimeSlot (new DateTime(currTime), this.truckTotalTimeRange.getEndTime()));
		TimeSlot currentSlot = availableSlots.get(0);
		//System.out.println("in method = " + currentSlot.toString());
		if (currentSlot.getStartTime().compareTo(interestedTime.minus(travelTime))< 0 //making rough estimation that will order enoughÊ interesting to be visited
				&& currentSlot.getEndTime().compareTo(interestedTime.plusHours(1).plus(travelTime)) > 0) {
			currentUnit = new TruckScheduleUnit((DeliveryTruckInitial)originator, //start time also includes the travel distance and loading at production
					new TimeSlot (interestedTime.minus(travelTime).minusMinutes(GlobalParameters.LOADING_MINUTES),null)); //EndTime of slot cannot be decided here, It is adjusted at Order
			return true;
		}
		return false;
	}

	public TruckScheduleUnit getCurrentUnit() {
		return this.currentUnit;
	}
	
	@Override
	public CommunicationUser getSender() {
		return super.getSender();
	}

	public ArrayList<TruckScheduleUnit> getSchedule() {
		return schedule;
	}
	
	/**
	 * @return true if probability returns true accroding to early returnProbability.
	 */
	public boolean isReturnEarly() {
		boolean prob;
		if (returnEarlyProbability == 1){
			prob = returnEarlyProbabilityGen.nextInt(10) <= 1 ;
		}
		else if (returnEarlyProbability == 5){
			prob = returnEarlyProbabilityGen.nextInt(10) <= 5 ;
		}
		else {
			prob = returnEarlyProbabilityGen.nextInt(10) < 9 ;
		}
		//System.out.println("probability is " + prob);
		return prob;
	}
	/**
	 * @param pSender The current sender from which the ant is sent to the recepient
	 * @return
	 */
	public ExpAnt clone(CommunicationUser pSender) {
		ExpAnt exp = new ExpAnt(pSender,this.availableSlots, this.schedule, this.creationTime, this.returnEarlyProbability, this.originator );
		exp.currentUnit = this.currentUnit;
		exp.scheduleComplete = this.scheduleComplete;
		return exp;
	}
 
	/**
	 * @return score of the proposed schedule 
	 */
	public int getScheduleScore() {
		/*
		 * score concerns: lagTime (20), travelDistance(20), ST Delay (10), wasted Concrete (10), preferred station (1)
		 * ????what about adding the factor of most filled schedule, or schedule with maximum allocations
		 *  
		 */
		int travelMin = 0;
		int lagTimeInMin = 0;
		int startTimeDelay = 0; //already included in lag time
		int wastedConcrete = 0;
		if (!schedule.isEmpty()) {
			for(TruckScheduleUnit u: schedule) {
				travelMin += u.getDelivery().getStationToCYTravelTime().getStandardMinutes();
				travelMin += u.getDelivery().getCYToStationTravelTime().getStandardMinutes();
				lagTimeInMin += u.getDelivery().getLagTime().getStandardMinutes();
				wastedConcrete += u.getDelivery().getWastedVolume();
			}
			//TODO: add weights as well..
			return travelMin+lagTimeInMin+startTimeDelay+wastedConcrete;
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
}
