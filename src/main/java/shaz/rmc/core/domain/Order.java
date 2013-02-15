package shaz.rmc.core.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class Order implements Serializable {

    private final String id;
    private final ConstructionYard constructionYard;
    private final Set<VehicleType> prohibitedVehicleTypes;
    
    private final Integer pumpLineLengthRequired;
    
    private final int requiredTotalVolume;  //millimeter3
    private Integer requiredDischargePerHour;  //millimeter3 / hour
    
    private final DateTime earliestStartTime; //as given by customer
    private DateTime StartTime; //as possible by the simulator --- by shaz, 29/05/2012
    
   

	private final boolean maximumVolumeAllowed;
    private final boolean pickup;
    
    private final String preferredStationCode;
    
    private final Duration waitingMinutes;
    
    public Order(
            final String id,
            final ConstructionYard constructionYard,
            final int pumpLineLengthRequired,
            final int requiredTotalVolume,
            Integer requiredDischargePerHour,
            final DateTime earliestStartTime,
            final boolean maximumVolumeAllowed,
            final boolean pickup,
            final String preferredStationCode,
            final Duration waitingMinutes) {
        
        this.id = id;
        this.constructionYard = constructionYard;
        this.earliestStartTime = earliestStartTime;
        this.pumpLineLengthRequired = pumpLineLengthRequired;
        this.requiredTotalVolume = requiredTotalVolume;
        this.requiredDischargePerHour = requiredDischargePerHour;
        this.maximumVolumeAllowed = maximumVolumeAllowed;
        this.pickup = pickup;
        this.prohibitedVehicleTypes = new HashSet<VehicleType>(0);
        this.preferredStationCode = preferredStationCode;
        this.waitingMinutes = waitingMinutes;
        this.StartTime = this.earliestStartTime;
    }
    
    public String getId() {
        return id;
    }
    
    public ConstructionYard getConstructionYard() {
		return constructionYard;
	}
    
    public DateTime getEarliestStartTime() {
        return earliestStartTime;
    }
    
    public int getRequiredTotalVolume() {
        return requiredTotalVolume;
    }
    
    public boolean isMaximumVolumeAllowed() {
        return maximumVolumeAllowed;
    }
    
    public boolean isPickup() {
        return pickup;
    }
    
    public int getRequiredDischargePerHour() {
        return requiredDischargePerHour;
    }
    public int getRequiredDischargePerMinute(){
    	return (int)Math.ceil((double)requiredDischargePerHour/60d);
    }
    
    public boolean hasRequiredDischargeRate() {
        return requiredDischargePerHour != null;
    }
    
    public Set<VehicleType> getProhibitedVehicleTypes() {
        return prohibitedVehicleTypes;
    }

	public String getPreferredStationCode() {
		return preferredStationCode;
	}

	public Integer getPumpLineLengthRequired() {
		return pumpLineLengthRequired;
	}
	
	public boolean hasRequiredPumpLineLength() {
		return pumpLineLengthRequired != null;
	}
	
	public Duration getWaitingMinutes() {
		return waitingMinutes;
	}
	
	//by shaz
	 public DateTime getStartTime() {
			return StartTime;
		}

		public void setStartTime(DateTime startTime) {
			StartTime = startTime;
		}
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			
			sb.append("Order[");
			sb.append("\n  order=").append(this.getId());
			sb.append("\n  Earliest StartTime=").append(this.earliestStartTime);
			sb.append("\n  Required Volume=").append(this.requiredTotalVolume);
			sb.append("\n  Discharge Rate=").append(this.requiredDischargePerHour);
			sb.append("\n  Required discharge Rate per minute=" ).append (this.getRequiredDischargePerMinute());
			
			sb.append("]");
			
			return sb.toString();
		}
}
