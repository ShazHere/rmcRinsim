/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.math3.random.MersenneTwister;
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
	private final Map<Integer, Integer> truckTime;
	//<truckId, MinuteOfDay>
	
	public TruckAgentFailureManager(RandomGenerator pRand, int pNoOfTruckToBeFailed) {
		rng = pRand;
		logger = Logger.getLogger(OrderManagerInitial.class);
		noOfTrucksToBeFailed = pNoOfTruckToBeFailed;
		breakSpecificTruck = false;
		
		timeTruck = new  LinkedHashMap<Integer, Integer>();
		truckTime = new  LinkedHashMap<Integer, Integer>();
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN == false)
			return;
		if (breakSpecificTruck)
			setSpecificTruckId();
		else
			setTruckIdAndFailTime();
				
		
	}

	/**
	 *  Adds truck id, according to probablity
	 */
	private void setTruckIdAndFailTime() {
		for (int i = 1; i <= GlobalParameters.PROBLEM.getVehicles().size(); i++) {
			if (rng.nextInt(100) < GlobalParameters.TRUCK_BREAKDOWN_PROBABILTY) //so take a percentage value, if that is below GlobalParameters.TRUCK_BREAKDOWN_PROBABILTY 
				//then break the truck
				putInTimeTruck(i);
		}
	}

	
	private void putInTimeTruck(int truckId) {
		//select time during the day
		int startMinute = GlobalParameters.START_DATETIME.getMinuteOfDay();
		int endMinute = GlobalParameters.END_DATETIME.getMinuteOfDay();
		int breakMin = rng.nextInt(endMinute - startMinute) + startMinute;
		timeTruck.put(breakMin, truckId);
		truckTime.put(truckId, breakMin);
	}

	private void setSpecificTruckId() {
		timeTruck.put(840, 1);
		//timeTruck.put(900, 5);
		truckTime.put(840, 1);
	}
	
	protected boolean canIBreakAt (DateTime currTime, int truckId) {
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN == false)
			return false;
		//if (timeTruck.get(currTime.getMinuteOfDay()) != null && timeTruck.get(currTime.getMinuteOfDay()) == truckId)
			//return true;
		if (truckTime.get(truckId) != null){
			if (currTime.getMinuteOfDay() >= truckTime.get(truckId))
				return true;
		}
		return false;
	}



}
