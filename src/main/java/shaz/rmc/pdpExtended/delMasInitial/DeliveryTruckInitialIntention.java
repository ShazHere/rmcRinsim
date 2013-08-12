/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.rits.cloning.Cloner;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import rinde.sim.core.model.road.MoveProgress;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Station;


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
	//private Point destination; //contain (CY/PC/null)..basically contains the location the vehicle is currently heading towards
	private boolean doneCurrentUnit;
	private boolean moveToOrder = false;
	//private boolean moveToPS = false;
	
	private final Logger logger; //for logging
	
	public DeliveryTruckInitialIntention(DeliveryTruckInitial a, DeliveryTruckInitialBelief b) {
		this.rmcTruck = a;
		this.b = b;
		//this.schedule = sch;
		
		currentUnitNo = 0; //contains  the schedule unit the truck is currently executing from its schedule, it starts at 0.
		//destination = null;
		doneCurrentUnit = false;

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
		//previousFollowSchedule(time);
		//currentFollowSchedule(time);
		//ArrayList<TruckScheduleUnit> practicalSchedule = rmcTruck.getSchedule();
		ArrayList<TruckScheduleUnit> practicalSchedule = rmcTruck.getPracticalSchedule(); //enabling this metthod is taking 43 seconds vs 10 second during computation..:(
		newFollowSchedule(time, practicalSchedule);
	}
	

	private void newFollowSchedule(TimeLapse time, ArrayList<TruckScheduleUnit> practicalSchedule) {
		long currentTime = GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()); //currentTime contains value in millis
		if (currentUnitNo >= practicalSchedule.size())
			return;
		
		if (isInCurrentUnitTime(currentTime, practicalSchedule)) { //remain in current unit		
				if (practicalSchedule.get(currentUnitNo) instanceof TruckDeliveryUnit) {
					TruckDeliveryUnit currUnit = (TruckDeliveryUnit)practicalSchedule.get(currentUnitNo);
					Delivery currentDelivery = ((TruckDeliveryUnit)practicalSchedule.get(currentUnitNo)).getDelivery();
					DeliveryInitial currentParcelDelivery =  ((OrderAgentInitial)currentDelivery.getOrder()).getDeliveryForDomainDelivery(currentDelivery);
					checkArgument( currentParcelDelivery != null, rmcTruck.getId()+ "T: current Parcel delivery shoulnt' be null, current Time = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime())+
							" delivery No = " + currentDelivery.getDeliveryNo() + "truck state = " + rmcTruck.getPdpModel().getVehicleState(rmcTruck));

					if (rmcTruck.getRoadModel().getPosition(rmcTruck).equals(currentDelivery.getLoadingStation().getLocation())) { //tuck at StartPS
						if(rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE ) {
							if (rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0 ) {
								checkArgument(rmcTruck.getRoadModel().containsObject(currentParcelDelivery) == true, true);
								logger.info(rmcTruck.getId()+ "T: Picking up " + currentParcelDelivery.getOrder().getOrder().getId() +"O and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
								rmcTruck.getPdpModel().pickup(rmcTruck, currentParcelDelivery, time); //Picking the deliver
							}
							else { //truck is filled and IDLE so start moving to OrderSite
								rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getOrder().getPosition(), time); 
								moveToOrder = true;
							}
						}
						else checkArgument(rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.PICKING_UP);
						
					}
					else if (moveToOrder) {
						if (rmcTruck.getRoadModel().getPosition(rmcTruck).equals(currentDelivery.getOrder().getPosition())) {
							moveToOrder = false;
							logger.info(rmcTruck.getId()+ "T: Started Deliverying and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
							rmcTruck.getPdpModel().deliver(rmcTruck, currentParcelDelivery, time);
						}
						else //move to order
							rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getOrder().getPosition(), time);
					}
					else if (rmcTruck.getRoadModel().getPosition(rmcTruck).equals(currentDelivery.getOrder().getPosition())) { //truck at order
						if (rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE  
								&& doneCurrentUnit == false) { 
							checkArgument( rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0, true);
							logger.info(rmcTruck.getId()+ "T: Done Deliverying and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
							doneCurrentUnit = true;
						} //else passing delivery time
					}
				}
				else { //current unit is Travel unit//if (moveToPS == true){ //deliveredCurrentDelivery = true
					TruckTravelUnit currUnit = (TruckTravelUnit)practicalSchedule.get(currentUnitNo);
					if (rmcTruck.getRoadModel().getPosition(rmcTruck).equals(currUnit.getEndLocation())) {
						logger.info(rmcTruck.getId()+ "T: Back to PS and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
						checkArgument(moveToOrder == false, true);
						doneCurrentUnit = true; //jsut to
					}
					else
						rmcTruck.getRoadModel().moveTo(rmcTruck, currUnit.getEndLocation(), time);
				}
		}
		else if (currentUnitNo < practicalSchedule.size()-1) 
			if (currentTime >= practicalSchedule.get(currentUnitNo+1).getTimeSlot().getStartTime().getMillis()) {
				//rmcTruck.getRoadModel().moveTo(rmcTruck, b.schedule.get(currentUnitNo).getEndLocation(), time); //just as a precaution, if last step is remaining..
				//checkArgument(rmcTruck.getRoadModel().getPosition(rmcTruck) == destination);
				checkArgument(practicalSchedule.get(currentUnitNo).getEndLocation().equals(practicalSchedule.get(currentUnitNo+1).getStartLocation()), true);
				checkArgument(rmcTruck.getRoadModel().getPosition(rmcTruck).equals(practicalSchedule.get(currentUnitNo+1).getStartLocation()), true);
				currentUnitNo++;
				doneCurrentUnit = false;
			}
	}
	/**
	 * @param currentTime
	 * @return
	 */
	private boolean isInCurrentUnitTime(long currentTime, ArrayList<TruckScheduleUnit> practicalSchedule) {
		return currentTime >= practicalSchedule.get(currentUnitNo).getTimeSlot().getStartTime().getMillis() //if current time is equal to trucks currentUnit's starttime
				&& currentTime <= practicalSchedule.get(currentUnitNo).getTimeSlot().getEndTime().plusMillis(500).getMillis();
	}
	
//	private void currentFollowSchedule(TimeLapse time) {
//		long currentTime = GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()); //currentTime contains value in millis
//		if (currentUnitNo >= b.schedule.size())
//			return;
//		Delivery currentDelivery = b.schedule.get(currentUnitNo).getDelivery();
//		DeliveryInitial currentParcelDelivery =  ((OrderAgentInitial)currentDelivery.getOrder()).getDeliveryForDomainDelivery(currentDelivery);
//		if (isInCurrentUnitTime(currentTime)) { //remain in current unit
//				checkArgument( currentParcelDelivery != null, rmcTruck.getId()+ "T: current Parcel delivery shoulnt' be null, current Time = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime())+
//						" delivery No = " + currentDelivery.getDeliveryNo() + "truck state = " + rmcTruck.getPdpModel().getVehicleState(rmcTruck));
//				if (deliveredCurrentDelivery == false)
//				{
//					if (rmcTruck.getRoadModel().getPosition(rmcTruck) == currentDelivery.getLoadingStation().getLocation()) { //tuck at StartPS
//						if(rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE ) {
//							if (rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0 ) {
//								checkArgument(rmcTruck.getRoadModel().containsObject(currentParcelDelivery) == true, true);
//								logger.info(rmcTruck.getId()+ "T: Picking up " + currentParcelDelivery.getOrder().getOrder().getId() +"O and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
//								rmcTruck.getPdpModel().pickup(rmcTruck, currentParcelDelivery, time); //Picking the deliver
//							}
//							else { //truck is filled and IDLE so start moving to OrderSite
//								rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getOrder().getPosition(), time); 
//								moveToOrder = true;
//							}
//						}
//						else checkArgument(rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.PICKING_UP);
//						
//					}
//					else if (moveToOrder) {
//						if (rmcTruck.getRoadModel().getPosition(rmcTruck) == currentDelivery.getOrder().getPosition()) {
//							moveToOrder = false;
//							logger.info(rmcTruck.getId()+ "T: Started Deliverying and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
//							rmcTruck.getPdpModel().deliver(rmcTruck, currentParcelDelivery, time);
//							checkArgument(moveToPS == false, true);
//						}
//						else //move to order
//							rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getOrder().getPosition(), time);
//					}
//					else if (rmcTruck.getRoadModel().getPosition(rmcTruck) == currentDelivery.getOrder().getPosition()) { //truck at order
//						if (rmcTruck.getPdpModel().getVehicleState(rmcTruck) == VehicleState.IDLE  ) { 
//							checkArgument( rmcTruck.getPdpModel().getContentsSize(rmcTruck) == 0, true);
//							logger.info(rmcTruck.getId()+ "T: Done Deliverying and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
//							rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getReturnStation().getLocation(), time);
//							moveToPS = true;
//							deliveredCurrentDelivery = true;
//						} //else passing delivery time
//					}
//				}
//				else if (moveToPS == true){ //deliveredCurrentDelivery = true
//					if (rmcTruck.getRoadModel().getPosition(rmcTruck) == currentDelivery.getReturnStation().getLocation()) {
//						moveToPS = false;
//						logger.info(rmcTruck.getId()+ "T: Back to PS and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + time.getStartTime()) );
//						checkArgument(moveToOrder == false, true);
//					}
//					else
//						rmcTruck.getRoadModel().moveTo(rmcTruck, currentDelivery.getReturnStation().getLocation(), time);
//				}
//
//					//else 
//				
//		}
//		else if (currentUnitNo < b.schedule.size()-1) 
//			if (currentTime >= b.schedule.get(currentUnitNo+1).getTimeSlot().getStartTime().getMillis()) {
//				//rmcTruck.getRoadModel().moveTo(rmcTruck, destination, time); //just as a precaution, if last step is remaining..
//				//checkArgument(rmcTruck.getRoadModel().getPosition(rmcTruck) == destination);
//				checkArgument(b.schedule.get(currentUnitNo).getDelivery().getReturnStation() == b.schedule.get(currentUnitNo+1).getDelivery().getLoadingStation(), true);
//				checkArgument(rmcTruck.getRoadModel().getPosition(rmcTruck).equals(b.schedule.get(currentUnitNo+1).getDelivery().getLoadingStation().getLocation()), true);
//				currentUnitNo++;
//				deliveredCurrentDelivery = false;
//				//destination = null;
//			}
//	}


}
