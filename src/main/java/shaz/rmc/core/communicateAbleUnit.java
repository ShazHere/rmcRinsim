/**
 * 
 */
package shaz.rmc.core;

import java.util.List;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;

/**
 * @author Shaza
 * communicatable unit for the purpose of sharing intentions between truck and order agents. 
 * It is not immutable, since the replies have to be set latter by the agents.
 */
public class communicateAbleUnit {
	
	private final TruckScheduleUnit tunit;
	private Reply psReply;  //ps can have NO_REPLY, UNDERPROCESS, WEEK_ACCEPT, REJECT
	private Reply orderReply;  //order can NO_REPLY, UNDERPROCESS, ACCEPT, WEEK_ACCEPT, REJECT
	private boolean isAddedInTruckSchedule;
	
	public communicateAbleUnit(TruckScheduleUnit tunit, Reply psReply,
			Reply orderReply, boolean isAddedInTruckSchedule) {
		super();
		this.tunit = tunit;
		this.psReply = psReply;
		this.orderReply = orderReply;
		this.isAddedInTruckSchedule = isAddedInTruckSchedule;
	}
	public communicateAbleUnit(TruckScheduleUnit ptruckScheduleUnit){
		this.tunit = ptruckScheduleUnit;
		psReply = Reply.NO_REPLY;
		orderReply = Reply.NO_REPLY;
		isAddedInTruckSchedule = false;
	}
	public Reply getPsReply() {
		return psReply;
	}
	public void setPsReply(Reply psReply) {
		this.psReply = psReply;
	}
	public Reply getOrderReply() {
		return orderReply;
	}
	public void setOrderReply(Reply orderReply) {
		this.orderReply = orderReply;
	}
	public boolean isAddedInTruckSchedule() {
		return isAddedInTruckSchedule;
	}
	public void setAddedInTruckSchedule(boolean isAddedInTruckSchedule) {
		this.isAddedInTruckSchedule = isAddedInTruckSchedule;
	}
	public TruckScheduleUnit getTunit() {
		return tunit;
	}
	public Delivery getDelivery() {
		return tunit.getDelivery();
	}

	public TimeSlot getTimeSlot() {
		return tunit.getTimeSlot();
	}

}
