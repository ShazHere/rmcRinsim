/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.domain.Station;

/**
 * @author Shaza
 *
 */
public class RmcProductionSite extends Depot implements ProductionSite {

	private RoadModel roadModel;
	private PDPModel pdpModel;
	private Point Location;
	
	protected final RandomGenerator rnd;
	private final Station station;
	
	
	public RmcProductionSite(Station pStation) {
		rnd = new MersenneTwister();
		station = pStation;
	}
	
	public RmcProductionSite( Point p, Station pStation){
		rnd = new MersenneTwister();
		this.Location = p;
		station = pStation;
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
		Location = roadModel.getRandomPosition(rnd);
		roadModel.addObjectAt(this,Location); 
	}
	public Station getStation() {
		return station;
	}
	
	public Point getLocation() {
		return Location;
	}
//	public void setStation(Station station) {
//		this.station = station;
//	}


}
