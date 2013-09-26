/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import org.joda.time.Duration;
import static com.google.common.base.Preconditions.checkArgument;

import com.rits.cloning.Cloner;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;

/**
 * @author Shaza
 * Sent by order Agent, when there is change in orderPlan due to truckFailures.
 * These ants became a necessity, since if truck fails just before making a delivery 
 * then there could be delay when truck notices that the intention Ant was unable to 
 * refresh a delivery and the reply of order is REPLY.REJECT. Due to this delay, sometimes 
 * the truck tried to pick parcel delivery, resulting inconsitent state. The more truck failures, 
 * more such situation occurs. So these ants are used
 * to avoid such event and so that truck already remove such deliveries from there schedules
 */
public class OrderPlanInformerAnt extends Ant {
	
	private final communicateAbleUnit commUnit;
	private final OrderAgentInitial originator;
	/**
	 * @param sender
	 * @param del
	 * only used when OrderPlanInformer crated for the first time, should never be used by clone() method.
	 */
	public OrderPlanInformerAnt(CommunicationUser sender, Delivery del) {
		super(sender);
		TimeSlot unitSlot = new TimeSlot(del.getLoadingTime(), del.getUnloadingFinishTime());
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(del.getTruck(), unitSlot, del, del.getWastedVolume(), new Duration( 0)); // so lag time is 0, means at truk side we should not compare lag time.
		commUnit = new communicateAbleUnit(tdu, Reply.NO_REPLY, Reply.REJECT, true);
		originator = (OrderAgentInitial)del.getOrder();
		checkArgument(originator == del.getOrder());
	}

	/**
	 * @param sender
	 * @param commUnit
	 * @param originator
	 * used only by clone()
	 */
	public OrderPlanInformerAnt(CommunicationUser sender, communicateAbleUnit commUnit, CommunicationUser originator) {
		super(sender);
		this.commUnit = commUnit; 
		this.originator = (OrderAgentInitial) originator;
	}
	public communicateAbleUnit getCommUnit() {
		return commUnit;
	}

	public OrderAgentInitial getOriginator() {
		return originator;
	}

	@Override
	public OrderPlanInformerAnt clone(CommunicationUser currentSender) {

		/**
		 * @param pSender The current sender from which the ant is sent to the recepient
		 * @return new OrderPlanInformerAnt, clone of the current one. 
		 */
		final Cloner cl = Utility.getCloner();
		OrderPlanInformerAnt opiAnt = new OrderPlanInformerAnt(currentSender, cl.deepClone(this.commUnit), this.originator ); 
		return opiAnt;
	}	

}
