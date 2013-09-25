package shaz.rmc.pdpExtended.delMasInitial;

import java.io.File;
import java.io.IOException;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
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
		//int randomSeed = 250; //if randomSeed == 0, then each generato is a new random Generator without any seed, else same rng should be passed to all
		int randomSeed = 0;
		final RandomGenerator rng = new MersenneTwister(randomSeed);
		sim = new Simulator(rng, 200); //tick is one milli second, step = 200ms 
		/* Discussed with Rinde that truck.getSpeed and roadModel.maxSpeed should always be in same units.  
		 * Here I use unit/hour or km/hour.																	
		 * Also if useSpeedConversion = true, then distance per hour will be matched with the tick time, and the move
		 * of truck will be accrodingly. I haven't tested this yet. Rin was saying that its simple to use useSpeedConversion = false
		 * and set a greater valueto the maxSpeed so that vehicles actual speed is used by model and maxSpeed doesn't 
		 * have to constraint vehicles speed.
		 * */
		final PlaneRoadModel prm = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), true, 10);//10by10 km plane
		CommunicationModel communicationModel;
		
		
		if (randomSeed !=0)
			 communicationModel = new CommunicationModel(rng, true);
		else
			 communicationModel = new CommunicationModel(new MersenneTwister(), true);
		final PDPModel pdpModel = new PDPModel();
		
		sim.register(prm); 
		sim.register(pdpModel);
		sim.register(communicationModel);
		
		final RmcSimulation rmSim = new RmcSimulation(); 
		rmSim.loadProblem();
		//adding managers..
		OrderManagerInitial omi;
		TruckAgentFailureManager tfm;
		final int noOfTruckToBeFailed = 1;
		if (randomSeed == 0){
			omi = new OrderManagerInitial(sim, new MersenneTwister(), prm);
			tfm = new TruckAgentFailureManager(new MersenneTwister(), noOfTruckToBeFailed);
		}
		else { 
			omi = new OrderManagerInitial(sim, rng, prm);
			tfm = new TruckAgentFailureManager(rng, noOfTruckToBeFailed);
		}
		// Statistic Tracker
		final StatisticTracker stTracker = new StatisticTracker(sim , pdpModel, omi, prm);
		sim.getEventAPI().addListener(stTracker, SimulatorEventType.values());
		
		
		sim.configure();
		
		
		//adding order manager	
		sim.register(omi);
		
		
		//Adding prodcuction Sites
		for (int j = 0; j<3 ; j++) {
			if (randomSeed == 0)
				sim.register(new ProductionSiteInitial(new MersenneTwister(), GlobalParameters.PROBLEM.getStations().get(j)));
			else
				sim.register(new ProductionSiteInitial(rng, GlobalParameters.PROBLEM.getStations().get(j)));
		}
		
		//Adding orders
		omi.addOrders();
		
		
		//Adding Delivery Trucks
		RandomGenerator randomPCSelector;
		if (randomSeed == 0 )
			randomPCSelector =   new MersenneTwister();
		else
			randomPCSelector =   new MersenneTwister(randomSeed+2);
		for (int j = 0; j< GlobalParameters.PROBLEM.getVehicles().size() ; j ++){
			if (randomSeed == 0)
				sim.register(new DeliveryTruckInitial(rmSim.getTruck(j), 
						new MersenneTwister().nextInt(GlobalParameters.DEPHASE_INTERVAL_SEC), randomPCSelector, tfm));
			else sim.register(new DeliveryTruckInitial(rmSim.getTruck(j), 
					rng.nextInt(GlobalParameters.DEPHASE_INTERVAL_SEC), randomPCSelector, tfm));
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
		String writeString = stTracker.collectStatistics(); 
			log.info(writeString);
			stTracker.writeFile();
			log.info(stTracker.collectDataString());
			
		//}
	}

	/**
	 * Loads the problem file into GlobalParameters.PROBLEM using XMLPROBLEMDAO 
	 * 
	 */
	protected void loadProblem() {
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
