/**
 * 
 */
package shaz.rmc.pdpExtended;

import static com.google.common.collect.Maps.newLinkedHashMap;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

/**
 * This is a virtual agent, it doesn't on physical model. It is responsible for a single customer order. 
 * At suitable time, it should create {@link RmcDelivery}s objects according to totals quantity of concrete 
 * ordered by Order, available vehicle types (i.e types w.r.t capacities), proposals of 
 * {@link RmcDeliveryTruck}s and startTime of Order. 
 * 
 * In its initial phase (@date 17/10/2012), it creates the {@link RmcDelivery}s objects in the beginning, when 
 * based on just truck types, and total quantity of concrete.
 * 
 * Conceptually we an assume that it resided at the location of
 * construction Yard (in case we need a gradientField kind of communication mode). 
 * 
 *  TODO add comments w.r.t communication model
 * 
 * @author Shaza
 * @date 17/10/2012
 */
public class RmcOrder extends Depot implements TickListener  {
	
	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	protected List<RmcDelivery> deliveries; 
	protected RmcProductionSite site;
	private Point Location;
	final Simulator sim; 
	//TODO here there should be object of domain.order 
	RmcOrder (Simulator pSim) {
		deliveries = new ArrayList<RmcDelivery>(); 
		sim = pSim;
	}

	public void tick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		if (deliveries.size() == 0) {
			
			Set<RmcProductionSite> sites = roadModel.getObjectsOfType(RmcProductionSite.class);
			RmcProductionSite site = null, previousSite = null;
			if (sites.size() > 0) {
				for (RmcProductionSite pS : sites) {
					if (previousSite == null)
						previousSite = pS;
					if (previousSite != null && previousSite.equals(pS) == false)
						site = pS;
				}
			}
			RmcDelivery d1 = new RmcDelivery(this, 1, previousSite!=null?roadModel.getPosition(previousSite):null, Location, 5, 5, 10); // at the moment
			RmcDelivery d2 = new RmcDelivery(this, 2, site!=null?roadModel.getPosition(site):roadModel.getPosition(previousSite), Location, 5, 5, 10); // at the moment
			deliveries.add(d1);
			deliveries.add(d2);
			//register deliveries
			sim.register(d1);
			sim.register(d2);
			
			//TODO fix the problem that I cannot assign the delivery locations latter by using setPostion, since once created, 
			//they should be assingined location at the time of creation, other wise some checks donot pass!!
		}


	}

	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}
	public void setPSiteForDeliveries(RmcProductionSite pSite) {
		site = pSite;
				
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
		RandomGenerator rnd = new MersenneTwister();
		Location = roadModel.getRandomPosition(rnd);
		roadModel.addObjectAt(this,Location); 
		
	}
	

}
