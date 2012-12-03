package shaz.rmc.pdpExtended.trial;

import java.util.Collection;
import java.util.Set;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;

public class RmcDeliveryTruck extends Vehicle {

	protected RoadModel roadModel;
	protected PDPModel pdpModel;

	protected final Point startLocation; //it wont be saved any where else 
	protected Point destination; 
	
	private Parcel currParcel;
	
	

	public RmcDeliveryTruck(Point startPosition, double capacity) {
		setStartPosition(startPosition);
		startLocation = startPosition;
		setCapacity(capacity);
		destination = null;
	}

	@Override
	public double getSpeed() {
		return .03;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}

	@Override
	protected void tickImpl(TimeLapse time) {
		final Collection<Parcel> parcels = pdpModel.getAvailableParcels();
		if (destination == null && roadModel.getPosition(this).equals(startLocation)) {
			if (pdpModel.getContents(this).isEmpty()) { //if the truck is empty..
				if (!parcels.isEmpty() && destination == null) {
					double dist = Double.POSITIVE_INFINITY;
					for (final Parcel p : parcels) { //get the one with smallest distance
						final double d = Point.distance(roadModel.getPosition(this), roadModel.getPosition(p));
						if (d <= dist) {
							dist = d;
							destination = roadModel.getPosition(p);
							currParcel = p;
						}
					}
				}
				if (destination != null) {// && roadModel.containsObject(currParcel)) {
					roadModel.moveTo(this, destination, time);
				} 
			} 
		    //destination = roadModel.getPosition(currParcel);
		}
		else if  (roadModel.getPosition(this).equals(destination)) {
			if (roadModel.containsObject(currParcel)) {
				pdpModel.pickup(this, currParcel, time);
				destination = currParcel.getDestination();
			}
			else if (destination.equals(startLocation))
				destination = null;
			else
				destination = startLocation;
		}
		
		else if (destination != null)
			roadModel.moveTo(this, destination, time);
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;
	}

	
}
