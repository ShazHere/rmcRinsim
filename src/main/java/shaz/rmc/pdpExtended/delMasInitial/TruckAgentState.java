/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import shaz.rmc.core.Ant;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.OrderPlanInformerAnt;

/**
 * @author Shaza
 * @date 19/08/2013
 * 
 * Abstract class to represent States of Truck. State pattern from GoF is followed
 */
public abstract class TruckAgentState {

	protected final Logger logger; //for logging
	protected DeliveryTruckInitial truckAgent;
	//since most of the time order remains in IN_RPOCESS state. these arrayLists are ok to be here. 
		//But if in future some other states handle them, then I need to change this.
	protected final ArrayList<ExpAnt> explorationAnts;
	protected final ArrayList<IntAnt> intentionAnts;
	protected final ArrayList<OrderPlanInformerAnt> orderPlanInformerAnts;
	protected final ArrayList<Ant> generalAnts;

	private final TimeSlot totalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//only start and endTime will be used
	protected ArrayList<AvailableSlot> availableSlots; 

	public TruckAgentState(DeliveryTruckInitial pTruck) {
		this.truckAgent = pTruck;
		this.logger = Logger.getLogger(TruckAgentState.class);
		
		totalTimeRange = new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME);
		
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		orderPlanInformerAnts = new ArrayList<OrderPlanInformerAnt>();
		generalAnts = new ArrayList<Ant>();
		availableSlots = new ArrayList<AvailableSlot>();
		Utility.adjustAvailableSlotInBeginning(GlobalParameters.START_DATETIME, availableSlots);
	}

	public abstract void processIntentionAnts(long startTime);
	public abstract void sendExpAnts(long startTime);
	public abstract void sendIntAnts(long startTime);
	public abstract void followPracticalSchedule(TimeLapse timeLapse);
	public abstract void letMeBreak(long startTime);
	public abstract void processGeneralAnts(long startTime);
	public abstract void processOrderPlanInformerAnts(long startTime);
	
	
	
	public void addExpAnt(ExpAnt eAnt) {
		explorationAnts.add(eAnt);
	}
	public void addIntAnt(IntAnt iAnt) {
		intentionAnts.add(iAnt);
	}
	public void addOrderPlanInformerAnt(OrderPlanInformerAnt opiAnt) {
		orderPlanInformerAnts.add(opiAnt);
	}
	public void addGeneralAnt(Ant gAnt) {
		generalAnts.add(gAnt);
	}

	public TimeSlot getTotalTimeRange() {
		return totalTimeRange;
	}
	
	protected void shouldIBreak(long startTime){
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (truckAgent.getPdpModel().getVehicleState(truckAgent).equals(VehicleState.IDLE)
				&& truckAgent.canIBreakAt(currTime, truckAgent.getId())){
			truckAgent.setTruckState(TruckAgentState.BROKEN);
			logger.info(truckAgent.getId()+"T BROKE currTime= " + currTime);
		}
	}
	
	abstract int getStateCode();
	
	static TruckAgentState newState(int newState, DeliveryTruckInitial truckAgent) {
		switch(newState) {
		case IN_PROCESS:
			return new TruckStateInProcess(truckAgent);
		case BROKEN:
			return new TruckStateBroken(truckAgent);
		case TEAM_COMMITMENT:
			return new TruckStateTeamCommitment(truckAgent);
			default:
				throw new IllegalArgumentException( "Illegal truck Agent State");
		}
	}
	
	protected void processOrderPlanInformerAnt(long startTime){
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if ( orderPlanInformerAnts.isEmpty()) 
			return;
		Iterator<OrderPlanInformerAnt> i = orderPlanInformerAnts.iterator();
		while (i.hasNext()) { 
			OrderPlanInformerAnt opiAnt = i.next();
			checkArgument(opiAnt.getCommUnit().getOrderReply() == Reply.REJECT);
			this.removeRejectedUnitFromSchedule(opiAnt.getCommUnit(), currTime);
		}
		orderPlanInformerAnts.clear();
	}
	
	/**
	 * @param u
	 * @param currTime 
	 */
	protected void removeRejectedUnitFromSchedule(communicateAbleUnit u, DateTime currTime) {
		if (u.getOrderReply() == Reply.REJECT && u.isAddedInTruckSchedule() == true){ //means proably orderPlan changed
			truckAgent.getTruckSchedule().remove(u.getTunit());
			if (truckAgent.getTruckSchedule().isEmpty())
				Utility.adjustAvailableSlotInBeginning(currTime, availableSlots);
			else
				truckAgent.getTruckSchedule().adjustTruckSchedule(truckAgent);
			logger.debug(truckAgent.getId()+"T Schedule unit removed in Trucks schedule (status= " +u.getOrderReply()+ ": " + u.getTunit().toString());
		}
		truckAgent.getTruckSchedule().makePracticalSchedule(truckAgent);
	}

	public static final int IN_PROCESS = 0; // The normal and general state
	public static final int BROKEN = 1;
	public static final int TEAM_COMMITMENT = 2;  //Truck is in a transition state w.r.t team. shouldn't send exp or int ants, neither process them.
	 
}
