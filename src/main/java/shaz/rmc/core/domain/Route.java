package shaz.rmc.core.domain;

import java.io.Serializable;
import java.util.ArrayList;

public class Route implements Serializable {
	
	private Vehicle vehicle;
	private ArrayList<Delivery> deliveries;

	//For getCopyOf
	public Route(){
	}
	
	public Route(Vehicle vehicle){
		this.vehicle = vehicle;
		deliveries = new ArrayList<Delivery>();
	}
	
	public void getCopyOf(Route route){
		vehicle = route.getVehicle();
		deliveries = route.getDeliveries();
	}

	
	public Vehicle getVehicle() {
		return vehicle;
	}

	public ArrayList<Delivery> getDeliveries() {
		return deliveries;
	}
}
