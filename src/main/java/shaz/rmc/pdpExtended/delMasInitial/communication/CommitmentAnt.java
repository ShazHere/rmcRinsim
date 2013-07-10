/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.List;

import org.joda.time.DateTime;

import com.rits.cloning.Cloner;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.Reply;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;

/**
 * @author Shaza
 *
 */
public class CommitmentAnt extends Ant {
	private final DateTime creationTime;
	private final OrderAgentInitial originator; //the actual truck agent which initialized the intAnt
	private final Delivery failedDelivery;
	private Reply truckReply; 
	private final List<ProductionSiteInitial> possibleSites; //to send which are the sites, from where current order can be fulfilled
	
	public CommitmentAnt(CommunicationUser sender, DateTime pCreateTime, Delivery pDelivery, List<ProductionSiteInitial> pPossibleSites) {
		super(sender);
		originator = (OrderAgentInitial)sender;
		creationTime = pCreateTime;
		failedDelivery = pDelivery;
		possibleSites = pPossibleSites;
		truckReply = Reply.NO_REPLY;
	}

	/**
	 * only to be called by clone method..
	 * @param sender
	 * @param creationTime
	 * @param originator
	 * @param failedDelivery
	 * @param truckReply
	 * @param possibleSites
	 */
	private CommitmentAnt( OrderAgentInitial originator, CommunicationUser sender, DateTime creationTime,
			 Delivery failedDelivery, Reply truckReply, List<ProductionSiteInitial> possibleSites) {
		super(sender);
		this.creationTime = creationTime;
		this.originator = originator;
		this.failedDelivery = failedDelivery;
		this.truckReply = truckReply;
		this.possibleSites = possibleSites;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public OrderAgentInitial getOriginator() {
		return originator;
	}

	public Delivery getFailedDelivery() {
		return failedDelivery;
	}
	public void setTruckReply(Reply pReply) {
		this.truckReply = pReply;
	}
	
	public Reply getTruckReply() {
		return truckReply;
	}

	public List<ProductionSiteInitial> getPossibleSites() {
		return possibleSites;
	}

	@Override
	public CommitmentAnt clone(CommunicationUser currentSender) {
		final Cloner cl = Utility.getCloner();
		CommitmentAnt cAnt = new CommitmentAnt(this.originator, currentSender, this.creationTime, this.failedDelivery, this.truckReply, this.possibleSites); 
				//(currentSender, cl.deepClone(this.communicateAbleSchedule), this.creationTime, this.originator );
		return cAnt;
	}

}
