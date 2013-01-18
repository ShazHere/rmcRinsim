/**
 * 
 */
package shaz.rmc.core;

/**
 * The statistic attributes related to Order
 * @author Shaza
 *
 */
public class ResultElementsOrder {
	

	int totalDeliveriesReadyByOrder =0; //so orders made these deliveries ready
	int totalConcreteByOrder = 0; //total amount of concrete orderd byt the customer
	int deliveredConcrete = 0; //totoal amount of concrete delivered to customer
	
	public ResultElementsOrder () {
		totalDeliveriesReadyByOrder =0;
		totalConcreteByOrder = 0;
		deliveredConcrete = 0;
	}
	
	public ResultElementsOrder(int totalDeliveriesReadyByOrder,
			int totalConcreteByOrder, int deliveredConcrete) {
		super();
		this.totalDeliveriesReadyByOrder = totalDeliveriesReadyByOrder;
		this.totalConcreteByOrder = totalConcreteByOrder;
		this.deliveredConcrete = deliveredConcrete;
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
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TotalDeliveriesByOrder: ").append(totalDeliveriesReadyByOrder).append("\n");
		sb.append("TotalConcreteByOrder: ").append(totalConcreteByOrder).append("\n");
		sb.append("DeliveredConcrete: ").append(deliveredConcrete).append("\n");
		return sb.toString();
	}
	
}
