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
	private final Double fixedCapacityAmount; //i.e order is looking for specific amount of capacity
	
	public FeaAnt(CommunicationUser sender, DateTime interestedTime, double fixedCapacityAmount) {
		super(sender);
		this.interestedAt = interestedTime;
		this.fixedCapacityAmount = fixedCapacityAmount;
	}

	public DateTime getInterestedAt() {
		return interestedAt;
	}

	public Double getFixedCapacityAmount() {
		return fixedCapacityAmount;
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
