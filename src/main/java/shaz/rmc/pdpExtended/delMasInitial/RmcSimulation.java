package shaz.rmc.pdpExtended.delMasInitial;

import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.eclipse.swt.graphics.RGB;
import org.joda.time.DateTime;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;
import rinde.sim.ui.renderers.UiSchema;

import shaz.rmc.core.domain.Problem;
import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.core.domain.XMLProblemDAO;

public class RmcSimulation {

	/**
	 * @param args
	 */
	private static Logger log = Logger.getLogger(RmcSimulation.class);
	private static Simulator sim;
	public static void main(String[] args) {
	
		final RandomGenerator rng = new MersenneTwister(250);
		sim = new Simulator(rng, 200); //tick is one milli second, step = 200ms 
		/* Discussed with Rinde that truck.getSpeed and roadModel.maxSpeed should always be in same units. 
		 * Here I use unit/hour or km/hour.
		 * Also if useSpeedConversion = true, then distance per hour will be matched with the tick time, and the move
		 * of truck will be accrodingly. I haven't tested this yet. Rin was saying that its simple to use useSpeedConversion = false
		 * and set a greater valueto the maxSpeed so that vehicles actual speed is used by model and maxSpeed doesn't 
		 * have to constraint vehicles speed.
		 * */
		final PlaneRoadModel prm = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), true, 10);//10by10 km plane
		CommunicationModel communicationModel = new CommunicationModel(rng, true);
		final PDPModel pdpModel = new PDPModel();
		final StatisticTracker stTracker = new StatisticTracker(sim , pdpModel);
		sim.register(prm);
		sim.register(pdpModel);
		sim.register(communicationModel);
		sim.getEventAPI().addListener(stTracker, SimulatorEventType.values());
		sim.configure();
		final RmcSimulation rmSim = new RmcSimulation(); 
		rmSim.loadProblem(sim);

		//Adding prodcuction Sites
		for (int j = 0; j<2 ; j++) 
			sim.register(new ProductionSiteInitial(rng, GlobalParameters.PROBLEM.getStations().get(j)));
		
		//Adding orders
		int  k =0, l =0, m =0, n =0 ; //4, 43, 19, 55
		for (int i = 0; i< GlobalParameters.PROBLEM.getOrders().size(); i++){
			if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("43")) {
				k = i;
				System.out.println(GlobalParameters.PROBLEM.getOrders().get(i).toString());
			}
			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("4")) {
				l = i;
				System.out.println(GlobalParameters.PROBLEM.getOrders().get(i).toString());
			}
//			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("19"))
//				m = i;
			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("55")) {				
				n = i;		
				System.out.println(GlobalParameters.PROBLEM.getOrders().get(i).toString());
			}
		}

		System.out.println("k= " +k+", l = " +l+ ", m = "+m+ ", n =" + n);
		sim.register(new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(k<GlobalParameters.PROBLEM.getOrders().size()?k:k-1)));
		sim.register(new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(l<GlobalParameters.PROBLEM.getOrders().size()?l:l-1)));
		//sim.register(new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(m<GlobalParameters.PROBLEM.getOrders().size()?m:m-1)));
		sim.register(new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(n<GlobalParameters.PROBLEM.getOrders().size()?n:n-1)));
//		for (int i = 0; i< GlobalParameters.PROBLEM.getOrders().size()-1; i++){
//			sim.register(new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(i)));
//		}
		
		//Adding Delivery Trucks
		for (int j = 0; j< 4 /*GlobalParameters.TOTAL_TRUCKS*/ ; j ++){
			sim.register(new DeliveryTruckInitial(prm.getRandomPosition(rng), rmSim.getTruck(j)));
		}
		
		sim.register(new TickListener() {
			public void tick(TimeLapse timeLapse) {
//				if (timeLapse.getStartTime() == 10800000) //3 hours passed
//				{
//					sim.register(new DeliveryTruckInitial(prm.getRandomPosition(rng), rmSim.getTruck(6)));
//					sim.register(new DeliveryTruckInitial(prm.getRandomPosition(rng), rmSim.getTruck(7)));
//					log.debug("Registered new Trucks");
//				}
				if (timeLapse.getStartTime() + GlobalParameters.START_DATETIME.getMillis() >= GlobalParameters.END_DATETIME.getMillis()){
					log.debug("Stopping Simulation: CURRENT TIME = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + timeLapse.getStartTime()) 
					+ " & ENDTIME = " + GlobalParameters.END_DATETIME);
					sim.stop();
				}		
			}
			public void afterTick(TimeLapse timeLapse) {}
			});
		//gui
		final UiSchema schema = new UiSchema();
		schema.add(DeliveryInitial.class, new RGB(0, 0, 255));
		schema.add(DeliveryTruckInitial.class, "/graphics/perspective/concreteTruck-32.png");
		schema.add(ProductionSiteInitial.class, "/graphics/perspective/productionSite-32.png");
		schema.add(OrderAgentInitial.class,"/graphics/perspective/tall-building-32.png"); 
		
		//View.startGui(sim, 1, new PlaneRoadModelRenderer(), new RoadUserRenderer(schema,false));
		sim.start();
		//if (!sim.isPlaying()) {
			log.debug(stTracker.collectDataString());
			log.debug(stTracker.collectStatistics());
		//}
	}

	/**
	 * Loads the problem file into GlobalParameters.PROBLEM using XMLPROBLEMDAO 
	 * @param sim Simulator instance.
	 * 
	 */
	protected void loadProblem(Simulator sim) {
		XMLProblemDAO dao = new XMLProblemDAO(new File(GlobalParameters.DATA_FOLDER+GlobalParameters.INPUT_FILE));
		Problem problem = null;
		try {
			problem = dao.loadProblem();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		GlobalParameters.PROBLEM = problem;
		//log.debug("problem is LOADED, CURRENT_TIME is = "  + new DateTime(GlobalParameters.START_DATETIME.getMillis() + (sim.getCurrentTime())) );
		assert problem == null : "Problem is not Loaded!"; 
	}
	
	/**
	 * @param pI
	 * @return If GlobalParameters.FIXED_VEHICLE_CAPACITY == true, it will return same truck value for all RmcTruck agents. 
	 * Else it will return ith truck from Problem.getVehicles List. 
	 */
	private Vehicle getTruck(int pI) {
		if (GlobalParameters.IS_FIXED_VEHICLE_CAPACITY ) //same kind of vehicle for each agent..
		{
			int i = -1;
			do { 
			i++;
			} while ( (!GlobalParameters.PROBLEM.getVehicles().get(i).getId().equals("M62")) && i < GlobalParameters.PROBLEM.getVehicles().size()) ;
			//System.out.println("i is " + i + GlobalParameters.PROBLEM.getVehicles().get(i).getId());
			return GlobalParameters.PROBLEM.getVehicles().get(i);
		}
		else 
			 return GlobalParameters.PROBLEM.getVehicles().get(pI);
	}

}
