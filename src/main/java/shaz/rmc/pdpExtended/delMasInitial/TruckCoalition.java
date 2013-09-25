/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.joda.time.DateTime;

import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckCostForDelivery;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 * @date 22/08/2013
 * 
 * For maintaining members of a coalition. This coalition could have been formed using any way, but its members will be so far only truck agents.
 * Later if have different requirements of coalitions, then I may add abstract/parent class in core and then derive classes form them.
 * 
 */
public class TruckCoalition {

	//TODO: should it be id's or full agents?
	ArrayList<DeliveryTruckInitial> members;
	
	/**
	 * 
	 */
	public TruckCoalition() {
		members = new ArrayList<DeliveryTruckInitial>();
	}
	
	protected void addMember(DeliveryTruckInitial truck) {
		members.add(truck);
	}
	protected void removeMember(DeliveryTruckInitial truck) {
		do 
			members.remove(truck);
		while (members.contains(truck));
	}
	
	public void addMemebers(ArrayList<DeliveryTruckInitial> truckList) {
		members = truckList;
	}
	
	public TruckCostForDelivery getMostSuitableTruck(Delivery del, TimeSlot timeWindow, DateTime currTime) {
		ArrayList<TruckCostForDelivery> truckCosts = new ArrayList<TruckCostForDelivery>();
		for (DeliveryTruckInitial memberTruck : members){
			TruckCostForDelivery tcfd = memberTruck.getCostForMakingDelivery(del, timeWindow, currTime); 
			if (tcfd != null)
				truckCosts.add(tcfd);
		}
		Collections.sort(truckCosts, new Comparator<TruckCostForDelivery>(){
	        public int compare( TruckCostForDelivery a, TruckCostForDelivery b ){ //sort in ascending order based on schedule score
	            return a.getDeliveryCost() - b.getDeliveryCost();
	        }
		});
		if (truckCosts.isEmpty())
			return null;
		return truckCosts.get(0); 
	}

}
