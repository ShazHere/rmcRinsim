/**
 * 
 */
package shaz.rmc.core.domain;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.domain.Delivery.Builder;

/**
 * @author Shaza
 * @date 11/07/2013
 * Immutable class to represent delivery of an order
 *
 */
final public class DeliveryS {
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

	private DeliveryS(Builder builder) {
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
//		this.wastedVolume =  builder.wastedVolume;
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
		
		public DeliveryS build(){
			return new DeliveryS (this);
		}
		
	}

	public Agent getOrder() {
		return order;
	}
	
	public int getDeliveredVolume() {
		return deliveredVolume;
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
		sb.append("\n  order=").append(order.getId());
		//sb.append("\n  truck=").append(truck.getId());
		sb.append("\n  loading time1=").append(deliveryTime.minus(loadingDuration).minus(stationToCYTravelTime));
		sb.append("\n  from Station=" + loadingStation);
		sb.append("\n  departs at station=").append(deliveryTime.minus(stationToCYTravelTime));
		sb.append("\n  unloading time=").append(deliveryTime);
		sb.append("\n  leaves CY at =").append(deliveryTime.plus(unloadingDuration));
		sb.append("]");
		
		return sb.toString();
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
