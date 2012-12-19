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
	
	/**
	 * similar to exp.isSchedule complete
	 * @return
	 */
	public boolean isScheduleComplete() { //is whole schedule completed 
		if (currentUnitNo >= schedule.size()-1){
			if (currentUnit.getOrderReply() != Reply.NO_REPLY && currentUnit.getPsReply() != Reply.NO_REPLY) //check if the currentUnit is processed and replies got
				return true;
			else 
				return false;
		}
		else
			return false;
	}
	
	/**
	 * before returning, it also removes the REJECTed unit from the intAnt's schedule
	 * @return true if the schedule within intAnt contains atleast one unit with both 
	 * orderReply and psReply != REJECT
	 * 
	 * else return false
	 * 
	 */
//	public boolean isConsiderable() {
//		ArrayList<TruckScheduleUnit> removableUnits = new ArrayList<TruckScheduleUnit>();
//		if (!this.schedule.isEmpty()){
//			for (TruckScheduleUnit u : this.schedule)
//				if (u.getPsReply() == Reply.REJECT || u.getOrderReply() == Reply.REJECT)
//					removableUnits.add(u); //remove if there are rejected units so that only acceptable units are considered.
//		}
//		if (!removableUnits.isEmpty()){
//			for (TruckScheduleUnit u : removableUnits) {
//				this.schedule.remove(u);
//			}
//		}
//		if (this.schedule.isEmpty())
//			return false;
//		else 
//			return true;
//	}
	//TODO test this method, may use test cases
	public boolean isConsiderable(final ArrayList<TruckScheduleUnit> existingSchedule) {
		if (!this.schedule.isEmpty()){
			for (TruckScheduleUnit u : this.schedule) {
				if (u.getPsReply() == Reply.REJECT || u.getOrderReply() == Reply.REJECT) {//should be then in existing schedule
					boolean rejectAble = true;
					for (TruckScheduleUnit existUnit : existingSchedule) {
						if (u.getDelivery().equals(existUnit.getDelivery()))    //TODO shud i check timeslots as well?
							rejectAble = false;
					}
					if (rejectAble)
						return false;
				}
			}
		}
		else
			return false;
		return true;
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
