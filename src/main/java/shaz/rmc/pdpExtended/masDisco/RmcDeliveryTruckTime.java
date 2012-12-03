/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.pdp.PDPModel;
//import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;

import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.ExplorationAnt;
import shaz.rmc.core.Agent;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
//import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.core.IntentionAnt;
import shaz.rmc.core.domain.Station;

/**
 * The truck agent for RMC problem.
 * contains separate Belief part.
 * Intention depends on physical layer as well, it is in separate intention class.
 *
 * @author Shaza
 *
 */
public class RmcDeliveryTruckTime extends rinde.sim.core.model.pdp.Vehicle implements MovingRoadUser, Agent {
	//protected Point startLocation;
	private final RandomData randomPCSelector;
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	private final Simulator sim;
	private static Logger log = Logger.getLogger(RmcDeliveryTruckTime.class);
	
	private RoadModel roadModel;
	private PDPModel pdpModel;
	List<RmcProductionSite> sites;
	ArrayList<TruckScheduleUnit> schedule;
	
	private static int totalDeliveryTruck = 0;
	private final int id;
	
	private final RmcDeliveryTruckBeliefs b;
	private final RmcDeliveryTruckIntentionTime i;

	private final shaz.rmc.core.domain.Vehicle truck;
	
	boolean firstTime; //to track if initialization is required?
	long explorationCount; //to keep track that should exploration ants be sent out or not?increamented each time delebrate() method is called
	
