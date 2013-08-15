package shaz.rmc.core.domain;

//import java.io.Serializable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.domain.DeliveryS.Builder;

/**
 * @author Shaza
 * Immutable class for saving details of a delivery object. 
 * made major changes at 11/07/2013
 */

final public class Delivery { 
	final private ProductionSite loadingStation;
	//final private DateTime loadingTime, unloadingTime; //Start time
	final private Duration loadingDuration, unloadingDuration;
	final private Duration stationToCYTravelTime;
	final private  Agent order;
	final private int deliveredVolume;
	final private int deliveryNo; //which delivery of order it could be?
	
	//added by shaz
	final private DateTime deliveryTime;  //according to d1 which might be settled
	private final Agent truck;
	final private int wastedVolume; // filed by truck while adding unit

	private Delivery(Builder builder) {
		this.order = builder.order;
		this.truck = builder.truck;
		this.deliveredVolume = builder.deliveredVolume;
		this.loadingStation = builder.loadingStation;
		//this.returnStation = builder.returnStation;
		this.deliveryTime = builder.deliveryTime;
		this.deliveryNo = builder.deliveryNo;
		this.loadingDuration = builder.loadingDuration;
		this.unloadingDuration = builder.unloadingDuration;
		this.stationToCYTravelTime = builder.stationToCYTravelTime;
//		this.CYToStationTravelTime = builder.CYToStationTravelTime;
//		this.lagTime = builder.lagTime;
		this.wastedVolume =  builder.wastedVolume;
	}
	
	public static class Builder {
		private ProductionSite loadingStation;
		private Duration loadingDuration, unloadingDuration;
		private Duration stationToCYTravelTime;
		private Agent order;
		private int deliveredVolume;
		private int deliveryNo; //which delivery of order it could be?
		private DateTime deliveryTime;  //according to d1 which might be settled
		private Agent truck;
		private int wastedVolume; // filed by truck while adding unit
		
		public Builder() {}

		public Builder setLoadingStation(ProductionSite loadingStation) {
			this.loadingStation = loadingStation;
			return this;
		}

		public Builder setLoadingDuration(Duration loadingDuration) {
			this.loadingDuration = loadingDuration;
			return this;
		}

		public Builder setUnloadingDuration(Duration unloadingDuration) {
			this.unloadingDuration = unloadingDuration;
			return this;
		}

		public Builder setStationToCYTravelTime(Duration stationToCYTravelTime) {
			this.stationToCYTravelTime = stationToCYTravelTime;
			return this;
		}

		public Builder setOrder(Agent order) {
			this.order = order;
			return this;
		}

		public Builder setDeliveredVolume(int deliveredVolume) {
			this.deliveredVolume = deliveredVolume;
			return this;
		}

		public Builder setDeliveryNo(int deliveryNo) {
			this.deliveryNo = deliveryNo;
			return this;
		}

		public Builder setDeliveryTime(DateTime deliveryTime) {
			this.deliveryTime = deliveryTime;
			return this;
		}

		public Builder setTruck(Agent truck) {
			this.truck = truck;
			return this;
		}
		
		public Delivery build(){
			return new Delivery (this);
		}
		public Builder setWastedVolume(int wastedVolume) {
			this.wastedVolume = wastedVolume;
			return this;
		}
	}

	public Agent getOrder() {
		return order;
	}
	
	public int getDeliveredVolume() {
		return deliveredVolume;
	}
	public int getWastedVolume() {
		return wastedVolume;
	}

	public ProductionSite getLoadingStation() {
		return loadingStation;
	}

	public Duration getLoadingDuration() {
		return loadingDuration;
	}

	public Duration getUnloadingDuration() {
		return unloadingDuration;
	}

	public Duration getStationToCYTravelTime() {
		return stationToCYTravelTime;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("Delivery[");
		//sb.append("\n  order=").append(order.getId());
		//sb.append("\n  truck=").append(truck.getId());
		sb.append("Delivery no =").append(deliveryNo);
		sb.append("\n  loading time1=").append(deliveryTime.minus(loadingDuration).minus(stationToCYTravelTime));
		sb.append("\n  from Station=" + loadingStation);
		sb.append("\n  departs at station=").append(deliveryTime.minus(stationToCYTravelTime));
		sb.append("\n  unloading time=").append(deliveryTime);
		sb.append("\n  leaves CY at =").append(deliveryTime.plus(unloadingDuration));
		sb.append("]");
		
		return sb.toString();
	}
	
	public DateTime getLoadingTime() {
		return deliveryTime.minus(loadingDuration).minus(stationToCYTravelTime);
	}
	public DateTime getDeliveryTime() {
		return deliveryTime;
	}
	//not sure if its required.. 13/12/2011
	public Duration requiredTimetoDeliver() {		
		return null;
	}

	public Agent getTruck() {
		return truck;
	}

	public int getDeliveryNo() {
		return deliveryNo;
	}
	

}
