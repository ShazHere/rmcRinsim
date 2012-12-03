/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial.communication;

import org.joda.time.DateTime;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationUser;
import shaz.rmc.core.Ant;

/**
 * @author Shaza
 *
 */
public class FeaAnt extends Ant {
	private final DateTime interestedAt; //for storing the time at which order wants Trucks to propose it.
	private Double travelDistance; //chk if required or shud it b directley added in pheromone
	
	
	public FeaAnt(CommunicationUser sender, DateTime interestedTime) {
		super(sender);
		this.interestedAt = interestedTime;
	}
	public void setTravelDistance (double pTravelDistance) { 
		this.travelDistance = pTravelDistance;
	}
	public DateTime getInterestedAt() {
		return interestedAt;
	}
	public Double getTravelDistance() {
		return travelDistance;
	}
	public void setTravelDistance(Double travelDistance) {
		this.travelDistance = travelDistance;
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
