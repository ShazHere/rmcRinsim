/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;

/**
 * @author Shaza
 * @date 12/08/2012
 */
/**
 * Manages which truck to fail and at which time. Reply's to qureris of truck related to failure
 * There should be only one object of this managert...apply singleton..
 *
 */
public class TruckAgentFailureManager {
	private final Logger logger; //for logging
	
	private final RandomGenerator rng;
	private final int noOfTrucksToBeFailed;
	private final boolean breakSpecificTruck;
	
	private final Map<Integer, Integer> timeTruck;
	//<MinuteOfDay, TruckId>
	
	public TruckAgentFailureManager(RandomGenerator pRand, int pNoOfTruckToBeFailed) {
		rng = pRand;
		logger = Logger.getLogger(OrderManagerInitial.class);
		noOfTrucksToBeFailed = pNoOfTruckToBeFailed;
		breakSpecificTruck = true;
		
		timeTruck = new  LinkedHashMap<Integer, Integer>();
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN == false)
			return;
		if (breakSpecificTruck)
			setSpecificTruckId();
		else
			setTruckIdAndFailTime();
				
		
	}

	private void setTruckIdAndFailTime() {
		// TODO randomly select truckBreakDowns
		//select timeInMinutes in an array using randomNo gen
		// select truck Id from totalTrucks..oh, need that as well
		// add both in hashMap
	}

	private void setSpecificTruckId() {
		timeTruck.put(706, 3);
		timeTruck.put(900, 5);
	}
	
	protected boolean canIBreakAt (DateTime currTime, int truckId) {
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN == false)
			return false;
		if (timeTruck.get(currTime.getMinuteOfDay()) != null && timeTruck.get(currTime.getMinuteOfDay()) == truckId)
			return true;
		return false;
	}



}
