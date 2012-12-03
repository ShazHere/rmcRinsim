package shaz.rmc.core.domain;

import java.io.Serializable;
import java.util.ArrayList;

public class OrderDelivery implements Serializable {

	private Order order;
	private ArrayList<Delivery> deliveryList;
	
	public OrderDelivery(Order order){
		this.order = order;
		deliveryList = new ArrayList<Delivery>();
	}
	
	public OrderDelivery(Order order, ArrayList<Delivery> deliveryList) {
		super();
		this.order = order;
		this.deliveryList = deliveryList;
	}


	public Order getOrder() {
		return order;
	}
	public void setOrder(Order order) {
		this.order = order;
	}
	public ArrayList<Delivery> getDeliveryList() {
		return deliveryList;
	}
	public void setDeliveryList(ArrayList<Delivery> deliveryList) {
		this.deliveryList = deliveryList;
	}
}
