package shaz.rmc.pdpExtended.masDisco;

import rinde.sim.core.Simulator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;

/**
 * Depicts the physical aspect of the order, Location of order. 
 * 
 * @author Shaza
 *
 */

//At some latter stage, i can mergen it into the OrderAgent class since orderAgent can also extend
// from Depot class of PDP model. 
public class OrderSite extends Depot {
	
	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	private final Simulator sim;
	private final Point Location;
	private final RmcOrderAgent order;
	public OrderSite(Simulator pSim, Point pLocation, RmcOrderAgent pOrder) {
		Location = pLocation;
		super.setStartPosition(Location);
		sim = pSim;	
		order = pOrder;
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
	}
	
	public boolean register() {
		assert sim == null : "Simulator not set yet";
		sim.register(this);
		return true;
	}
	public Point getPosition() {
		return pdpModel.getPosition(this);
	}
	public Point getLocation() {
		return Location;
	}
	public RmcOrderAgent getOrder() {
		return order;
	}

}
