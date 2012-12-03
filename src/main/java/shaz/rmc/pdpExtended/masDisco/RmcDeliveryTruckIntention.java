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
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.road.RoadModel;

import shaz.rmc.core.Agent;
import shaz.rmc.core.TruckScheduleUnit;

/**
 * Performs the physical world interaction. Fullfills the current intention.
 * @author Shaza
 *
 */
public class RmcDeliveryTruckIntention {
	private RmcDeliveryTruck rmcTruck;
	private RmcDeliveryTruckBeliefs b;
	private ArrayList<TruckScheduleUnit> schedule;
	//private Duration previousTime = null;
	
//	boolean isLoaded;	//Is the vehicle loaded with concrete?
	
	// variables related to current destination
	int currentUnitNo = 0;
	Point destination; //contain (CY/PC/null)..basically contains the location the vehicle is currently heading towards
//	Duration remainingMsToDestination;  //remaining MS to destination
//	Duration totalMsRequiredToDestination; //total time required to reach destination extracted from the problem file
//	boolean wastedConcreteAddedForFirstDelivery;
	
	final Logger logger; //for logging
	
	public RmcDeliveryTruckIntention(RmcDeliveryTruck a, RmcDeliveryTruckBeliefs b, ArrayList<TruckScheduleUnit> sch) {
		this.rmcTruck = a;
		this.b = b;
	//	timeDifference = new Duration(200); //200 coz agent processTick is called after 200,000microseconds, so we will reduce 200MS
		this.schedule = sch;
		
		currentUnitNo = 0; //contains  the schedule unit the truck is currently executing from its schedule, it starts at 0.
		destination = null;
//		remainingMsToDestination = new Duration(0);
//		totalMsRequiredToDestination= null;
//		isLoaded = false;
	
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
		if (currentUnitNo >= schedule.size())
			return;
		RmcDelivery currentParcelDelivery =  ((RmcOrderAgent)schedule.get(currentUnitNo).getDelivery().getOrder()).getDeliveryForDomainDelivery(schedule.get(currentUnitNo).getDelivery());
		if (currentTime >= schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis()
				&& currentTime < schedule.get(currentUnitNo).getTimeSlot().getEndTime().getMillis()) { //remain in current unit
				if (schedule.get(currentUnitNo).getDelivery().getLoadingStation().getLocation() == rmcTruck.getRoadModel().getPosition(rmcTruck) 
						&& rmcTruck.getRoadModel().containsObject(currentParcelDelivery)) {
					logger.debug(rmcTruck.getId()+ "T: Picking up  and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
					rmcTruck.getPdpModel().pickup(rmcTruck, currentParcelDelivery, time); //Picking the deliver
					destination = schedule.get(currentUnitNo).getDelivery().getOrder().getPosition(); //get postion of CY i.e unloading location
				}
				if (destination != null) { //so moving could be involved
					if (rmcTruck.getRoadModel().getPosition(rmcTruck) == destination) {
						if (rmcTruck.getPdpModel().containerContains(rmcTruck, currentParcelDelivery) 
								&& rmcTruck.getPdpModel().getVehicleState(rmcTruck) != VehicleState.DELIVERING) {
							rmcTruck.getPdpModel().deliver(rmcTruck, currentParcelDelivery, time);	
						}
						else if (destination != schedule.get(currentUnitNo).getDelivery().getReturnStation().getLocation() 
								&& rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0) {
							logger.debug(rmcTruck.getId()+ "T: Done Delivery and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
							destination = schedule.get(currentUnitNo).getDelivery().getReturnStation().getLocation();
							rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time);
							
						}
					}
					else 
						rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time);
				}
				else
					logger.debug(rmcTruck.getId()+ "T: Unexpectedly, destination is NULL");
		}
		else if (currentUnitNo < schedule.size()-1) 
			if (currentTime >= schedule.get(currentUnitNo+1).getTimeSlot().getStartTime().getMillis())
				currentUnitNo++; 
		else ;//keep on waiting till the schedule starts
	}

//	public Point getDestination() {
//		return destination;
//	}
//	

}
