/**
 * 
 */
package shaz.rmc.core;

/**
 * @author Shaza
 * @date 22/08/2012
 */
public class TruckCostForDelivery {
	private int deliveryCost;
	private int travelCost;
	TruckDeliveryUnit tdu;
	//Agent truckAgent;
	//AvailableSlot availableSlot;


	public TruckCostForDelivery(int deliveryCost, int travelCost, TruckDeliveryUnit tdu) {
		super();
		this.deliveryCost = deliveryCost;
		this.travelCost = travelCost;
		this.tdu = tdu;
	}

	public int getDeliveryCost() {
		return deliveryCost;
	}


	public int getTravelCost() {
		return travelCost;
	}

	public Agent getTruckAgent() {
		return tdu.getTruck();
	}

	public TruckDeliveryUnit getTruckDeliveryUnit() {
		return tdu;
	}

	//TODO: add compareTo for sorting..

}
