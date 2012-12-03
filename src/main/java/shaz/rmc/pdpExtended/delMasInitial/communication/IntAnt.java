/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.ArrayList;
import java.util.Random;

import org.joda.time.DateTime;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;

/**
 * @author Shaza
 *
 */
public class IntAnt extends Ant {

	private final ArrayList<TruckScheduleUnit> schedule;
	private final DeliveryTruckInitial originator; //the actual truck agent which initialized the intAnt
	private TruckScheduleUnit currentUnit;
	private int currentUnitNo;
	private final DateTime creationTime;

	
	public IntAnt(CommunicationUser sender,
			 ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;
		schedule = new ArrayList<TruckScheduleUnit>(pSchedule);
		creationTime = pCreateTime;
		currentUnitNo = 0;
		if (!schedule.isEmpty())
			currentUnit = schedule.get(currentUnitNo);
	}

	//TODO: check if clone requires any other copying stuff?
	public IntAnt(CommunicationUser sender,
			 ArrayList<TruckScheduleUnit> pSchedule, DateTime pCreateTime, CommunicationUser pOriginator) {
		super(sender);
		originator = (DeliveryTruckInitial)pOriginator;
		schedule = new ArrayList<TruckScheduleUnit>(pSchedule);
		creationTime = pCreateTime;
		
		if (!schedule.isEmpty())
			currentUnit = schedule.get(0);
	}

	@Override
	public IntAnt clone(CommunicationUser pSender) {
		IntAnt iAnt = new IntAnt(pSender, this.schedule, this.creationTime, this.originator );
		iAnt.currentUnit = this.currentUnit;
		iAnt.currentUnitNo= this.currentUnitNo;
		return iAnt;
	}
	public DeliveryTruckInitial getOriginator() {
		return originator;
	}

	public TruckScheduleUnit getCurrentUnit() {
		return this.currentUnit;
	}
	public void setNextCurrentUnit() {
		if (currentUnitNo < schedule.size()-1) {
			currentUnitNo += 1;
			currentUnit = schedule.get(currentUnitNo);
		}
	}
	public boolean isScheduleComplete() { //is whole schedule completed 
		if (currentUnitNo >= schedule.size()-1){
			if (currentUnit.getOrderReply() != Reply.NO_REPLY && currentUnit.getPsReply() != Reply.NO_REPLY)
				return true;
			else 
				return false;
		}
		else
			return false;
	}

	@Override
	public CommunicationUser getSender() {
		return super.getSender();
	}

	public ArrayList<TruckScheduleUnit> getSchedule() {
		return schedule;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}
}