	public RmcDeliveryTruckTime (Point pStartLocation, shaz.rmc.core.domain.Vehicle pTruck, Simulator pSim){
		
		sim = pSim;
		
		this.schedule = new ArrayList<TruckScheduleUnit>();
		randomPCSelector = new RandomDataImpl(); //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		b = new RmcDeliveryTruckBeliefs();
		i = new RmcDeliveryTruckIntentionTime(this, b, schedule);
		explorationCount = 0; 
				
		firstTime = true;
		truck = pTruck;
		id = ++totalDeliveryTruck;
	}
	
	
	@Override
	protected void tickImpl(TimeLapse time) {
		if (firstTime)
			initialize();
		deliberate(time);
		//acting on intentions
		if (!schedule.isEmpty())
			i.followSchedule(time);
				
	}
	/**
	 * Do the communication and deliberation task
	 */
	private void deliberate(TimeLapse time) {
		explorationCount = explorationCount + 1;
		
	//Propose new order, based on OrderSites at roadModel
		final Collection<OrderSite> orderSites = roadModel.getObjectsOfType(OrderSite.class);		
		RmcOrderAgent currOrder;
		DateTime currDateTime = new DateTime(GlobalParameters.START_DATETIME.getMillis() + (sim.getCurrentTime()));
		if (!orderSites.isEmpty() &&
				(explorationCount+b.explorationRandomFactor) % GlobalParameters.EXPLORATION_RATE == 0 && 
				b.getAvailableslot().getStartTime().compareTo(currDateTime.plusHours(GlobalParameters.TRUCKS_PLAN_IN_ADVANCE_HOURS)) < 0  ) {
			double selectedDist = Double.POSITIVE_INFINITY;
			for (final OrderSite p : orderSites) { //get the one with smallest distance
				final double dist = Point.distance(roadModel.getPosition(this), roadModel.getPosition(p));
				if (dist <= selectedDist) {
					if (!isProposed(p.getOrder()) && p.getOrder().isOrderReserved() == false) { //propose only if order isn't reserved
						selectedDist = dist;
						currOrder = p.getOrder();
						ExplorationAnt expAnt = new ExplorationAnt(this,  sim.getCurrentTime());
						assert b.getAvailableslot().getLocationAtStartTime() != null : this.getId()+"T: availableSlot.getLocationAtStartTime is null";
						Duration d = GlobalParameters.PROBLEM.getDuration(b.getAvailableslot().getProductionSiteAtStartTime().getStation(), currOrder.getOrder().getConstructionYard()); //duraion to reach CY
						expAnt.configure(currOrder, b.getAvailableslot(), d.plus(b.getAvailableslot().getProductionSiteAtStartTime().getStation().getLoadingDuration())); //loading duraiton at PC
						cApi.send(currOrder, expAnt);
						b.explorationAnts.add(expAnt);
						log.debug(this.getId()+"T: Adding next exp ant to order " + currOrder.getOrder().getId() + " with available slot: S= " +
								b.getAvailableslot().getStartTime() + " & E= " + b.getAvailableslot().getEndTime() +" and PC = " + b.getAvailableslot().getProductionSiteAtStartTime().getStation().getId());
								b.addProposedOrder(b.new ProposedOrder(currOrder, 1));
					}
				}
			}
		}	
		
	//check exploration ants have returned
		//check exploration ants retruned?
		if ((!b.explorationAnts.isEmpty())) {//&& continueExploration == true) {
			//logger.debug(b.truck.getId()+ "T: processing exp ants and CURRENT TIME IS = " + new DateTime(GlobalVariables.START_DATETIME.getMillis() + (VirtualClock.currentTime()/1000)) );
			Iterator<ExplorationAnt> i = b.explorationAnts.iterator();
			while (i.hasNext()) { //out of these one best should be selected and proposed
				ExplorationAnt exp = i.next();
				if (time.getStartTime() == exp.getCreatedTime() * GlobalParameters.EXPLORATION_EVAPORATION_RATE ) {
					log.debug(this.getId()+ "T: evaporated Exp Ant & CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()/1000)));
					i.remove();
				}
				if (exp.isReturned() && exp.getDelivery() != null) { //returned with delivery
					RmcProductionSite ps = (RmcProductionSite)exp.getAvailableTimeSlot().getProductionSiteAtStartTime();   
					DateTime startTime = ps.getStation().makeBookingAt(exp.getAvailableTimeSlot().getStartTime(), this);
					if (startTime == null)  {//means station isn't free afterwards..
						log.debug(this.getId()+ "T: Station " + ps.getStation().getId() + " is not available");
						i.remove();//no need to further think about it..
						return;
					}
					else {
						assert startTime.compareTo(exp.getAvailableTimeSlot().getStartTime()) >= 0 : this.getId()+"T: Station's Available time is before Truck's available time;" +
						" startTime = " + startTime + "amd truck is available at = " + b.getAvailableslot().getStartTime();
						IntentionAnt intAnt = new IntentionAnt(this);
						intAnt.configure(exp.getOrder(), exp.getDelivery(), new TimeSlot(startTime, null, exp.getAvailableTimeSlot().getLocationAtStartTime(), exp.getAvailableTimeSlot().getProductionSiteAtStartTime()) ); //end time is set at the time of adding unit to truck schedule
						cApi.send(exp.getOrder(), intAnt);
						b.intentionAnts.add(intAnt);
						log.debug(this.getId()+ "T: Intention ant sent to " + ((RmcOrderAgent)exp.getOrder()).getOrder().getId() +"O & CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()/1000)));
						i.remove(); //remove expant
						log.debug(this.getId()+ "T: Proposal accepted at delivery time = " +exp.getAvailableTimeSlot().getStartTime());
						break;
					}
					
				}
				else if (exp.isReturned()) {
					assert exp.getDelivery() != null : this.getId()+"T Delivery should be NULL!!";
					log.debug(this.getId()+ "T: exp from "+ ((RmcOrderAgent)exp.getOrder()).getOrder().getId()+ "O returned without Delivery & CURRENT TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + (time.getStartTime()/1000)));
					i.remove();
				}
			}
		}
		
	//Check if intention ants replied...
		if(!b.intentionAnts.isEmpty()) {
			//System.out.println(b.truck.getId() + "T: processing int ants");
			Iterator<IntentionAnt> i = b.intentionAnts.iterator();
			while (i.hasNext())  {
				IntentionAnt intAnt = i.next();
				if (intAnt.getReply() == Reply.NO_REPLY || intAnt.getReply() == Reply.UNDER_PROCESS)
					continue;
				else if (intAnt.getReply() == Reply.REJECT) {
					log.debug(this.getId()+ "T: Reply of intention ant from " + ((RmcOrderAgent)intAnt.getOrder()).getOrder().getId() + "O is REJECTED");
					i.remove();//remove intention
				}
				else {//Reply.ACCEPT
					assert intAnt.getReply() == Reply.ACCEPT : this.getId()+ "T: Reply wasn't ACCEPT rather it was " + intAnt.getReply();
					//continueExploration = true;
					Delivery d = intAnt.getDelivery();
					d.setStationToCYTravelTime(GlobalParameters.PROBLEM.getDuration(intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime().getStation(), ((RmcOrderAgent)intAnt.getOrder()).getOrder().getConstructionYard()));
					d.setLoadingStation(intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime());
					d.setLoadingDuration(((RmcProductionSite)d.getLoadingStation()).getStation().getLoadingDuration());
					//int rand = randomPCSelector.nextInt(0, GlobalParameters.PROBLEM.getStations().size()-1); //truck will return to any station at random after unloading
					int rand = randomPCSelector.nextInt(0, sites.size()-1); //truck will return to any station at random after unloading
					d.setReturnStation(sites.get(rand));
					d.setCYToStationTravelTime(GlobalParameters.PROBLEM.getDuration(((RmcOrderAgent)intAnt.getOrder()).getOrder().getConstructionYard(), GlobalParameters.PROBLEM.getStations().get(rand))); //Only one PC is assumed
					TruckScheduleUnit unit = new TruckScheduleUnit(this);
					b.addWastedConcrete(this.getTruck().getNormalVolume() - d.getDeliveredVolume());
					d.setWastedVolume(this.getTruck().getNormalVolume() - d.getDeliveredVolume());
					unit.setDelivery(d); 
					unit.setTimeSlot(new TimeSlot(d.getDeliveryTime().minus(d.getStationToCYTravelTime().getMillis()).minus(d.getLoadingDuration().getMillis()), //start time of unit,
							d.getDeliveryTime().plus(d.getUnloadingDuration()).plus(d.getCYToStationTravelTime()), //end time of schedule unit
							sites.get(rand).getLocation(), sites.get(rand)   ));  //Location (point) and production site where truck will return
					this.schedule.add(unit);
					log.debug(this.getId()+ "T: New unit added:  "+unit.toString());
					log.debug(this.getId() + "T: added Waste is : " + (this.getTruck().getNormalVolume() - d.getDeliveredVolume()));
					log.debug(this.getId()+"T: Station " + intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime().getStation().getId()+ " availability List is: " + intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime().getStation().pringAvailabilityList());
					i.remove();
				}
			}
		}
	}

	private void sendMsgs(long currentTime) {
		//if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
			//lastCommunication = currentTime;
			if (cApi != null) {
				cApi.broadcast(new Message(this) {});
				System.out.println(this.id + "T message broad caseted");
			}
//		}
	}

	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0)
			System.out.println(this.id+"T received messages quantity = " + messages.size());
//		for (Message m : messages) {
//			lastCommunicationTime.put((RandomWalkAgent) m.getSender(), currentTime);
//			communications++;
//		}
	}
	
	/**
	 * @param currOrder, to check if this Order has been already proposed by truck
	 * @return 
	 */
	private boolean isProposed(RmcOrderAgent currOrder){
		if(!b.getProposedOrders().isEmpty()) {
			for(RmcDeliveryTruckBeliefs.ProposedOrder or : b.getProposedOrders()) {
				if (or.getOrderAgent().getOrder().getId().compareTo(currOrder.getOrder().getId()) == 0){
					or.incrementNoOfTimes();
					if (or.getNoOfTimes() % 20 == 0) //means 100/20 = 5% times it will be that proposed again..
						return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * For doing some initial tasks in fist tick
	 */
	private void initialize() {
		this.firstTime = false;
	}
	
	public shaz.rmc.core.domain.Vehicle getTruck() {
		return truck;
	}

	public int getId() {
		return id;
	}

	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;	
		// setting startLocation 
		sites = new ArrayList<RmcProductionSite>(roadModel.getObjectsOfType(RmcProductionSite.class));
		int rand = randomPCSelector.nextInt(0, sites.size()-1);
		b.getAvailableslot().setLocationAtStartTime(sites.get(rand).getLocation(), sites.get(rand)); //in the begining truck is at start location
		b.startLocation = sites.get(rand).getLocation();
		//setStartPosition(b.startLocation);
		roadModel.addObjectAt(this, b.startLocation);
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;
	}
	
	@Override
	public Point getPosition() {
		return roadModel.getPosition(this);
	}

	@Override
	public double getRadius() {
		return 0; // since distance sensitiviy doesn't matter..:P
	}
	
	@Override
	public double getSpeed() {
		return .03;
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

}
