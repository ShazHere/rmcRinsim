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
	
	protected ArrayList<FeaAnt> feasibilityAnts;
	protected ArrayList<ExpAnt> explorationAnts;
	protected ArrayList<IntAnt> intentionAnts;
	
	public ProductionSiteInitial(RandomGenerator pRnd, Station pStation) {
		station = pStation;	
		rnd = pRnd;
		mailbox = new Mailbox();
		logger = Logger.getLogger(ProductionSiteInitial.class);
		
		feasibilityAnts = new ArrayList<FeaAnt>();
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		interestedTime = new LinkedHashMap();
		travelDistanceToOrder = new LinkedHashMap();
		pheromoneUpdateTime = new LinkedHashMap();
		noOfExplorations = new LinkedHashMap();
	}
	
	@Override
	public void tick(TimeLapse timeLapse) {
		try {
		checkMsgs (timeLapse.getStartTime());
		evaporatePheromones(timeLapse);
		processFeasibilityAnts(timeLapse);
		processExplorationAnts(timeLapse);
		processIntentionAnts(timeLapse);
		/*it shoud check the messages, then 
		if feas, then generate pheromone accrodingly..
			create set for making pheromone..chk pdpmodel for inspiration..
		if exp, let it see the pheromones..
			also exp shud include station booking in schedule as well
		
		*/
		}
		catch (Exception ex) {
			//logger.debug(ex.getStackTrace());
			ex.printStackTrace();
		}
	}
	private void evaporatePheromones(TimeLapse timeLapse) {
		long currMilli; 
		for (OrderAgentInitial or:pheromoneUpdateTime.keySet()) {
			currMilli = timeLapse.getStartTime() -  pheromoneUpdateTime.get(or);
			if (new Duration(currMilli).getStandardMinutes() >= GlobalParameters.FEASIBILITY_EVAPORATION_INTERVAL_MIN ) {
				;//TODO  remove order from all Maps..
			}
		}		
	}

	private void processIntentionAnts(TimeLapse timeLapse) {
		if (intentionAnts.isEmpty()) 
			return;
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //include booking of Station
			IntAnt iAnt = i.next();
			checkArgument(iAnt.getCurrentUnit().getTimeSlot().getProductionSiteAtStartTime() == iAnt.getCurrentUnit().getDelivery().getLoadingStation(),
					"UnExpectedly, INtAnt getProductionAtStartTime != getDelivery.getLoadingStation");
			//checkArgument(iAnt.getCurrentUnit().getDelivery().getLoadingStation() == this, "Unexpected: Int Ant not at properLoading station");
			if (iAnt.isScheduleComplete()) { //send back to orginator
				IntAnt newAnt = (IntAnt)iAnt.clone(this);
				logger.debug(station.getId() +"P Int is being sent back since schedule complete with schedule size = " + newAnt.getSchedule().size());
				cApi.send(newAnt.getOriginator(), newAnt); //sending back to the truck who originated exp ant
				i.remove();
				continue;
			}
			if (station.makeBookingAt(iAnt.getCurrentUnit().getTimeSlot().getStartTime(), iAnt.getOriginator())!= null) { //means station is booked
				iAnt.getCurrentUnit().setPsReply(Reply.ACCEPT);
				logger.debug(station.getId() +"P Int ACCEPTED by PS");
			}
			else {
				iAnt.getCurrentUnit().setPsReply(Reply.REJECT);
				logger.debug(station.getId() +"P Int REJECT by PS");
			}
			IntAnt newAnt = iAnt.clone(this);
			cApi.send(iAnt.getCurrentUnit().getDelivery().getOrder(), newAnt);
			i.remove();
		}
	}

	private void processExplorationAnts(TimeLapse timeLapse) throws CloneNotSupportedException {
		if (explorationAnts.isEmpty()) 
			return;
		Iterator<ExpAnt> i = explorationAnts.iterator();
		while (i.hasNext()) { 
			ExpAnt exp = i.next();
			if (exp.isScheduleComplete()) { //send back to orginator
				ExpAnt newExp = (ExpAnt)exp.clone(this);
				logger.debug(station.getId() +"P Exp is being sent back since schedule complete with schedule size = " + newExp.getSchedule().size());
				cApi.send(newExp.getOriginator(), newExp); //sending back to the truck who originated exp ant
				i.remove();
				continue;
			}
			else if (exp.getSchedule().size() > 0) 
				if (exp.isReturnEarly()) { //send back to origninator according to probability 
					ExpAnt newExp = (ExpAnt)exp.clone(this);
					logger.debug(station.getId() +"P Exp is being sent back becaise probability true with schedule size = " + newExp.getSchedule().size());
					cApi.send(newExp.getOriginator(), newExp);
					i.remove();
					continue;
				}	
			logger.debug(station.getId() +"P Exp Originator is=" + exp.getOriginator().getId()+ " and expScheduleSize = " + exp.getSchedule().size());
			for (OrderAgentInitial or : interestedTime.keySet()) { //check all order pheromones..
				if(exp.isInterested(interestedTime.get(or), travelDistanceToOrder.get(or))) { //is exp intereseted in this particular order?
					logger.debug(station.getId() +"P exp is interested at " + exp.getCurrentUnit().getTimeSlot().getStartTime() +
							", order interested At= " + interestedTime.get(or) + "expScheduleSize = " + exp.getSchedule().size());//", travelDistance= " + travelDistanceToOrder.get(or));
					checkArgument(noOfExplorations.containsKey(or), true); 
					noOfExplorations.put(or, noOfExplorations.get(or)+1);
					exp.getCurrentUnit().getTimeSlot().setLocationAtStartTime(this.Location, this);
					ExpAnt newExp = (ExpAnt)exp.clone(this);
					cApi.send(or, newExp); //send exp clones to all orders(or) it is interested in
				}
			}
			i.remove(); // if exp is not further intereste it will automaticaly die..
		}
		
	}
	//TODO add test cases to confirm its impl
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
		logger.debug(station.getId() +"P pheromone table: \n" + this.pheromoneToString());
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
	}
	
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
//		if (messages.size() > 0)
//			logger.debug(station.getId() +"P received messages quantity = " + messages.size());
		for (Message m : messages) {
			if (m.getClass() == FeaAnt.class) {
				this.feasibilityAnts.add((FeaAnt)m);
				//logger.debug(station.getId() +"P received F-ants quantity = " + messages.size());
			}
			else if (m.getClass() == IntAnt.class) {
				this.intentionAnts.add((IntAnt)m);
				logger.debug(station.getId() +"P received IntAnts quantity = " + intentionAnts.size());
			}
			else if (m.getClass() == ExpAnt.class) {
				this.explorationAnts.add((ExpAnt)m);
				//logger.debug(station.getId() +"P received Exp-ants quantity = " + explorationAnts.size());
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
}
