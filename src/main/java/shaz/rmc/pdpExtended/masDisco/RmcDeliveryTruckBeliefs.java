/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.ExplorationAnt;
import shaz.rmc.core.IntentionAnt;
import shaz.rmc.core.Agent;

/**
 * Keep track of the belifs of RmcTruck. Statistical information can be saved here as well.
 * @author Shaza
 *
 */
public class RmcDeliveryTruckBeliefs {
	private ArrayList<ProposedOrder> proposedOrders;
	Point startLocation;
	ArrayList<shaz.rmc.core.ExplorationAnt> explorationAnts;
	ArrayList<IntentionAnt> intentionAnts;
	
	//for Objective function
	Duration totalTravelTime; //keeps record of the time vehicle kept on travelling
	private int wastedConcrete; // to keep record of wasted concrete
	//available schedule
	private TimeSlot availableslot; //at the moment it only gives the first available time slot later i should use an array.
	// availableSlot the next proposable time slot for this truck. Its location gives the next PC location, the truck may go after the delivery

	double explorationRandomFactor; //used as a random no, so that all the trucks donot query the schedular at the same time after same interval. 
	//Their interval is constant, but due to this random factor, they won't query at same time. 
	
	RmcDeliveryTruckBeliefs() {
		proposedOrders = new ArrayList<RmcDeliveryTruckBeliefs.ProposedOrder>();
		explorationRandomFactor = GlobalParameters.RANDOM_DATA_GEN.nextInt(0, 10);
		
		wastedConcrete = 0;
		totalTravelTime = new Duration(0); //to keep record of total travelling time, to be used by objective function latter.
		
		explorationAnts = new ArrayList<ExplorationAnt>();
		intentionAnts = new ArrayList<IntentionAnt>();
		availableslot = new TimeSlot();
		availableslot.setStartTime(GlobalParameters.START_DATETIME);
		availableslot.setEndtime(GlobalParameters.END_DATETIME);
		
	}
	
public class ProposedOrder {
		
		public RmcOrderAgent orderAgent;
		public int noOfTimeProposed;
		public ProposedOrder(RmcOrderAgent or, int no) {
			this.orderAgent = or;
			this.noOfTimeProposed = no;
		}
		public RmcOrderAgent getOrderAgent () {
			return this.orderAgent;
		}
		public int getNoOfTimes(){
			return noOfTimeProposed;
		}
		public void incrementNoOfTimes() {
			noOfTimeProposed = noOfTimeProposed + 1;
		}
	}


public ArrayList<ProposedOrder> getProposedOrders() {
	return proposedOrders;
}

public boolean addProposedOrder(ProposedOrder pProposedOrder) {
	if (this.proposedOrders != null) {
		this.proposedOrders.add(pProposedOrder);
		return true;
	}
	return false;
}

public TimeSlot getAvailableslot() {
	return availableslot;
}

public void setAvailableslot(TimeSlot availableslot) {
	this.availableslot = availableslot;
}
public void setAvailableslot(DateTime startTime, DateTime endTime, Point startTimeLocation, RmcProductionSite startTimeSite) {
	this.availableslot.setStartTime(new DateTime(startTime));
	this.availableslot.setEndtime(new DateTime(endTime));
	this.availableslot.setLocationAtStartTime (startTimeLocation, startTimeSite);
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
}
