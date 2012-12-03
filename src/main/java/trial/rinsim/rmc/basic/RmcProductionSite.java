package trial.rinsim.rmc.basic;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

public class RmcProductionSite implements RoadUser {
	Point Location;
	
	protected RoadModel roadModel;
	protected final RandomGenerator rnd;
	
	RmcProductionSite (RandomGenerator rnd){
		this.rnd = rnd;
	}
	RmcProductionSite (Point p){
		this.Location = p;
		rnd = new MersenneTwister();
	}
	
	@Override
	public void initRoadUser(RoadModel model) {

		roadModel = model;
		//Location =  roadModel.getRandomPosition(rnd);
		roadModel.addObjectAt(this,Location);
	}

}
