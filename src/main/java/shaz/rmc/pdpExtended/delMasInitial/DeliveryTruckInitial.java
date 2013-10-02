/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckCostForDelivery;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckTravelUnit;

import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.pdpExtended.delMasInitial.communication.TruckCommunicationStrategy;
import shaz.rmc.pdpExtended.delMasInitial.communication.TruckStrategyCoalition;
import shaz.rmc.pdpExtended.delMasInitial.communication.TruckStrategyDelMAS;

/**
 * @author Shaza
 *
 */
public class DeliveryTruckInitial extends rinde.sim.core.model.pdp.Vehicle implements MovingRoadUser, Agent {
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	private static Logger logger = Logger.getLogger(DeliveryTruckInitial.class);
	private final RandomGenerator randomPCSelector;
	
	Duration totalTravelTime; //keeps record of the time vehicle kept on traveling
	private int wastedConcrete; // to keep record of wasted concrete 
	
	private RoadModel roadModel;
	private PDPModel pdpModel;
	List<ProductionSiteInitial> sites;
	private final DeliveryTruckSchedule truckSchedule;
	
	private Point startLocation;
	private ProductionSite startPS;

	private static int totalDeliveryTruck = 0;
	private final int id;
	private final int dePhaseBySec;
	private final shaz.rmc.core.domain.Vehicle truck;
	private final DeliveryTruckInitialIntention i;
//	private TruckAgentState state;
	private final TruckCommunicationStrategy strategy;
	private final TruckAgentFailureManager truckFailureManager;
	
	public DeliveryTruckInitial( Vehicle pTruck, int pDePhaseBySec, RandomGenerator pRandomPCSelector, TruckAgentFailureManager pTruckAgentFailureManager) {
		setCapacity(pTruck.getNormalVolume());
		dePhaseBySec = pDePhaseBySec;
		truckFailureManager = pTruckAgentFailureManager;
		//System.out.println("Dephase no. is " + dePhaseByMin);
		randomPCSelector = pRandomPCSelector; //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		
		truckSchedule = new DeliveryTruckSchedule(new ArrayList<TruckScheduleUnit>());
		i = new DeliveryTruckInitialIntention(this);
		
		wastedConcrete = 0;
		totalTravelTime = new Duration(0); //to keep record of total travelling time, to be used by objective function latter.

		truck = pTruck;
		id = ++totalDeliveryTruck;
		if (GlobalParameters.ENABLE_JI)
			strategy = new TruckStrategyCoalition(this);
		else
			strategy = new TruckStrategyDelMAS(this);
		setTruckState(TruckAgentState.IN_PROCESS);
	
	}
	@Override
	protected void tickImpl(TimeLapse timeLapse) {
		strategy.handleMessages(timeLapse);
		strategy.executeStrategy(timeLapse);

	}

	/**
	 * @param timeLapse
	 */
	protected void followSchedule(TimeLapse timeLapse) {
		checkArgument(strategy.getState() instanceof TruckStateInProcess);
		//acting on intentions
		if (!truckSchedule.isEmpty()) {
			i.followSchedule(timeLapse);
		}
	}
	
	protected boolean canIBreakAt(DateTime currTime, int id2) {
		checkArgument(strategy.getState() instanceof TruckStateInProcess);
		return truckFailureManager.canIBreakAt(currTime, id2);
	}
	
