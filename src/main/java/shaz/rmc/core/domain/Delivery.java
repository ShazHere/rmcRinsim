package shaz.rmc.core.domain;

import java.io.Serializable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;

public class Delivery implements Serializable {

	private ProductionSite loadingStation;
	private ProductionSite returnStation;
	//private DateTime loadingTime, unloadingTime; //Start time
	private Duration loadingDuration, unloadingDuration;
	private Duration stationToCYTravelTime, CYToStationTravelTime;
	//private Vehicle vehicle;
	private Agent order;
	private int deliveredVolume;
	private Duration lagTime;// for storing lagTime before this delivery
	
	
	//added by shaz
	private boolean isReserved;
	private DateTime deliveryTime;  //according to d1 which might be settled
	//private boolean isLastDelivery; //to track that is this the last delivery of order, Truck may give different priority based on this fact..may be used in waste calculation
	private int wastedVolume; // filed by truck while adding unit

	
	//private boolean isExplored; //means is sent as reply to an exploration ant of a truck. not used now.. @shaz 15/03/2012
	private boolean confirmed; //to record that the truck has confirmed and sent intention ant, that it will pick the delivery at delivery
	private Agent truck;
	//private Agent orderAg;




	public Delivery(Agent orderAg, Vehicle vehicle, int deliveredVolume,
			ProductionSite loadingStation, ProductionSite returnStation) {
		this.order = orderAg;
		//this.vehicle = vehicle;
		this.deliveredVolume = deliveredVolume;
		this.loadingStation = loadingStation;
		this.returnStation = returnStation;
		this.isReserved = false;
		this.deliveryTime = null;
		this.confirmed = false;
		//this.orderAg = orderAg;
//		this.isLastDelivery = false;
		//this.isExplored = false;
	}
	
	public void getCopyOf(Delivery delivery){
		order = delivery.getOrder();
		//vehicle = delivery.getVehicle();
		deliveredVolume = delivery.getDeliveredVolume();
		loadingStation = delivery.getLoadingStation();
		returnStation = delivery.getReturnStation();
	}
	
	public Agent getOrder() {
		return order;
	}
	
//	public Vehicle getVehicle() {
//		return vehicle;
//	}
	
	public int getDeliveredVolume() {
		return deliveredVolume;
	}

//	public DateTime getLoadingTime() {
//		return loadingTime;
//	}
//
//	public void setLoadingTime(DateTime loadingTime) {
//		this.loadingTime = loadingTime;
//	}
//
//	public DateTime getUnloadingTime() {
//		return unloadingTime;
//	}
//
//	public void setUnloadingTime(DateTime unloadingTime) {
//		this.unloadingTime = unloadingTime;
//	}

	public ProductionSite getLoadingStation() {
		return loadingStation;
	}

	public ProductionSite getReturnStation() {
		return returnStation;
	}

	public Duration getLoadingDuration() {
		return loadingDuration;
	}

	public void setLoadingDuration(Duration loadingDuration) {
		this.loadingDuration = loadingDuration;
	}

	public Duration getUnloadingDuration() {
		return unloadingDuration;
	}

	public void setUnloadingDuration(Duration unloadingDuration) {
		this.unloadingDuration = unloadingDuration;
	}

	public Duration getStationToCYTravelTime() {
		return stationToCYTravelTime;
	}

	public void setStationToCYTravelTime(Duration stationToCYTravelTime) {
		this.stationToCYTravelTime = stationToCYTravelTime;
	}

	public Duration getCYToStationTravelTime() {
		return CYToStationTravelTime;
	}

	public void setCYToStationTravelTime(Duration cYToStationTravelTime) {
		CYToStationTravelTime = cYToStationTravelTime;
	}

	public void setLoadingStation(ProductionSite loadingStation) {
		this.loadingStation = loadingStation;
	}

	public void setReturnStation(ProductionSite returnStation) {
		this.returnStation = returnStation;
	}

//	public void setVehicle(Vehicle vehicle) {
//		this.vehicle = vehicle;
//	}

	public void setDeliveredVolume(int deliveredVolume) {
		this.deliveredVolume = deliveredVolume;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		sb.append("\n  order=").append(order.getId());
		//sb.append("\n  truck=").append(truck.getId());
		sb.append("\n  loading time=").append(deliveryTime.minus(loadingDuration).minus(stationToCYTravelTime));
		sb.append("\n  departs at station=").append(deliveryTime.minus(stationToCYTravelTime));
		sb.append("\n  unloading time=").append(deliveryTime);
		sb.append("\n leaves CY at =").append(deliveryTime.plus(unloadingDuration));
		sb.append("\n reaches at Station=").append(deliveryTime.plus(unloadingDuration).plus(CYToStationTravelTime));
		sb.append("]");
		
		return sb.toString();
	}
	
//	public boolean isExplored() {
//		return isExplored;
//	}
//	public void setExplored (boolean val) {
//		this.isExplored = val;
//	}
	public synchronized void setReserved(boolean isRes) {
		this.isReserved = isRes;
	}
	public synchronized boolean isReserved() {
		return this.isReserved;
	}
	
	public DateTime getDeliveryTime() {
		return deliveryTime;
	}
	public void setDeliveryTime(DateTime deliveryTime) {
		this.deliveryTime = deliveryTime;
	}
	
	//not sure if its required.. 13/12/2011
	public Duration requiredTimetoDeliver() {
		
		return null;
	}
	public boolean isConfirmed() {
		return confirmed;
	}

	public void setConfirmed(boolean confirmed) {
		this.confirmed = confirmed;
	}

	public Agent getTruck() {
		return truck;
	}

	public void setTruck(Agent truck) {
		this.truck = truck;
	}
//	public Agent getOrderAg() {
//		return orderAg;
//	}
	public int getWastedVolume() {
		return wastedVolume;
	}

	public void setWastedVolume(int wastedVolume) {
		this.wastedVolume = wastedVolume;
	}
//	public boolean isLastDelivery() {
//		return isLastDelivery;
//	}
//
//	public void setLastDelivery(boolean isLastDelivery) {
//		this.isLastDelivery = isLastDelivery;
//	}

	public Duration getLagTime() {
		return lagTime;
	}

	public void setLagTime(Duration lagTime) {
		this.lagTime = lagTime;
	}
}
