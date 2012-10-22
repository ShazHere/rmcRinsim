package shaz.rmc.pdpExtended;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;

public class RmcProductionSite extends Depot {

	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	protected final Point Location;
	protected final RandomGenerator rnd;
	
	public RmcProductionSite() {
		rnd = new MersenneTwister();
		Location = roadModel.getRandomPosition(rnd);
	}
	
	public RmcProductionSite( Point p){
		rnd = new MersenneTwister();
		this.Location = p;
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
		roadModel.addObjectAt(this,Location); 
	}

}
