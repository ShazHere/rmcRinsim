/**
 * 
 */
package shaz.rmc.core;

import org.joda.time.DateTime;

import shaz.rmc.core.domain.Delivery;


/**
 * @author Shaza
 * @description originated by RmcDelivery agent, and always contains some sort of proposal for order agent. 
 * @date 10/11/2011
 * 
 */
public class IntentionAnt extends Ant {

	private int id = 0;
	private static int totalants = 0;
	private Agent originTruck; //primary agent, who is sending expAnt
	private Agent order;
	private Delivery delivery;
	private Reply orderReply;
	private TimeSlot intendedTimeSlot;
	//private DateTime proposedTime; it was used by truck agent while sending the proposal, but i think now its not required
	//declare something that contains proposal
	
	public IntentionAnt(Agent truck) {
		super(truck);
		id = totalants;
		totalants++;
		this.originTruck = truck;
		this.order = null;
		this.delivery = null;
		this.intendedTimeSlot = null;
		//this.proposedTime = null;
		
		this.orderReply = Reply.NO_REPLY;
	}
	
	public boolean configure(Agent order, Delivery intendedDelivery, TimeSlot intendedTs) {
		
		this.order = order;
		this.delivery = intendedDelivery;
		this.intendedTimeSlot = intendedTs;
		
		try {
	//		order.addIntentionAnts(this);
			return true;
		}
		catch (Exception e){
			return false;
		}
	}
	public TimeSlot getIntendedTimeSlot() {
		return this.intendedTimeSlot;
	}
	public void setReply(Reply r) {
		orderReply = r;
	}
	public Reply getReply() {
		return orderReply;
	}
	
		public Agent getDeliveryTruck() {
		return this.originTruck;
	}
	public Delivery getDelivery() {
		return this.delivery;
	}
	public Agent getOrder() {
		return this.order;
	}
}