	public void removeFromSchedule(TruckDeliveryUnit tdu) {
		truckSchedule.remove(tdu, this);
	}

	
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;	
		// setting startLocation 
		sites = new ArrayList<ProductionSiteInitial>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
		int rand = randomPCSelector.nextInt(sites.size());
		//in the begining truck is at start location
		setStartLocation( sites.get(rand).getLocation());
		setStartPS(sites.get(rand));
		roadModel.addObjectAt(this, getStartLocation());
		logger.info(this.getId()+"T capacity is = " + this.truck.getNormalVolume() + " & startLocation = " + sites.get(rand).getStation().getId());
	}
	public void setTruckState (int newState) {
		//this.state = TruckAgentState.newState(newState, this);
		strategy.setState(TruckAgentState.newState(newState, this));
	}
	/**
	 * @return the dePhaseByMin
	 */
	protected int getDePhaseBySec() {
		return dePhaseBySec;
	}
	protected RandomGenerator getRandomPCSelector (){
		return randomPCSelector;
	}
	/**
	 * @return the truckSchedule
	 */
	protected DeliveryTruckSchedule getTruckSchedule() {
		return truckSchedule;
	}
	protected List<ProductionSiteInitial> getSites() {
		return sites;
	}
	public int getWastedConcrete() {
		return wastedConcrete;
	}
	public void setWastedConcrete(int wastedConcrete) {
		this.wastedConcrete = wastedConcrete;
	}
	
	public boolean addWastedConcrete(int amount){
		this.wastedConcrete += amount;
		return true;
	}

	public Duration getTotalTravelTime() {
		return totalTravelTime;
	}
	
	public Point getStartLocation() {
		return startLocation;
	}

	public void setStartLocation(Point startLocation) {
		this.startLocation = startLocation;
	}
	public ProductionSite getStartPS() {
		return startPS;
	}

	public void setStartPS(ProductionSite startPS) {
		this.startPS = startPS;
	}

	public TimeSlot getTotalTimeRange() {
		return strategy.getState().getTotalTimeRange();
	}
	public shaz.rmc.core.domain.Vehicle getTruck() {
		return truck;
	}

	protected ArrayList<TruckScheduleUnit> getSchedule() {
		return truckSchedule.getSchedule();
	}
	protected ArrayList<TruckScheduleUnit> getPracticalSchedule() {
		return truckSchedule.getPracticalSchedule();
	}
	
	public TruckCostForDelivery getCostForMakingDelivery (Delivery del, TimeSlot timeWindow, DateTime currTime)
	{
		return truckSchedule.getCostForMakingDelivery(del, timeWindow, this, currTime);
	}
	public int getId() {
		return id;
	}
	public TruckCommunicationStrategy getStrategy() {
		return strategy;
	}
	@Override
	public void afterTick(TimeLapse timeLapse) {
		}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;
	}
	public CommunicationAPI getcApi() {
		return cApi;
	}
	
	@Override
	public Point getPosition() {
		return pdpModel.getPosition(this);
	}

	@Override
	public double getRadius() {
		return 100; // since distance sensitiviy doesn't matter..:P
	}
	
	@Override
	public double getSpeed() {
		return GlobalParameters.TRUCK_SPEED;
	}// As per my exploration, this speed is in units per hour. So the truck will travel x/hour. 
	
	
	public Queue<Message> receiveMessages() {
		return mailbox.getMessages();
	}
	@Override
	public double getReliability() {
		return 1; //completelly reliable trucks
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}

	public RoadModel getRoadModel() {
		return roadModel;
	}

	public PDPModel getPdpModel() {
		return pdpModel;
	}
	
	@Override
	public String toString() {
		return "Id = " + this.getId();
	}
	/**
	 * @return score of the proposed schedule 
	 */
	public ResultElementsTruck getTruckResult() {
		/*
		 * score concerns: travelDistance(20), ST Delay (10), wasted Concrete (10), preferred station (1)
		 */
		int travelMin = 0;
		int lagTimeInMin = 0;
		int startTimeDelay = 0; //already included in lag time
		int wastedConcrete = 0;
		int deliveredConcrete =0;
		int totalDeliveries = 0;
		if (!truckSchedule.isEmpty()) {
			for(TruckScheduleUnit u: truckSchedule.getPracticalSchedule()) {
				if (u instanceof TruckTravelUnit) 
					travelMin += ((TruckTravelUnit) u).getTravelTime().getStandardMinutes();
				else {
					totalDeliveries ++;
					travelMin += ((TruckDeliveryUnit) u).getDelivery().getStationToCYTravelTime().getStandardMinutes();
					if (((TruckDeliveryUnit) u).getDelivery().getDeliveryNo() == 0)
						startTimeDelay += (int)Math.ceil((double)((TruckDeliveryUnit) u).getLagTime().getStandardSeconds()/60d); //since for first delivery delay means lag time delay..
					else
						lagTimeInMin += (int)Math.ceil((double)((TruckDeliveryUnit) u).getLagTime().getStandardSeconds()/60d);
					wastedConcrete += ((TruckDeliveryUnit) u).getWastedConcrete();
					deliveredConcrete += ((TruckDeliveryUnit) u).getDelivery().getDeliveredVolume(); //truck used to deliver all concrete, even the one that will go wastage for order site. so unloading times would also have boht values
				}
			}
		ResultElementsTruck re = new ResultElementsTruck(totalDeliveries, travelMin, lagTimeInMin, startTimeDelay, wastedConcrete,deliveredConcrete);
			return re; 
		}
		else 
			return null;
	}
	public String getScheduleToPrint() {
		StringBuffer strbuf = new StringBuffer();
		for (TruckScheduleUnit tsu : this.truckSchedule.getPracticalSchedule()){
			strbuf.append(tsu.toString());
		}
		return strbuf.toString();
		
	}
	public String getState() {
		
		return this.strategy.getState().toString();
	}

}
