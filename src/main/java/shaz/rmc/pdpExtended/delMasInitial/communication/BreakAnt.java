/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import rinde.sim.core.model.communication.CommunicationUser;
import org.joda.time.DateTime;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.core.domain.Delivery;

/**
 * The purpose is to inform order that the truck has broken down, and the corresponding delivery couldn't be accomplished.
 * TODO: I think, we don't even require it. Since we can stop sending refresh of intention ants and order could know that the
 * delivery is not further intended by truck. 
 * @author Shaza
 *
 */
public class BreakAnt extends Ant {

	private final DateTime creationTime;
	private final DeliveryTruckInitial originator; //the actual truck agent which initialized the intAnt
	private final Delivery failedDelivery;
	
	public BreakAnt(CommunicationUser sender, DateTime pCreateTime, Delivery pDelivery) {
		super(sender);
		originator = (DeliveryTruckInitial)sender;
		creationTime = pCreateTime;
		failedDelivery = pDelivery;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public DeliveryTruckInitial getOriginator() {
		return originator;
	}

	public Delivery getFailedDelivery() {
		return failedDelivery;
	}

	@Override
	public Ant clone(CommunicationUser currentSender) {
		// TODO Auto-generated method stub
		return null;
	}

}

