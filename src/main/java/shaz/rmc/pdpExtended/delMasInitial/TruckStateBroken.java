/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;

import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;

/**
 * @author Shaza
 *
 */
public class TruckStateBroken extends TruckAgentState {

	boolean extraUnitsRemoved;
	/**
	 * @param truckAgent 
	 * 
	 */
	public TruckStateBroken(DeliveryTruckInitial truckAgent) {
		super(truckAgent);
		extraUnitsRemoved = false;
	}


	@Override
	int getStateCode() {
		return TruckAgentState.BROKEN;
	}


	@Override
	void processIntentionAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	void sendExpAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	void sendIntAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	void followPracticalSchedule(TimeLapse timeLapse) {
		//TODO: it should empty the schedule Units comming after the truck broken time.		
		//if this method is called, means truck has already broken
		if (extraUnitsRemoved)
			return;
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		ArrayList<TruckDeliveryUnit> deleteAble = new ArrayList<TruckDeliveryUnit>();
		for (TruckScheduleUnit tsu : truckAgent.getSchedule()){
			if (tsu.getTimeSlot().getEndTime().compareTo(currTime) > 0 && tsu instanceof TruckDeliveryUnit)
				deleteAble.add((TruckDeliveryUnit)tsu);
		}
		for (TruckDeliveryUnit tdu: deleteAble) {
			truckAgent.removeFromSchedule(tdu);
		}
		extraUnitsRemoved = true;
	}


	@Override
	void letMeBreak(long startTime) {
		// TODO Auto-generated method stub
		
	}

}
