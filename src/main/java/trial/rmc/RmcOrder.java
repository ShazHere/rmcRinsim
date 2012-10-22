/**
 * 
 */
package trial.rmc;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

/**
 * @author Shaza
 *
 */
public class RmcOrder  implements TickListener, RoadUser {
	
	protected RoadModel roadModel;
	protected final RandomGenerator rnd;
	protected Point startLocation;
	private int id;
	public int getId() {
		return id;
	}

	static int totalNum;
	
	RmcOrder(Point p) {
		rnd = new MersenneTwister();
		startLocation = p;
		totalNum = totalNum+1;
		id = totalNum;
	}

	@Override
	public void tick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initRoadUser(RoadModel model) {
		roadModel = model;
		roadModel.addObjectAt(this, startLocation);
		
	}
}
