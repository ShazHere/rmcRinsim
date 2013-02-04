/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.road.MoveProgress;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.domain.Delivery;


/**
 * @author Shaza
 *
 */
public class DeliveryTruckInitialIntention {
	private DeliveryTruckInitial rmcTruck;
	private DeliveryTruckInitialBelief b;
	//private ArrayList<TruckScheduleUnit> schedule;
	
		
	// variables related to current destination
	private int currentUnitNo = 0;
	private Point destination; //contain (CY/PC/null)..basically contains the location the vehicle is currently heading towards 
	
	private final Logger logger; //for logging
	
	public DeliveryTruckInitialIntention(DeliveryTruckInitial a, DeliveryTruckInitialBelief b) {
		this.rmcTruck = a;
		this.b = b;
		//this.schedule = sch;
		
		currentUnitNo = 0; //contains  the schedule unit the truck is currently executing from its schedule, it starts at 0.
		destination = null;

		logger = Logger.getLogger(DeliveryTruckInitialIntention.class);
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
	 * At the beginning, currentUnitNo = 0, isLoaded = false, destination = null
	 * The time between two units could be long, but it will keep on passing quietly, without moving.
	 */
	public void followSchedule(TimeLapse time) {
		long currentTime = GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()); //currentTime contains value in millis
		if (currentUnitNo >= b.schedule.size())
			return;
		Delivery currentDelivery = b.schedule.get(currentUnitNo).getDelivery();
		DeliveryInitial currentParcelDelivery =  ((OrderAgentInitial)currentDelivery.getOrder()).getDeliveryForDomainDelivery(currentDelivery);
		if (currentTime >= b.schedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis() //if current time is equal to trucks currentUnit's starttime
				&& currentTime < b.schedule.get(currentUnitNo).getTimeSlot().getEndTime().getMillis()) { //remain in current unit
				checkArgument( currentParcelDelivery != null, rmcTruck.getId()+ "T: current Parcel delivery shoulnt' be null, current Time = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime())+
						" delivery No = " + currentDelivery.getDeliveryNo() + "truck state = " + rmcTruck.getPdpModel().getVehicleState(rmcTruck));
				if (currentDelivery.getLoadingStation().getLocation() == rmcTruck.getRoadModel().getPosition(rmcTruck) 
						&& rmcTruck.getRoadModel().containsObject(currentParcelDelivery)) {
					logger.debug(rmcTruck.getId()+ "T: Picking up " + currentParcelDelivery.getOrder().getOrder().getId() +"O and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
					rmcTruck.getPdpModel().pickup(rmcTruck, currentParcelDelivery, time); //Picking the deliver
					destination = currentDelivery.getOrder().getPosition(); //get postion of CY i.e unloading location
				} //cuurent time has started, but truck is not at location of pickup, so drive to it..
				if (destination != null) { //so moving could be involved
					if (rmcTruck.getRoadModel().getPosition(rmcTruck) == destination) {
						if (rmcTruck.getPdpModel().containerContains(rmcTruck, currentParcelDelivery) 
								&& rmcTruck.getPdpModel().getVehicleState(rmcTruck) != VehicleState.DELIVERING) {
							logger.debug(rmcTruck.getId()+ "T: Started Delivery and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
							rmcTruck.getPdpModel().deliver(rmcTruck, currentParcelDelivery, time);
							//deliveredCurrentDelivery = true;
						}
						else if (destination != currentDelivery.getReturnStation().getLocation() 
								&& rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0 
								&& rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE) { 
								//&& deliveredCurrentDelivery == true) {
							logger.debug(rmcTruck.getId()+ "T: Done Delivery and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
							destination = currentDelivery.getReturnStation().getLocation();
							rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time);
							
						}
					}
					else if (rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE)
						rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time);
						//logger.debug(rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time).toString());
				}
				else
					logger.debug(rmcTruck.getId()+ "T: Unexpectedly, destination is NULL");
		}
		else if (currentUnitNo < b.schedule.size()-1) 
			if (currentTime >= b.schedule.get(currentUnitNo+1).getTimeSlot().getStartTime().getMillis()) {
				currentUnitNo++;
			}
		else ;//keep on waiting till the schedule starts
	}
}
