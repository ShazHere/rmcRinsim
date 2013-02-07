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
import shaz.rmc.core.domain.Delivery;
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
		final Cloner cl = new Cloner();
		cl.dontCloneInstanceOf(Agent.class);
		cl.dontCloneInstanceOf(ProductionSite.class);
		cl.registerImmutable(DateTime.class);
		//cl.dontCloneInstanceOf(Delivery.class);
		//cl.setDumpClonedClasses(true);
		schedule = cl.deepClone(pSchedule);
		
		creationTime = pCreateTime;
		currentUnitNo = 0;
		if (!schedule.isEmpty()) {
			currentUnit = schedule.get(currentUnitNo);
			for(TruckScheduleUnit u: schedule) {
				u.setOrderReply(Reply.NO_REPLY);
				u.setPsReply(Reply.NO_REPLY);
			}
		}
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
	 * similar to exp.isSchedule complete. Checks if whole schedule is completed
	 * @return
	 */
	public boolean isScheduleComplete() { 
		if (currentUnitNo >= schedule.size()-1){ //still need to check if currentUnit is required to be checked 
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
		checkArgument(this.schedule.isEmpty() == false, true);
		for (TruckScheduleUnit newu: this.schedule) {
			if (newu.getPsReply() == Reply.REJECT || newu.getOrderReply() == Reply.REJECT) {//if any reply REJECT, dnot consider schedule
				return false;
			}
		}
		
		
		if (!existingSchedule.isEmpty()){
			boolean unitExist = false;
			for (TruckScheduleUnit u : existingSchedule) {//for each of existing schedule in truck
				unitExist = false;
				for (TruckScheduleUnit newu: this.schedule) { //chek if existing unit, exists in iAnt as well..
					if (u.getDelivery().equalsWithSameTruck(newu.getDelivery()) && unitExist == false) {
						unitExist = true;
						checkArgument(u.isAddedInTruckSchedule() == true, true);
						checkArgument(newu.getOrderReply() == Reply.WEEK_ACCEPT && newu.getPsReply() == Reply.WEEK_ACCEPT, true);
					}
					else {
						//checkArgument(u.isAddedInTruckSchedule() == false, true);
						//checkArgument(newu.getOrderReply() == Reply.UNDER_PROCESS && newu.getPsReply() == Reply.UNDER_PROCESS, true);
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

	public ArrayList<TruckScheduleUnit> getSchedule() {
		return schedule;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}
}
