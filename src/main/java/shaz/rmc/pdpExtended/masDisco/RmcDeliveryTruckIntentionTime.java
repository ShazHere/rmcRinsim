/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;

import shaz.rmc.core.Agent;
import shaz.rmc.core.TruckScheduleUnit;

/**
 * Performs the physical world interaction. Fullfills the current intention.
 * @author Shaza
 *
 */
public class RmcDeliveryTruckIntentionTime {
	Agent truck;
	RmcDeliveryTruckBeliefs b;
	ArrayList<TruckScheduleUnit> schedule;
	Duration previousTime = null;
	Duration timeDifference; //to keep the difference in time according to each process tick
	
	boolean isLoaded;	//Is the vehicle loaded with concrete?
	
	// variables related to current destination
	int currentUnitNo = 0;
	Point destination; //contain (CY/PC/null)..basically contains the location the vehicle is currently heading towards
	Duration remainingMsToDestination;  //remaining MS to destination
	Duration totalMsRequiredToDestination; //total time required to reach destination extracted from the problem file
//	boolean wastedConcreteAddedForFirstDelivery;
	
	final Logger logger; //for logging
	
	public RmcDeliveryTruckIntentionTime(Agent a, RmcDeliveryTruckBeliefs b, ArrayList<TruckScheduleUnit> sch) {
		this.truck = a;
		this.b = b;
		timeDifference = new Duration(200); //200 coz agent processTick is called after 200,000microseconds, so we will reduce 200MS
		this.schedule = sch;
		
		currentUnitNo = 0; //contains  the schedule unit the truck is currently executing from its schedule, it starts at 0.
		destination = null;
		remainingMsToDestination = new Duration(0);
		totalMsRequiredToDestination= null;
		isLoaded = false;
	
//		wastedConcreteAddedForFirstDelivery = false;
		logger = Logger.getLogger(RmcDeliveryTruckBeliefs.class);
	}
	/**
	 * assumes that there is some schedule prepared, according to which vehicle has to move.
	 * schedule.empty() == false
	 * currentUnitNo. contains  the schedule unit the truck is currently executing from its schedule, it starts at 0.
	 * A unit includes w.r.t time 
	 * 					loading time, then isLoaded = true and destination=CY
	 * 					stationToCYtime
	 * 					unloading time, then isLoaded = false, destination = station
	 * 					CYtoStationtime, then destination = null 
	 * 
	 * At the begining, currentUnitNo = 0, isLoaded = false, destination = null
	 * The between two units could be long, but it will keep on passing quietly, without moving.
	 */
	public void followSchedule(TimeLapse time) {
		long currentTime = GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()); //currentTime contains value in millis
		if (currentTime >= schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis() ){ 
			if (currentTime < schedule.get(currentUnitNo).getTimeSlot().getEndTime().getMillis()) { //remain in current unit
				if (destination == null && isLoaded == false) {
					if (currentTime >= schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis() + schedule.get(currentUnitNo).getDelivery().getLoadingDuration().getMillis() ) {
						isLoaded = true;
						setDestination(((RmcOrderAgent)schedule.get(currentUnitNo).getDelivery().getOrderAg()).getPosition(), schedule.get(currentUnitNo).getDelivery().getStationToCYTravelTime(), 
							schedule.get(currentUnitNo).getDelivery().getStationToCYTravelTime());
						logger.debug(truck.getId()+ "T:" + " Starting Delivery at Station " + ((RmcProductionSite)schedule.get(currentUnitNo).getDelivery().getLoadingStation()).getStation().getId() + " for "  + schedule.get(currentUnitNo).getDelivery().getOrder().getId() + 
								"O.d" + ((RmcOrderAgent)schedule.get(currentUnitNo).getDelivery().getOrderAg()).getDeliveries().indexOf(schedule.get(currentUnitNo).getDelivery()) +
								" and CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
					}
					else ; //pass the loading time
				}
				 // isLoaded == true means loading time passed so move to CY 
				else //destination is not null
					if(!moveForward(time)) // if moveforward is true, then it will be simply moving forward
					{
						if (isLoaded == true) {
							if (currentTime >= schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis() 
									+ schedule.get(currentUnitNo).getDelivery().getLoadingDuration().getMillis()
									+ schedule.get(currentUnitNo).getDelivery().getStationToCYTravelTime().getMillis()
									+ schedule.get(currentUnitNo).getDelivery().getUnloadingDuration().getMillis()) { //else pass the unloading time
								logger.debug(truck.getId()+ "T:" + " Delivered Concrete for " + schedule.get(currentUnitNo).getDelivery().getOrder().getId() + 
										"O.d" + ((RmcOrderAgent)schedule.get(currentUnitNo).getDelivery().getOrderAg()).getDeliveries().indexOf(schedule.get(currentUnitNo).getDelivery()) +
										" and CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
								clearDestination();
								isLoaded = false;
								setDestination(((RmcProductionSite)schedule.get(currentUnitNo).getDelivery().getReturnStation()).getLocation() ,  schedule.get(currentUnitNo).getDelivery().getCYToStationTravelTime(), 
										schedule.get(currentUnitNo).getDelivery().getCYToStationTravelTime());
							}
						}
					}
					
			}
			else if (currentTime == schedule.get(currentUnitNo).getTimeSlot().getEndTime().getMillis() && destination != null) {
				moveForward(time);
				if (!moveForward(time)){
					;
					assert getRemainingTime().compareTo(new Duration(0)) == 0 : truck.getId()+ "T: Remaining time is not zero!";
				}
				clearDestination();
				logger.debug(truck.getId()+ "T:" + " back to station  " +((RmcProductionSite)schedule.get(currentUnitNo).getDelivery().getReturnStation()).getStation().getId() +"and CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
			}
			else if (currentUnitNo+1 < schedule.size()) {
				if(currentTime < schedule.get(currentUnitNo +1).getTimeSlot().getStartTime().getMillis()) { //so next Unit's time hasn't started yet..
					clearDestination();//keep on waiting
					isLoaded = false;
				}
				else if (currentTime >= schedule.get(currentUnitNo +1).getTimeSlot().getStartTime().getMillis()) {
//					if (currentUnitNo == 0 && wastedConcreteAddedForFirstDelivery == false) { //TODO check if its still ok after addition of lines 127 to 130? 1/5/2012
//						addWastedConcrete(); //TODO this won't be excuted if there is only one unit in schedule..:(..
//											//so in that case wasted concrete won't be calculated.// but NOW it fixed by adding lines 127 to 130.(1/05/2012)
//						wastedConcreteAddedForFirstDelivery = true;
//						//logger.debug(b.truck.getId()+ "T: wasted concrete called for first delivery");
//					}
					currentUnitNo = currentUnitNo +1;
//					addWastedConcrete();
					//logger.debug(b.truck.getId()+ "T: wasted concrete called other then for first delivery");
				}
			}
//			else if(currentUnitNo+1 == schedule.size() && wastedConcreteAddedForFirstDelivery == false) {
//				addWastedConcrete();
//				wastedConcreteAddedForFirstDelivery = true;
//				logger.debug(b.truck.getId()+ "T: wasted concrete called for first delivery");
//			}
		}
		else //means currentTime < schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis()
			; //keep on waiting till the schedule starts
	}

	
//	private void addWastedConcrete() {
//		
//		if (GlobalVariables.om.getFixedTruckVolume() > schedule.get(currentUnitNo).getDelivery().getDeliveredVolume())
//			
//			b.addWastedConcrete(GlobalVariables.om.getFixedTruckVolume() - schedule.get(currentUnitNo).getDelivery().getDeliveredVolume());
//
//	}
	/**
	 * @return true if made a forward move and false if didn't made a forward move. 
	 */
	public boolean moveForward(TimeLapse time) {
		if (getRemainingTime().compareTo(new Duration(0)) != 0 ) {
			
			if (getRemainingTime().compareTo(getTotalTime()) == 0)//means first move
				logger.debug(truck.getId()+ "T:" + " Leaving for " + schedule.get(currentUnitNo).getDelivery().getOrder().getId() + 
						"O.d" + ((RmcOrderAgent)schedule.get(currentUnitNo).getDelivery().getOrderAg()).getDeliveries().indexOf(schedule.get(currentUnitNo).getDelivery()) +
						" and CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
			//previousTime = new Duration(VirtualClock.currentTime() / 1000); //virtual clock return in micro seconds so convert to MS
			if (getRemainingTime().getMillis() - timeDifference.getMillis() >= 0) {
				decreaseRemainingTime(timeDifference);
				return true;
			}
			else { //no remaininng time is remaining after this last move and remainig time is even less then timeDifference..
				decreaseRemainingTime(new Duration(getRemainingTime().getMillis() - timeDifference.getMillis()));
				return true;  //made a forward move
			}
		}
		return false;  //didn't moved forward
	}
	/**
	 * @description sets destination
	 * @param destination 
	 * @return true/false
	 */
	public boolean setDestination(Point destination, Duration timeToDest, Duration remainingTimeToDest) {
		this.destination = destination;
		this.remainingMsToDestination = remainingTimeToDest;
		this.totalMsRequiredToDestination = timeToDest;
		return true;
	}
	public boolean clearDestination() {
		this.destination = null;
		this.remainingMsToDestination = null;
		this.totalMsRequiredToDestination = null;
		return true;
	}
	public Point getDestination() {
		return destination;
	}
	
	/**
	 * @description returns remaining time in Milliseconds to the current Destination
	 * @return Duration
	 */
	public Duration getRemainingTime() {
		return remainingMsToDestination;
	}
	
	public boolean decreaseRemainingTime(Duration decreasedTime) {
		try {
			remainingMsToDestination = remainingMsToDestination.minus(decreasedTime.getMillis()) ;
			b.totalTravelTime = b.totalTravelTime.plus(decreasedTime.getMillis()); //keeping record of travelling record of truck
		}
		catch (Exception ex) {
			return false;
		}
		return true;
	}
	
	/**
	 * @description returns total time in Milliseconds to the current Destination
	 * @return Duration required to destination
	 */
	public Duration getTotalTime() {
		return totalMsRequiredToDestination;
	}
	

}
