/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import static com.google.common.base.Preconditions.checkArgument;


import java.util.ArrayList;

import org.joda.time.DateTime;

import com.rits.cloning.Cloner;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Agent;
import shaz.rmc.core.Ant;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;

/**
 * @author Shaza
 *
 */
public class IntAnt extends Ant {

	private final ArrayList<communicateAbleUnit> communicateAbleSchedule;
	private final DeliveryTruckInitial originator; //the actual truck agent which initialized the intAnt
	private communicateAbleUnit currentUnit;
	private int currentUnitNo;
	private final DateTime creationTime;

	
	public IntAnt(CommunicationUser sender,
			 ArrayList<communicateAbleUnit> pSchedule, DateTime pCreateTime) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;
		communicateAbleSchedule = pSchedule;
		creationTime = pCreateTime;
		currentUnitNo = 0;
		if (!communicateAbleSchedule.isEmpty())
			currentUnit = communicateAbleSchedule.get(currentUnitNo);
	}

	//TODO: check if clone requires any other copying stuff?
	/**
	 * only to be used by the clone method.
	 * @param sender
	 * @param pSchedule
	 * @param pCreateTime
	 * @param pOriginator
	 */
	private IntAnt(CommunicationUser sender,
			 ArrayList<communicateAbleUnit> pSchedule, DateTime pCreateTime, CommunicationUser pOriginator) {
		super(sender);
		originator = (DeliveryTruckInitial)pOriginator;
		communicateAbleSchedule = pSchedule;
		creationTime = pCreateTime;
		
		if (!communicateAbleSchedule.isEmpty())
			currentUnit = communicateAbleSchedule.get(0);
	}

	@Override
	public IntAnt clone(CommunicationUser pSender) {
		final Cloner cl = Utility.getCloner();
		IntAnt iAnt = new IntAnt(pSender, cl.deepClone(this.communicateAbleSchedule), this.creationTime, this.originator );
		iAnt.currentUnitNo = this.currentUnitNo;
		iAnt.currentUnit = iAnt.communicateAbleSchedule.get(iAnt.currentUnitNo);
		return iAnt;
	}
	public DeliveryTruckInitial getOriginator() {
		return originator;
	}
	//TODO: shouldn't i return defensive copy? or what to do?
	public communicateAbleUnit getCurrentUnit() {
		return this.currentUnit;
	}
	public void setNextCurrentUnit() {
		if (currentUnitNo < communicateAbleSchedule.size()-1) {
			currentUnitNo += 1;
			currentUnit = communicateAbleSchedule.get(currentUnitNo);
		}
	}
	
	/**
	 * similar to exp.isSchedule complete. Checks if whole schedule is completed
	 * @return
	 */
	public boolean isScheduleComplete() { 
		if (currentUnitNo >= communicateAbleSchedule.size()-1){ //still need to check if currentUnit is required to be checked 
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
		checkArgument(this.communicateAbleSchedule.isEmpty() == false, true);
		for (communicateAbleUnit newu: this.communicateAbleSchedule) {
			if (newu.getPsReply() == Reply.REJECT || newu.getOrderReply() == Reply.REJECT) {//if any reply REJECT, dnot consider schedule
				return false;
			}
			checkArgument((newu.getPsReply() == Reply.NO_REPLY || newu.getOrderReply() == Reply.NO_REPLY) == false, true);
				//return false;
		}
		if (!existingSchedule.isEmpty()){
			boolean unitExist = false;
			for (TruckScheduleUnit u : existingSchedule) {//for each of existing schedule in truck
				unitExist = false;
				for (communicateAbleUnit newu: this.communicateAbleSchedule) { //chek if existing unit, exists in iAnt as well..
					if (u.getDelivery().equals(newu.getTunit().getDelivery()) && unitExist == false) {
						unitExist = true;
						//checkArgument(u.isAddedInTruckSchedule() == true, true);
						checkArgument(newu.getOrderReply() == Reply.WEEK_ACCEPT && newu.getPsReply() == Reply.WEEK_ACCEPT, true);
						break;
					}
				}
				checkArgument(unitExist, true);
				if (!unitExist) //means a previous unit doesn't exist...theoratically this should never be the case, because none deletes the existing units of an ant!
					return false;
			}
			
		}
		return true; //if not returned due to any reason so far then probably its valid..
	}
	@Override
	public CommunicationUser getSender() {
		return super.getSender();
	}

	public ArrayList<communicateAbleUnit> getSchedule() {
		return communicateAbleSchedule;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}
}
