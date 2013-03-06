/**
 * 
 */
package shaz.rmc.core;

import java.util.ArrayList;

import shaz.rmc.core.domain.Delivery;
/**
 * The statistic attributes related to Order
 * @author Shaza
 *
 */
public class ResultElementsOrder {
	
	//private final ArrayList<Delivery> deliveries;
	private int totalDeliveriesReadyByOrder =0; //so orders made these deliveries ready
	private int totalConcreteByOrder = 0; //total amount of concrete orderd byt the customer
	private int deliveredConcrete = 0; //totoal amount of concrete delivered to customer
	private int undeliveredConcrete = 0; //amount of concrete, not furnished by any of trucks
	
	public ResultElementsOrder () {
		//deliveries = null;
		totalDeliveriesReadyByOrder =0;
		totalConcreteByOrder = 0;
		deliveredConcrete = 0;
		undeliveredConcrete = 0;
	}
	
	public ResultElementsOrder(ArrayList<Delivery> deliveriesReadyByOrder,
			int totalConcreteByOrder, int deliveredConcrete) {
		super();
		//this.deliveries = deliveriesReadyByOrder;
		
		this.totalConcreteByOrder = totalConcreteByOrder;
		this.deliveredConcrete = deliveredConcrete;
		int actualServedConcrete = 0;
		if (deliveriesReadyByOrder != null) {
			for (Delivery d : deliveriesReadyByOrder) {
				actualServedConcrete += d.getDeliveredVolume() - d.getWastedVolume();
			}
			this.totalDeliveriesReadyByOrder = deliveriesReadyByOrder.size();
		} //else default values are already ok.
			
		this.undeliveredConcrete = this.totalConcreteByOrder - actualServedConcrete;
	}

	public int getTotalDeliveriesReadyByOrder() {
		return totalDeliveriesReadyByOrder;
	}

	public void addTotalDeliveriesReadyByOrder(int totalDeliveriesReadyByOrder) {
		this.totalDeliveriesReadyByOrder += totalDeliveriesReadyByOrder;
	}

	public int getTotalConcreteByOrder() {
		return totalConcreteByOrder;
	}

	public void addTotalConcreteByOrder(int totalConcreteByOrder) {
		this.totalConcreteByOrder += totalConcreteByOrder;
	}

	public int getDeliveredConcrete() {
		return deliveredConcrete;
	}

	public void addDeliveredConcrete(int deliveredConcrete) {
		this.deliveredConcrete += deliveredConcrete;
	}
	public int getUndeliveredConcrete() {
		return undeliveredConcrete;
	}
	public void addUndeliveredConcrete(int undeliveredConcrete) {
		this.undeliveredConcrete += undeliveredConcrete;
	}
	
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TotalDeliveriesByOrder: ").append(totalDeliveriesReadyByOrder).append("\n");
		sb.append("TotalConcreteByOrder: ").append(totalConcreteByOrder).append("\n");
		sb.append("DeliveredConcrete: ").append(deliveredConcrete).append("\n");
		sb.append("UndeliveredConcrete: ").append(undeliveredConcrete).append("\n");
		return sb.toString();
	}
	
}
