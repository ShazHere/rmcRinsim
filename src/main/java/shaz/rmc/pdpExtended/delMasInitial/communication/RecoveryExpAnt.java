/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.ArrayList;

import org.joda.time.DateTime;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;

/**
 * Special ExpAnt, that doesn't go from PS to order then again same cycle. Rather it is meant for the breakdown recovery ant.
 * It will be sent by the truck to PS and then to specific order. 
 * @author Shaza
 *
 */
public class RecoveryExpAnt extends ExpAnt {

	public RecoveryExpAnt(CommunicationUser sender,
			ArrayList<TimeSlot> pAvailableSlots,
			ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime) {
		super(sender, pAvailableSlots, pSchedule, pCreateTime);
		// TODO Auto-generated constructor stub
	}

}
