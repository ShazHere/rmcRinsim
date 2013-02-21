/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import org.joda.time.DateTime;

import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;

/**
 * @author Shaza
 *
 */
public class FeaAnt extends Ant {
	private final DateTime interestedAt; //for storing the time at which order wants Trucks to propose it.
	
	public FeaAnt(CommunicationUser sender, DateTime interestedTime) {
		super(sender);
		this.interestedAt = interestedTime;
	}

	public DateTime getInterestedAt() {
		return interestedAt;
	}


	@Override
	public FeaAnt clone(CommunicationUser currentSender)  {
		
		try {
			return (FeaAnt)super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return null;
		}
	}
}
