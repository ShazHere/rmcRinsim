/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;

import org.joda.time.Duration;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.CommitmentAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

//import static com.google.common.base.Preconditions.checkArgument;
/**
 * @author Shaza
 *
 */
public class DeliveryTruckInitialBelief {
	
	private Point startLocation;
	private ProductionSite startPS;
	private final TimeSlot totalTimeRange; //for storing the actual period of activity of truck. i.e when trucks start its day, and ends it
	//only start and endTime will be used
	protected final ArrayList<ExpAnt> explorationAnts;
	protected final ArrayList<IntAnt> intentionAnts;
	protected final ArrayList<CommitmentAnt> commitmentAnts;
	
	//for Objective function
	Duration totalTravelTime; //keeps record of the time vehicle kept on traveling
	private int wastedConcrete; // to keep record of wasted concrete 
	
	protected ArrayList<AvailableSlot> availableSlots; 
	
	public DeliveryTruckInitialBelief() {
		wastedConcrete = 0;
		totalTravelTime = new Duration(0); //to keep record of total travelling time, to be used by objective function latter.
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		commitmentAnts = new ArrayList<CommitmentAnt>();
		totalTimeRange = new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME);
		availableSlots = new ArrayList<AvailableSlot>();
		adjustAvailableSlotInBeginning();
	}
//	public ArrayList<TimeSlot> getAvailableSlots() {
//		return availableSlots;
//	}


	/**
	 * Just to avoid that AvailableSlot is 1 if truck schedule is empty.
	 */
	protected void adjustAvailableSlotInBeginning() {
		availableSlots.clear();
		availableSlots.add(new AvailableSlot(new TimeSlot(GlobalParameters.START_DATETIME, GlobalParameters.END_DATETIME), null, null));
	}
	

	public int getWastedConcrete() {
		return wastedConcrete;
	}
	public void setWastedConcrete(int wastedConcrete) {
		this.wastedConcrete = wastedConcrete;
	}
	
	public boolean addWastedConcrete(int amount){
		this.wastedConcrete += amount;
		return true;
	}
	/** @description Returns total time traveled by this truck
	 * 
	 */
	public Duration getTotalTravelTime() {
		return totalTravelTime;
	}

	public Point getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Point startLocation) {
		this.startLocation = startLocation;
	}
	public ProductionSite getStartPS() {
		return startPS;
	}


	public void setStartPS(ProductionSite startPS) {
		this.startPS = startPS;
	}


	public TimeSlot getTotalTimeRange() {
		return totalTimeRange;
	}
	
}

