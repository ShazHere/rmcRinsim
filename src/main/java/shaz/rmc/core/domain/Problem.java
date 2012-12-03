package shaz.rmc.core.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.joda.time.Duration;

public class Problem implements Serializable {

	private Map<Location, Map<Location, Duration>> distanceMatrix;
    private ArrayList<Order> orders;
    private ArrayList<Vehicle> vehicles;
    private ArrayList<Station> stations;
    //private ArrayList<Station> stations;
    private Set<ConstructionYard> yards;
    private Map<String,VehicleType> vehicleTypes;
    
    public Problem() {
        distanceMatrix = new HashMap<Location, Map<Location,Duration>>();
        orders = new ArrayList<Order>();
        vehicles = new ArrayList<Vehicle>();
        stations = new ArrayList<Station>();
        yards = new HashSet<ConstructionYard>();
        vehicleTypes = new HashMap<String, VehicleType>();
    }
    
    public void addVehicleType(VehicleType vt) {
    	vehicleTypes.put(vt.getName(),vt);
    }
    
    public Set<VehicleType> getVehicleTypes() {
    	return new HashSet<VehicleType>(vehicleTypes.values());
    }
    
    public VehicleType getVehicleType(String name) {
    	return vehicleTypes.get(name);
    }
    
    public void addDuration(Location from, Location to, Duration duration) {

    	Map<Location, Duration> map = distanceMatrix.get(from);
    	if(map == null) {
    		map = new HashMap<Location, Duration>();
    		distanceMatrix.put(from, map);
    	}
    	
    	map.put(to, duration);
    }
    
    public Duration getDuration(Location from, Location to) {
    	return distanceMatrix.get(from).get(to);
    }
    
    public ArrayList<Order> getOrders() {
        return orders;
    }
    
    public void addOrder(Order order) {
        this.orders.add(order);
        yards.add(order.getConstructionYard());
    } 
    
//    public void addConstructionYard(ConstructionYard yard) {
//    	this.yards.put(yard.getId(), yard);
//    }
    public Set<ConstructionYard> getConstructionYards() {
		return yards;
	}
    
//    public ConstructionYard getConstructionYard(String id) {
//    	return this.yards.get(id);
//    }
    
    public void addVehicle(Vehicle vehicle) {
    	vehicles.add(vehicle);
    }
    
    public ArrayList<Vehicle> getVehicles() {
        return vehicles;
    }

	public ArrayList<Station> getStations() {
		return stations;
	}

	public void setStations(ArrayList<Station> stations) {
		this.stations = stations;
	}
	
	public void addStation(Station station){
		stations.add(station);
	}
	
	public Station getStation(String stationCode){
		
		for(int i = 0; i < stations.size(); i++){
			if(stations.get(i).getId().equals(stationCode)){
				return stations.get(i);
			}
		}
		
		return null;
	}
   
}
