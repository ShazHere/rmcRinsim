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
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.ScheduleHelper;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters.Weights;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;

/**
 * @author Shaza
 *
 */
public class ExpAnt extends Ant {

	private ArrayList<AvailableSlot> availableSlots;
	private final ArrayList<TruckScheduleUnit> schedule;
	private boolean scheduleComplete;
	private final DateTime creationTime;
	
	
	private final double truckSpeed;
	private final TimeSlot truckTotalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//private TimeSlot currentSlot;
	private TruckScheduleUnit currentUnit;
	private DateTime currentInterestedTime;
	private Duration currentLagTime;
	private int currentWastedConcrete;
	
	private TruckTravelUnit lastOrderTravelUnit;
	private TruckTravelUnit pS4NextOrderVisited;

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
			ArrayList<AvailableSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;  //ant originator, creater. Sender means the one who is curreently sending ant after cloning
		
		schedule = Utility.getCloner().deepClone(pSchedule);
		//schedule = pSchedule;
		availableSlots = Utility.getCloner().deepClone(pAvailableSlots);
		//availableSlots = pAvailableSlots;
		scheduleComplete = false; 		// to keep track if the schedule is explored by ant, now it could return to orignator.
		creationTime = pCreateTime;
		scheduleUnitsAdded = 0; 		//to keep track how many schedule units are added by the current ant. 
		
