/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import shaz.rmc.core.Ant;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

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
		generalAnts = new ArrayList<Ant>();
		availableSlots = new ArrayList<AvailableSlot>();
		adjustAvailableSlotInBeginning();
	}

	public abstract void processIntentionAnts(long startTime);
	public abstract void sendExpAnts(long startTime);
	public abstract void sendIntAnts(long startTime);
	public abstract void followPracticalSchedule(TimeLapse timeLapse);
	public abstract void letMeBreak(long startTime);
	public abstract void processGeneralAnts(long startTime);
	
	/**
	 * Just to avoid that AvailableSlot is 1 if truck schedule is empty.
	 */
	protected void adjustAvailableSlotInBeginning() {
		availableSlots.clear();
		availableSlots.add(new AvailableSlot(new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME), null, null));
	}
	
	public void addExpAnt(ExpAnt eAnt) {
		explorationAnts.add(eAnt);
	}
	public void addIntAnt(IntAnt iAnt) {
		intentionAnts.add(iAnt);
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
			logger.debug(truckAgent.getId()+"T BROKE currTime= " + currTime);
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
	
	
	public static final int IN_PROCESS = 0; // The normal and general state
	public static final int BROKEN = 1;
	public static final int TEAM_COMMITMENT = 2;  //Truck is in a transition state w.r.t team. shouldn't send exp or int ants, neither process them.
	 
}
