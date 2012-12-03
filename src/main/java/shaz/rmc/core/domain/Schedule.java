package shaz.rmc.core.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class Schedule implements Serializable {

	private Problem problem;
	private Map<Order, PriorityQueue<Delivery>> deliveryMap;
	private Double score;
	
	public Schedule(Problem problem) {
		this.problem = problem;
		this.deliveryMap = new HashMap<Order, PriorityQueue<Delivery>>();
		
		for(Order o : problem.getOrders()) {
			deliveryMap.put(o, new PriorityQueue<Delivery>(1,new Comparator<Delivery>() {

				@Override
				public int compare(Delivery o1, Delivery o2) {
					return o1.getUnloadingTime().compareTo(o2.getUnloadingTime());
				}
			}));
		}
	}
	
	public void addDelivery(Delivery delivery) {
		deliveryMap.get(delivery.getOrder()).add(delivery);
	}
	
	public Map<Order, PriorityQueue<Delivery>> getDeliveryMap() {
		return deliveryMap;
	}
	
	public void setScore(Double score) {
		this.score = score;
	}
	
	public Double getScore() {
		return score;
	}
	
	public static class Delivery {
		private Order order;
		private Vehicle vehicle;
		private BigDecimal deliveredVolume;
		private DateTime loadingTime;
		private DateTime unloadingTime;
		private Duration loadingDuration;
		private Duration unloadingDuration;
		private Station loadingStation;
		private Station returnStation;
		
		public Order getOrder() {
			return order;
		}
		public void setOrder(Order order) {
			this.order = order;
		}
		public Vehicle getVehicle() {
			return vehicle;
		}
		public void setVehicle(Vehicle vehicle) {
			this.vehicle = vehicle;
		}
		public BigDecimal getDeliveredVolume() {
			return deliveredVolume;
		}
		public void setDeliveredVolume(BigDecimal deliveredVolume) {
			this.deliveredVolume = deliveredVolume;
		}
		public DateTime getLoadingTime() {
			return loadingTime;
		}
		public void setLoadingTime(DateTime loadingTime) {
			this.loadingTime = loadingTime;
		}
		public DateTime getUnloadingTime() {
			return unloadingTime;
		}
		public void setUnloadingTime(DateTime unloadingTime) {
			this.unloadingTime = unloadingTime;
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
		public Station getLoadingStation() {
			return loadingStation;
		}
		public void setLoadingStation(Station loadingStation) {
			this.loadingStation = loadingStation;
		}
		public Station getReturnStation() {
			return returnStation;
		}
		public void setReturnStation(Station returnStation) {
			this.returnStation = returnStation;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			sb.append("Delivery[");
			sb.append("\n  order=").append(order.getId());
			sb.append("\n  vehicle=").append(vehicle.getId());
			sb.append("\n  loading time=").append(loadingTime);
			sb.append("\n  departs at station=").append(loadingTime.plus(loadingDuration));
			sb.append("\n  unloading time=").append(unloadingTime);
			sb.append("\n  departs at yard=").append(unloadingTime.plus(unloadingDuration));
			sb.append("]");
			
			return sb.toString();
		}
		
	}
	
}
