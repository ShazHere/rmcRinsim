/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Queue;
import java.util.Map;


import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;


import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Station;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.FeaAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class ProductionSiteInitial extends Depot implements ProductionSite, Agent{
	private RoadModel roadModel;
	private PDPModel pdpModel;
	private Point Location;
	
	protected final RandomGenerator rnd;
	private final Station station;

	private final Logger logger; //for logging
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	//All these could be managed in seperate inner class..
	private final Map<OrderAgentInitial, DateTime> interestedTime; //pheromone values to store info
	private final Map<OrderAgentInitial, Double> travelDistanceToOrder; 
	private final Map<OrderAgentInitial, Long > pheromoneUpdateTime;
	private final Map<OrderAgentInitial, Long > noOfExplorations;
	
	private ArrayList<FeaAnt> feasibilityAnts;
	private ArrayList<ExpAnt> explorationAnts;
	private ArrayList<IntAnt> intentionAnts;
	
	public ProductionSiteInitial(RandomGenerator pRnd, Station pStation) {
		station = pStation;	
		rnd = pRnd;
		mailbox = new Mailbox();
		logger = Logger.getLogger(ProductionSiteInitial.class);
		
		feasibilityAnts = new ArrayList<FeaAnt>();
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		interestedTime = new LinkedHashMap<OrderAgentInitial, DateTime>(); 
		travelDistanceToOrder = new LinkedHashMap<OrderAgentInitial, Double>();
		pheromoneUpdateTime = new LinkedHashMap<OrderAgentInitial, Long >();
		noOfExplorations = new LinkedHashMap<OrderAgentInitial, Long >();
	}
	
	@Override
	public void tick(TimeLapse timeLapse) {
		try {
			checkMsgs (timeLapse.getStartTime());
			evaporatePheromones(timeLapse);
			DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
			processFeasibilityAnts(timeLapse);
			processExplorationAnts(currTime);
			processIntentionAnts(currTime);
			station.evaporateBookings(currTime); //related to truck intentions of loading concrete
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	private void evaporatePheromones(TimeLapse timeLapse) {
		long currMilli; 
		ArrayList<OrderAgentInitial> removeAbleOrd = new ArrayList<OrderAgentInitial>();
		for (OrderAgentInitial or:pheromoneUpdateTime.keySet()) { 
			currMilli = timeLapse.getStartTime() -  pheromoneUpdateTime.get(or);
			if (new Duration(currMilli).getStandardMinutes() >= GlobalParameters.FEASIBILITY_EVAPORATION_INTERVAL_MIN ) {
				removeAbleOrd.add(or); //FEASIBILITY_EVAPORATION_INTERVAL_MIN is used since this info is only feasiblity related
			}
		}		
		if (!removeAbleOrd.isEmpty()) {
			for (OrderAgentInitial ord : removeAbleOrd) {
				interestedTime.remove(ord);
				travelDistanceToOrder.remove(ord);
				pheromoneUpdateTime.remove(ord);
				noOfExplorations.remove(ord);
			}
			logger.debug(station.getId() +"P pheromone table after evaporation: (curTime="+ GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime()) +")\n" + this.pheromoneToString());
		}
	}

	private void processIntentionAnts(DateTime currTime) {
		if (intentionAnts.isEmpty()) 
			return;
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //includes booking of Station
			IntAnt iAnt = i.next();
			checkArgument(iAnt.getCurrentUnit().getTunit().getTimeSlot().getProductionSiteAtStartTime() == iAnt.getCurrentUnit().getTunit().getDelivery().getLoadingStation(),
					"UnExpectedly, INtAnt getProductionAtStartTime != getDelivery.getLoadingStation");
			if (iAnt.isScheduleComplete()) { //send back to originator
				IntAnt newAnt = (IntAnt)iAnt.clone(this);
				logger.debug(station.getId() +"P Int-" +newAnt.getOriginator().getId()+ " is being sent back since schedule complete with schedule size = " + newAnt.getSchedule().size());
				cApi.send(newAnt.getOriginator(), newAnt); //sending back to the truck who originated exp ant
				i.remove();
				continue;
			}
			DateTime stationReplyDate = station.makeBookingAt(iAnt.getCurrentUnit().getTimeSlot().getStartTime(), iAnt.getOriginator(), iAnt.getCurrentUnit().getDelivery(), currTime);
			if (stationReplyDate != null) { //means station is booked or refreshed
				if (stationReplyDate.equals(new DateTime(0))) { //means booking refrehe d
					iAnt.getCurrentUnit().setPsReply(Reply.WEEK_ACCEPT);
				}
				else { //the dateTime booked is returned
					iAnt.getCurrentUnit().setPsReply(Reply.UNDER_PROCESS);
				}
			}
			else {
				iAnt.getCurrentUnit().setPsReply(Reply.REJECT);
			}
			logger.debug(station.getId() +"P Int-" +iAnt.getOriginator().getId()+ " " + iAnt.getCurrentUnit().getPsReply() +" by PS" + " currTime= " + currTime);
			IntAnt newAnt = iAnt.clone(this);
			cApi.send(iAnt.getCurrentUnit().getDelivery().getOrder(), newAnt);
			i.remove();
		}
		logger.debug(station.getId() +"P STATTION  Availability List = " + station.pringAvailabilityList());
		checkArgument (intentionAnts.isEmpty(), true);
	}

	private void processExplorationAnts(DateTime currTime)  {
		if (explorationAnts.isEmpty()) 
			return;
		Iterator<ExpAnt> i = explorationAnts.iterator();
		while (i.hasNext()) {  //first two if-else to check should exp's be sent back to orignator, or let them further explore"?
			ExpAnt exp = i.next();
			if (exp.isScheduleComplete()) { //send back to orginator if according to GlobalParameters.EXPLORATION_SCHEDULE_SIZE, schedule is done
				ExpAnt newExp = (ExpAnt)exp.clone(this);
				logger.debug(station.getId() +"P Exp-" +exp.getOriginator().getId()+ " is being sent back since schedule complete with schedule size = " + newExp.getSchedule().size());
				cApi.send(newExp.getOriginator(), newExp); //sending back to the truck who originated exp ant
				i.remove();
				continue;
			}
			else if (exp.getNewScheduleSize() > 0) { //send an exp back any way
					ExpAnt newExp = (ExpAnt)exp.clone(this);
					logger.debug(station.getId() +"P Exp-" +exp.getOriginator().getId()+ " is being sent back with schedule size = " + newExp.getSchedule().size());
					cApi.send(newExp.getOriginator(), newExp);
				}
		//let exp further explore
			sendToOrders(exp, currTime);
			i.remove(); // if exp is not further intereste it will automaticaly die..
		} //end while (i.hasNext())
		checkArgument (explorationAnts.isEmpty(), true);
		logger.debug(station.getId() +"P pheromone table: (curTime="+ currTime +")\n" + this.pheromoneToString());
	}
	/**
	 * Should be called when the expAnt is not to be sent to its orginator, rather it needed to be further sent to other orders
	 * @param exp the ExpAnt that needed to be sent around
	 * @param timeLapse
	 */
	private void sendToOrders(ExpAnt exp, DateTime currTime) {
		if (exp.getSender().getClass() == DeliveryTruckInitial.class)  //this if-else is just for logging purpose
			logger.debug(station.getId() +"P Exp-" +exp.getOriginator().getId()+ ", sender= "+ ((DeliveryTruckInitial)exp.getSender()).getId() +"T expScheduleSize = " + exp.getSchedule().size());
		else if (exp.getSender().getClass() == OrderAgentInitial.class)
			logger.debug(station.getId() +"P Exp-" +exp.getOriginator().getId()+ ", sender= "+ ((OrderAgentInitial)exp.getSender()).getOrder().getId() +"O expScheduleSize = " + exp.getSchedule().size());
		
		for (OrderAgentInitial or : interestedTime.keySet()) { //check all order pheromones..
			if(exp.isInterested(interestedTime.get(or), travelDistanceToOrder.get(or), currTime) 
					&& noOfExplorations.get(or) <= GlobalParameters.MAX_NO_OF_EXPLORATION_FOR_ORDER) { //if interested, and order in't explored too much
				logger.debug(station.getId() +"P exp-" +exp.getOriginator().getId()+ " is interested " + exp.getCurrentInterestedTime() +
						", orderInterested " + interestedTime.get(or) + " & curTim=" + currTime);//", travelDistance= " + travelDistanceToOrder.get(or));
				checkArgument(noOfExplorations.containsKey(or), true); 
				noOfExplorations.put(or, noOfExplorations.get(or)+1);
				//exp.getCurrentUnit().getTimeSlot().setLocationAtStartTime(this.getPosition(), this); //normally make this as start PS
				ExpAnt newExp = (ExpAnt)exp.clone(this);
				cApi.send(or, newExp); //send exp clones to all orders(or) it is interested in
			}
		}	
	}
	private void processFeasibilityAnts(TimeLapse timeLapse) {
		if (feasibilityAnts.isEmpty())
			return;
		for (FeaAnt f: feasibilityAnts) {
			interestedTime.put((OrderAgentInitial)f.getSender(), f.getInterestedAt());
			travelDistanceToOrder.put((OrderAgentInitial)f.getSender(), Point.distance(this.getPosition(), ((OrderAgentInitial)f.getSender()).getPosition()));
			pheromoneUpdateTime.put((OrderAgentInitial)f.getSender(), timeLapse.getStartTime());
			noOfExplorations.put((OrderAgentInitial)f.getSender(),0l); //so current info is acquired by none..
		}
		feasibilityAnts.clear();
		checkArgument (feasibilityAnts.isEmpty(), true);
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
	}
	
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		for (Message m : messages) {
			if (m.getClass() == FeaAnt.class) {
				this.feasibilityAnts.add((FeaAnt)m);
			}
			else if (m.getClass() == IntAnt.class) {
				this.intentionAnts.add((IntAnt)m);
			}
			else if (m.getClass() == ExpAnt.class) {
				this.explorationAnts.add((ExpAnt)m);
			}
		}
	}
	
	
	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;				
	}
	@Override
	public Point getPosition() {
		checkArgument(pdpModel.getPosition(this) == this.Location, "Some problem in site postion");
		return pdpModel.getPosition(this);
	}
	@Override
	public double getRadius() {
		return 1000;
	}
	@Override
	public double getReliability() {
		return 1;
	}
	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}
	@Override
	public int getId() {
		return 0; //Integer.getInteger(this.getStation().getId());
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
		Location = roadModel.getRandomPosition(rnd);
		//TODO: check distance between two PS should be greater than some value, so that they are enough spread across.
		//roadModel.getObjectsOfType(ProductionSiteInitial.class);
		roadModel.addObjectAt(this,Location); 
	}
	public Station getStation() {
		return station;
	}
	
	public Point getLocation() {
		return getPosition();
	}
	public double getTravelDistance(OrderAgentInitial pOrder) {
		return travelDistanceToOrder.get(pOrder);
	}
	public DateTime getInterestedTime(OrderAgentInitial pOrder) {
		return interestedTime.get(pOrder);
	}
	public String pheromoneToString () {	
		
		StringBuilder sb = new StringBuilder();
		for (OrderAgentInitial or : this.interestedTime.keySet()) {
			sb.append(or.getOrder().getId() + "O ");
			sb.append("[time = " + this.interestedTime.get(or));
			sb.append(", travelDistance = " + this.travelDistanceToOrder.get(or));
			sb.append(", explorations = " + this.noOfExplorations.get(or));
			sb.append("] \n");
		}
		return sb.toString();
	}
	@Override
	public String toString () {
		return this.station.toString();
	}
}
