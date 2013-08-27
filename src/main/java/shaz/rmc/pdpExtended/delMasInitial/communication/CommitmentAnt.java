/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import java.util.ArrayList;

import org.joda.time.DateTime;

import com.rits.cloning.Cloner;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;

/**
 * @author Shaza
 *
 */
public class CommitmentAnt extends Ant {
	private final DateTime creationTime;
	private final communicateAbleUnit commUnit;
	private final OrderAgentInitial originator;
	//private final ArrayList<TruckScheduleUnit> scheduleToBeAdded; TODO what about relavent travel units? 
	
	
	public CommitmentAnt(CommunicationUser sender, DateTime pCreateTime, TruckDeliveryUnit tdu) {
		super(sender);
		originator = (OrderAgentInitial)sender;
		creationTime = pCreateTime;
		commUnit = new communicateAbleUnit(tdu, Reply.NO_REPLY, Reply.ACCEPT, false); //TODO: should order reply with reply.accept?
		
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
			communicateAbleUnit pCommunicationUnit) {
		super(sender);
		this.creationTime = creationTime;
		this.originator = originator;
		this.commUnit = pCommunicationUnit;
	}

	public DateTime getCreationTime() {
		return creationTime;
	}

	public OrderAgentInitial getOriginator() {
		return originator;
	}

	public communicateAbleUnit getCommUnit() {
		return commUnit;
	}
	

	@Override
	public CommitmentAnt clone(CommunicationUser currentSender) {
		final Cloner cl = Utility.getCloner();
		CommitmentAnt cAnt = new CommitmentAnt(this.originator, currentSender, this.creationTime, this.commUnit); 
		return cAnt;
	}

}
