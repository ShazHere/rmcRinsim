/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.VehicleState;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 * @date 19/08/2013
 * 
 * Abstract class to represent States of Truck. State pattern from GoF is followed
 */
abstract class TruckAgentState {

	protected final Logger logger; //for logging
	protected DeliveryTruckInitial truckAgent;
	//since most of the time order remains in IN_RPOCESS state. these arrayLists are ok to be here. 
		//But if in future some other states handle them, then I need to change this.
	protected final ArrayList<ExpAnt> explorationAnts;
	protected final ArrayList<IntAnt> intentionAnts;

	private final TimeSlot totalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//only start and endTime will be used
	protected ArrayList<AvailableSlot> availableSlots; 

	public TruckAgentState(DeliveryTruckInitial pTruck) {
		this.truckAgent = pTruck;
		this.logger = Logger.getLogger(TruckAgentState.class);
		
		totalTimeRange = new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME);
		
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		availableSlots = new ArrayList<AvailableSlot>();
		adjustAvailableSlotInBeginning();
	}

	abstract void processIntentionAnts(long startTime);
	abstract void sendExpAnts(long startTime);
	abstract void sendIntAnts(long startTime);
	abstract void followPracticalSchedule(TimeLapse timeLapse);
	abstract void letMeBreak(long startTime);
	
	/**
	 * Just to avoid that AvailableSlot is 1 if truck schedule is empty.
	 */
	protected void adjustAvailableSlotInBeginning() {
		availableSlots.clear();
		availableSlots.add(new AvailableSlot(new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME), null, null));
	}
	
	protected void addExpAnt(ExpAnt eAnt) {
		explorationAnts.add(eAnt);
	}
	protected void addIntAnt(IntAnt iAnt) {
		intentionAnts.add(iAnt);
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
			default:
				throw new IllegalArgumentException( "Illegal truck Agent State");
		}
	}
	
	
	static final int IN_PROCESS = 0; // The normal and general state
	static final int BROKEN = 1;
	//static final int TEAM_NEED = 0; //Truck is in a transition state w.r.t team. shouldn't send exp or int ants, neither process them. 
}
