package shaz.rmc.pdpExtended;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.graphics.RGB;

import rinde.sim.core.Simulator;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.GraphRoadModel;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

public class RmcTrialPDPExample {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final RandomGenerator rng = new MersenneTwister();
		final Simulator sim = new Simulator(rng, 1);
		//final RoadModel roadModel = new GraphRoadModel(graph);
		PlaneRoadModel roadModel = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), false, 1);
		final PDPModel pdpModel = new PDPModel();
		sim.register(roadModel);
		sim.register(pdpModel);
		sim.configure();		
		
		//Tracking location for production sites
		Point ps[] = new Point[2];
		for (int j = 0; j< 2; j++) {
			ps[j] = roadModel.getRandomPosition(rng);
			sim.register(new RmcProductionSite(ps[j]));
		}
			
		//for trucks
		int radius = 30000;
		final int numTrucks = 2;
		for (int i = 0; i < numTrucks; i++) {
			// when an object is registered in the simulator it gets
			// automatically 'hooked up' with models that it's interested in. An
			// object declares to be interested in an model by implementing an
			// interface.
			sim.register(new RmcDeliveryTruck(ps[i%2], radius));
		}		
		
		// for construction yards/orders
		final int numOrders = 1;
		for (int j = 0; j< numOrders; j++) {
			sim.register(new RmcOrder(sim));
		}
		

		final UiSchema schema = new UiSchema();
		schema.add(RmcDeliveryTruck.class, "/graphics/perspective/concreteTruck-32.png");
		schema.add(RmcProductionSite.class, "/graphics/perspective/productionSite-32.png");
		schema.add(RmcOrder.class,"/graphics/perspective/tall-building-32.png"); 
		schema.add(RmcDelivery.class, new RGB(255,0,0));
		// schema.add(Package.class, new RGB(0x0, 0x0, 0xFF));

		//View.setTestingMode(true);
		View.startGui(sim, 1, new PlaneRoadModelRenderer(), new RoadUserRenderer(schema,
				false));
		//View.startGui(sim, 1, new PlaneRoadModelRenderer(), new RoadUserRenderer());
		// in case a GUI is not desired, the simulation can simply be run by
		// calling: sim.start();
	}

}
