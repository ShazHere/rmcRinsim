package shaz.rmc.core.domain;

import java.io.Serializable;
import java.util.ArrayList;

import org.joda.time.DateTime;

public class Vehicle implements Serializable {
    
	private final String id;
    private final VehicleType vehicleType;
    private final int normalVolume, maximalVolume;
    private final int dischargeRate;
    private Integer pumpLineLength;
    
    
    //private DateTime currentAvailability;
    //private int currentStationIndex; 
    //private Station currentStation;
    
    //private int routeIndex;
    
	//private ArrayList<DateTime[]> availabilityList;
	
	//private ArrayList<String[]> stationIdList;
    
    public Vehicle(
    		final String id, 
    		final VehicleType vehicleType,
    		final int normalVolume,
    		final int maximalVolume,
    		final int dischargeRate) {
        this.id = id;
        this.vehicleType = vehicleType;
        this.normalVolume = normalVolume;
        this.maximalVolume = maximalVolume;
        this.dischargeRate = dischargeRate;
        
		//availabilityList = new ArrayList<DateTime[]>();
		//stationIdList = new ArrayList<String[]>();
    }
    
    public Vehicle(
    		final String id, 
    		final VehicleType vehicleType,
    		final int normalVolume,
    		final int maximalVolume,
    		final int dischargeRate,
    		Integer pumpLineLength) {
        this(id,vehicleType,normalVolume,maximalVolume,dischargeRate);
        
        this.pumpLineLength = pumpLineLength;
    }
    
    public String getId() {
        return id;
    }
    
    public VehicleType getVehicleType() {
        return vehicleType;
    }
    
    public int getNormalVolume() {
		return normalVolume;
	}
    
    public int getMaximalVolume() {
		return maximalVolume;
	}
    
    public int getDischargeRate() {
		return dischargeRate;
	}
    
    public Integer getPumpLineLength() {
		return pumpLineLength;
	}
    
    public boolean hasPumpLineLength() {
    	return pumpLineLength != null;
    }
    
//    public DateTime getCurrentAvailability() {
//		return currentAvailability;
//	}
//
//	public void setCurrentAvailability(DateTime currentAvailability) {
//		this.currentAvailability = currentAvailability;
//	}

//	public Station getCurrentStation() {
//		return currentStation;
//	}
//
//	public void setCurrentStation(Station currentStation) {
//		this.currentStation = currentStation;
//	}
//
//	public int getCurrentStationIndex() {
//		return currentStationIndex;
//	}
//
//	public void setCurrentStationIndex(int currentStationIndex) {
//		this.currentStationIndex = currentStationIndex;
//	}
//	
//	public int getRouteIndex() {
//		return routeIndex;
//	}
//
//	public void setRouteIndex(int routeIndex) {
//		this.routeIndex = routeIndex;
//	}

//	public ArrayList<DateTime[]> getAvailabilityList() {
//		return availabilityList;
//	}
//	
//	public ArrayList<String[]> getStationIdList() {
//		return stationIdList;
//	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(getClass().getSimpleName()).append("[");
        sb.append("id=").append(id).append(",");
        sb.append("\n vehicleType=").append(vehicleType);
        sb.append("\n Normal volume=").append(this.normalVolume);
        sb.append("\n Maximum volume=").append(this.maximalVolume);
        sb.append("]");
        
        return sb.toString();
    }
}