		truckSpeed = ((DeliveryTruckInitial)originator).getSpeed();
		truckTotalTimeRange = new TimeSlot(new DateTime(creationTime), originator.getTotalTimeRange().getEndTime());
		currentUnit = null; // It should not contain any unit if currently expAnt isn't interested in any order.
		currentInterestedTime = null;
		currentLagTime = null;
		lastOrderTravelUnit = null;
		pS4NextOrderVisited = null;
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
			ArrayList<AvailableSlot> pAvailableSlots, ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime, CommunicationUser pOriginator) {
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
	 * @param interestedTime in which currentOrder is interested
	 * @param travelDistance from current PS to Order
	 * @param currTime
	 * @return true if the ExpAnt is interested at interested time even though it has to travel through 
	 * the travelDistance after loading time (LOADING_MINUTES). 
	 */
	public boolean isInterested(DateTime interestedTime, Double travelDistance,  final DateTime currTime, ProductionSiteInitial pS, OrderAgentInitial pOr) {
		Duration currPSToCurrOrderTravelTime = new Duration((long)((travelDistance/truckSpeed)*60*60*1000)); //travel time required to reach order from specific PS
		Utility.getAvailableSlots(this.schedule, this.availableSlots, new TimeSlot (new DateTime(currTime), this.truckTotalTimeRange.getEndTime()), GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l);
		if (availableSlots.size() == 0)
			return false;
		for(AvailableSlot av : availableSlots){
			AvailableSlot currentSlot = av;//availableSlots.get(0);
			DateTime actualInterestedTime = ScheduleHelper.getActualInterestedTime(interestedTime, currPSToCurrOrderTravelTime); //means time at which truck should start its slot for current delivery
				if (ScheduleHelper.isInterestedAtExactTime(this.originator, interestedTime, currPSToCurrOrderTravelTime, currentSlot,actualInterestedTime, pS, pOr)) {
					//if ((new Duration(currentSlot.getStartTime(), actualInterestedTime)).getStandardMinutes() < (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()) {
						this.currentLagTime = new Duration(0);
						currentInterestedTime = actualInterestedTime;
						lastOrderTravelUnit = ScheduleHelper.makeLastOrderTravelUnit(this.originator, currentInterestedTime, currentSlot, pS);
						pS4NextOrderVisited = ScheduleHelper.makeNextOrderTravelUnit(this.originator, currentInterestedTime, currentSlot, pOr);
						return true;
					//}
				}
				else if (GlobalParameters.LAG_TIME_ENABLE) {//estimate with lag time
					if (ScheduleHelper.isInterestedWithLagTime(this.originator, interestedTime, currPSToCurrOrderTravelTime,currentSlot, actualInterestedTime, pS, pOr)) {
	//					Duration possibleLagtime = new Duration(actualInterestedTime, currentSlot.getStartTime() );
	//					if (currentSlot.getStartTime().compareTo(actualInterestedTime.plus(possibleLagtime)) <= 0) {// this condition is added since, once entered in from outer if, there were chances hat 
							//(new Duration(currentSlot.getStartTime(), actualInterestedTime)).getStandardMinutes() < (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()) {
						checkArgument( ((new Duration(currentSlot.getStartTime(), currentSlot.getEndTime())).getStandardMinutes() >= (Duration.standardHours(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS)).getStandardMinutes()), true);						
							this.currentLagTime = ScheduleHelper.makeLagTime(currentSlot, actualInterestedTime, pS);//TODO: check this, it should be duraton b/w actualInterestedTime and the induced lag time! should calculate with seperate method..17/07/2013
							currentInterestedTime = actualInterestedTime.plus(this.currentLagTime);
							lastOrderTravelUnit = ScheduleHelper.makeLastOrderTravelUnit(this.originator, currentInterestedTime, currentSlot, pS);
							pS4NextOrderVisited = ScheduleHelper.makeNextOrderTravelUnit(this.originator, currentInterestedTime, currentSlot, pOr);
							return true;
						//}
					}
				}
		}
		return false;
	}

//	/**
//	 * @param currentSlot
//	 * @param actualInterestedTime
//	 * @return
//	 */
//	private Duration makeLagTime(AvailableSlot currentSlot,DateTime actualInterestedTime, ProductionSiteInitial pS) {
//		Duration durationLastOrderToPS = new Duration(0);
//		if (currentSlot.getLastOrderVisited() != null) {
//			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
//			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
//		}
//		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1); 
//		//currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0
//		return new Duration (lastOrderToPSJustBeforeActualInterstedTime, currentSlot.getStartTime() );
//	}
//	/**
//	 * @param currentSlot
//	 * @param pOr current order under consideration
//	 * @return the TruckTravelUnit to travel from last order to current PS. Should be called 
//	 * - after setting currentTnterestedTime and currentLagTime
//	 * - if isInterestedWithLagTime(..) or isInterestedAtExactTime(..) is true
//	 */ //TODO: change accroding to utility.getTravelUnitTimeSlot..
//	private TruckTravelUnit makeNextOrderTravelUnit(AvailableSlot currentSlot, OrderAgentInitial pOr) { 
//		if (currentSlot.getPS4NextOrderVisitedd() == null)
//			return null;
//		Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
//		Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
//		TimeSlot travelSlot =  new TimeSlot( currentSlot.getEndTime().minus(durationOrderToPS4NextOrder).minusMinutes(1), currentSlot.getEndTime().minusMinutes(1));
//		TruckTravelUnit ttu = new TruckTravelUnit(originator, travelSlot, pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition(), durationOrderToPS4NextOrder );
//		checkArgument(currentInterestedTime.compareTo(ttu.getTimeSlot().getStartTime()) < 0, true);
//		return ttu;	
//	}
//	/**
//	 * @param currentSlot
//	 * @param pS
//	 * @return the TruckTravelUnit to travel from last order to current PS. Should be called 
//	 * - after setting currentTnterestedTime and currentLagTime
//	 * - if isInterestedWithLagTime(..) or isInterestedAtExactTime(..) is true
//	 */
//	private TruckTravelUnit makeLastOrderTravelUnit(AvailableSlot currentSlot, ProductionSiteInitial pS) {
//		if (currentSlot.getLastOrderVisited() == null)
//			return null;
//		Double distanceLastOrderToPS = Point.distance( currentSlot.getLastOrderVisited().getPosition(), pS.getPosition());
//		Duration durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
//		TimeSlot travelSlot =  new TimeSlot(currentInterestedTime.minus(durationLastOrderToPS).minusMinutes(1), currentInterestedTime.minusMinutes(1));
//		TruckTravelUnit ttu = new TruckTravelUnit(originator, travelSlot, currentSlot.getLastOrderVisited().getPosition(), pS.getPosition(),durationLastOrderToPS );
//		checkArgument(currentInterestedTime.compareTo(ttu.getTimeSlot().getStartTime()) > 0, true);
//		return ttu;
//	}
//	/**
//	 * @param interestedTime
//	 * @param travelTime
//	 * @param currentSlot
//	 * @param actualInterestedTime
//	 * @return
//	 */
//	private boolean isInterestedWithLagTime(DateTime interestedTime, Duration travelTime, AvailableSlot currentSlot,
//			DateTime actualInterestedTime, ProductionSiteInitial pS, OrderAgentInitial pOr) {
//		Duration durationLastOrderToPS = new Duration(0);
//		if (currentSlot.getLastOrderVisited() != null) {
//			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
//			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
//		}
//		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1); 
//		DateTime currentSlotEndTime = currentSlot.getEndTime();
//		if (currentSlot.getPS4NextOrderVisitedd() != null) {
//			Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
//			Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
//			currentSlotEndTime = currentSlotEndTime.minus(durationOrderToPS4NextOrder).minusMinutes(1);
//		}
//		return currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0 //making rough estimation that will order enoughÊ interesting to be visited
//				&& currentSlotEndTime.compareTo(interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES).plusMinutes(70).plus(getCurrentOrderEstimatedTravelTimeAndUnloadingTime(travelTime))) > 0;
////		return currentSlot.getStartTime().compareTo(actualInterestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0 //making rough estimation that will order enoughÊ interesting to be visited
////				&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES).plusMinutes(70).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0;
//	}
//	/**
//	 * @param interestedTime
//	 * @param travelTime
//	 * @param currentSlot
//	 * @param actualInterestedTime
//	 * @return
//	 */
//	private boolean isInterestedAtExactTime(DateTime interestedTime, Duration travelTime, AvailableSlot currentSlot,
//			DateTime actualInterestedTime, ProductionSiteInitial pS, OrderAgentInitial pOr) {
//		Duration durationLastOrderToPS = new Duration(0);
//		if (currentSlot.getLastOrderVisited() != null) {
//			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
//			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
//		}
//		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1);
//		DateTime currentSlotEndTime = currentSlot.getEndTime();
//		if (currentSlot.getPS4NextOrderVisitedd() != null) {
//			Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
//			Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
//			currentSlotEndTime = currentSlotEndTime.minus(durationOrderToPS4NextOrder).minusMinutes(1);
//		}
//		return currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime)<= 0 //making rough estimation that will order enoughÊ interesting to be visited, one hour added since we donot not know how much could be travel distance from selected PS. PS is selected latter
//				&& currentSlotEndTime.compareTo(interestedTime.plusMinutes(70).plus(getCurrentOrderEstimatedTravelTimeAndUnloadingTime(travelTime))) > 0;
////		return currentSlot.getStartTime().compareTo(actualInterestedTime)<= 0 //making rough estimation that will order enoughÊ interesting to be visited, one hour added since we donot not know how much could be travel distance from selected PS. PS is selected latter
////				&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(70).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0;
//	}
//	
//	private Duration getCurrentOrderEstimatedTravelTimeAndUnloadingTime(Duration pTravelTime) {
//		return pTravelTime.plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)));
//	}
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
		exp.lastOrderTravelUnit = this.lastOrderTravelUnit;
		exp.pS4NextOrderVisited = this.pS4NextOrderVisited;
		exp.currentWastedConcrete = this.currentWastedConcrete;
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
		int startTimeDelay = 0; //already included in lag time
		int wastedConcrete = 0;
		int deliveryNoEffect = 0;
		if (!schedule.isEmpty()) {
			for(TruckScheduleUnit u: schedule) {
				if (u instanceof TruckTravelUnit)
					travelMin += ((TruckTravelUnit) u).getTravelTime().getStandardMinutes();
				else {
					travelMin += ((TruckDeliveryUnit) u).getDelivery().getStationToCYTravelTime().getStandardMinutes();
					//travelMin += u.getDelivery().getCYToStationTravelTime().getStandardMinutes();
					//lagTimeInMin += u.getDelivery().getLagTime().getStandardMinutes();
					wastedConcrete += ((TruckDeliveryUnit) u).getWastedConcrete();
					if (((TruckDeliveryUnit) u).getDelivery().getDeliveryNo() == 0) {
						deliveryNoEffect += 1;
						startTimeDelay += ((TruckDeliveryUnit) u).getLagTime().getStandardMinutes(); //line added on 17/07/2013
					}
				}
			}
			int score = (Weights.TRAVEL_TIME * travelMin) + //(Weights.LAGTIME*lagTimeInMin) + 
					(Weights.STARTTIME_DELAY*startTimeDelay) + (Weights.CONCRETE_WASTAGE*wastedConcrete) ;
					//+ (30 * deliveryNoEffect); 
			
			//add here attraction for the schedule with greater than delivery no. 0
			return score/schedule.size(); //this is an attempt to normalize score with respect to size of schedule
			//return score;
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
	public ArrayList<AvailableSlot> getAvailableSlots() {
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
			checkArgument(currentUnit != null, true);
			if (lastOrderTravelUnit != null) {
				checkArgument(lastOrderTravelUnit.getTimeSlot().getEndTime().compareTo(currentUnit.getTimeSlot().getStartTime()) <= 0 , true);
				this.schedule.add(lastOrderTravelUnit); //travel unit from last order to current order
			}
			if (pS4NextOrderVisited != null) {
				checkArgument(currentUnit.getTimeSlot().getEndTime().compareTo(pS4NextOrderVisited.getTimeSlot().getStartTime()) <= 0 , true);
				this.schedule.add(this.pS4NextOrderVisited);
			}
			if (lastOrderTravelUnit != null && pS4NextOrderVisited != null) { // TODO: I have to tackle this, remove the travel unit form previous order to next PS4Order 
				//and inserth the curren units and the relavent travel units..
				checkArgument((lastOrderTravelUnit != null && pS4NextOrderVisited != null) , false);
			}
			this.schedule.add(this.currentUnit);//Actual addition in schedule
			scheduleUnitsAdded += 1; //keepint it one considering that the added delivery units is one only. .. 
			Utility.getAvailableSlots(this.schedule, this.availableSlots, this.truckTotalTimeRange, GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l); //also sorts schedule
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	public boolean makeCurrentUnit(Delivery del) {
		DateTime unitEndTime = del.getDeliveryTime().plus(del.getUnloadingDuration());//.plus(del.getCYToStationTravelTime());
		//TimeSlot ts = new TimeSlot(this.getCurrentInterestedTime(), unitEndTime, del.getLoadingStation().getLocation(), del.getLoadingStation());
		TimeSlot ts = new TimeSlot(this.getCurrentInterestedTime(), unitEndTime);
		this.currentUnit = new TruckDeliveryUnit(originator, ts, del, this.currentWastedConcrete, this.currentLagTime);
		if (this.addCurrentUnitInSchedule())
			return true;
		else
			return false;
	}
	public void setCurrentWastedConcrete(int currentWastedConcrete) {
		this.currentWastedConcrete = currentWastedConcrete;
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
			if (u instanceof TruckDeliveryUnit)
				lagTime = lagTime.plus( ((TruckDeliveryUnit) u).getLagTime());
		}
		return lagTime;
	}
}
