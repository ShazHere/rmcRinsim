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
	public
	void processIntentionAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public
	void sendExpAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public
	void sendIntAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public
	void followPracticalSchedule(TimeLapse timeLapse) {
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
	public
	void letMeBreak(long startTime) {
		return;
	}


	@Override
	public void processGeneralAnts(long startTime) {
		return;
		
	}


	@Override
	public void processOrderPlanInformerAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public String toString(){
		return "BROKEN";
	}
}
