/**
 * 
 */
package shaz.rmc.core;


/**
 * @author Shaza
 * @date 11/07/2013
 * immutable
 *
 */
public class TruckTravelUnit extends TruckScheduleUnit {

	public TruckTravelUnit(Agent pTruck) {
		super(pTruck);
	}
	public TruckTravelUnit(Agent pTruck, TimeSlot pSlot) {
		super(pTruck, pSlot);
	}

}
