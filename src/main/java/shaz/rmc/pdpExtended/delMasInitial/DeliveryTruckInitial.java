/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import com.rits.cloning.Cloner;

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
import shaz.rmc.core.Reply;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;

import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class DeliveryTruckInitial extends rinde.sim.core.model.pdp.Vehicle implements MovingRoadUser, Agent {
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	private static Logger logger = Logger.getLogger(DeliveryTruckInitial.class);
	private final RandomData randomPCSelector;
	
	private DateTime timeForLastExpAnt; //time at which last Exp was sent by truck
	private DateTime timeForLastIntAnt; //time at which last Int was sent by truck	
	
	private RoadModel roadModel;
	private PDPModel pdpModel;
	List<ProductionSiteInitial> sites;
	
	private static int totalDeliveryTruck = 0;
	private final int id;
	private final int dePhaseByMin;
	private final shaz.rmc.core.domain.Vehicle truck;
	private final DeliveryTruckInitialBelief b;
	private final DeliveryTruckInitialIntention i;
	private ExpAnt bestAnt;
	
	public DeliveryTruckInitial(Point randomPosition, Vehicle pTruck, int pDePhaseByMin) {
		setCapacity(pTruck.getNormalVolume());
		dePhaseByMin = pDePhaseByMin;
		//System.out.println("Dephase no. is " + dePhaseByMin);
		randomPCSelector = new RandomDataImpl(); //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		b = new DeliveryTruckInitialBelief(this, new ArrayList<TruckScheduleUnit>());
		
		i = new DeliveryTruckInitialIntention(this, b);
		
		timeForLastExpAnt = new DateTime(b.getTotalTimeRange().getStartTime().plusMinutes(dePhaseByMin));
		timeForLastIntAnt = new DateTime(b.getTotalTimeRange().getStartTime().plusMinutes(dePhaseByMin).plusMinutes(GlobalParameters.INTENTION_INTERVAL_MIN));
		
		bestAnt = null;
		truck = pTruck;
		id = ++totalDeliveryTruck;
		logger.info(this.getId()+"T capacity is = " + pTruck.getNormalVolume());
	}
	@Override
	protected void tickImpl(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());		
		processIntentionAnts(timeLapse.getStartTime());
		sendExpAnts(timeLapse.getStartTime());
		sendIntAnts(timeLapse.getStartTime());
		//acting on intentions
		if (!b.schedule.isEmpty()) {
			assert ((ProductionSiteInitial)(b.schedule.get(b.schedule.size()-1).getTimeSlot().getProductionSiteAtStartTime())).getStation() != null : truck.getId()+"T: The return location of Truck shouldn't be null";
			i.followSchedule(timeLapse);
		}
	}
	private void processExplorationAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if 	(b.explorationAnts.size()> 0) {
			bestAnt = new ExpAnt(this, Utility.getAvailableSlots(b.schedule, b.availableSlots, 
					new TimeSlot(new DateTime(currTime), b.getTotalTimeRange().getEndTime())), b.schedule, currTime);
			for (ExpAnt eAnt: b.explorationAnts) { //find eAnt with smallest score, i.e least cost
				if (b.scheduleStillValid(b.schedule, eAnt.getSchedule())){				
					if (eAnt.getSchedule().size() > bestAnt.getSchedule().size()) { //select the with highest units..but there shud be further selection if there is a tie
							bestAnt = eAnt;
						}
				}
			}
			printBestAnt(startTime);
			b.explorationAnts.clear(); 
		}	
	}
	private void processIntentionAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (!b.intentionAnts.isEmpty() ) { 
			boolean scheduleDone = false;
			Iterator<IntAnt> i = b.intentionAnts.iterator();
			while (i.hasNext()) { //at the moment just select the first one
				IntAnt iAnt = i.next();
				if (iAnt.isConsiderable(b.schedule)) 
				{
					//logger.debug(this.getId()+"T Intention returned & considerable " + " currTime= " + currTime);
					final Cloner cl = Utility.getCloner();
					for (communicateAbleUnit u : iAnt.getSchedule()){
						if (u.isAddedInTruckSchedule() == false) {
							checkArgument(isOverlapped(u.getTunit()) == false, true);
							checkArgument(u.getTunit().getDelivery().getDeliveryTime().minus(u.getTunit().getDelivery().getStationToCYTravelTime()).minusMinutes(GlobalParameters.LOADING_MINUTES).isEqual(u.getTunit().getTimeSlot().getStartTime()), true);
							checkArgument(u.getTunit().getTimeSlot().getEndTime().compareTo(b.getTotalTimeRange().getEndTime()) <= 0 , true);
							checkArgument(u.getTunit().getTimeSlot().getStartTime().compareTo(b.getTotalTimeRange().getStartTime()) >= 0 , true);
							TruckScheduleUnit tu = cl.deepClone(u.getTunit());
							checkArgument(this.alreadyExist(tu) == false, true);
							b.schedule.add(tu);
							logger.info(this.getId()+"T Schedule unit added in Trucks schedule: " + u.getTunit().getSummary());
						}
					}
					scheduleDone = true;
				} //no need of else,coz it will be removed any way..
				i.remove();
				if (scheduleDone)
					break;
			}
			b.intentionAnts.clear();  //remove all remaining ants if any..
		}
	}
	
	private void sendExpAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		//check  exp ants to be sent after particular interval only
		if (currTime.minusMinutes(timeForLastExpAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.EXPLORATION_INTERVAL_MIN ) {
			ExpAnt eAnt = new ExpAnt(this, Utility.getAvailableSlots(b.schedule, b.availableSlots, 
					new TimeSlot(new DateTime(currTime), b.getTotalTimeRange().getEndTime())), b.schedule, currTime);
			if (b.getAvailableSlots().size()>0) {
				checkArgument(b.getAvailableSlots().get(0).getProductionSiteAtStartTime() != null, true);
				cApi.send(b.getAvailableSlots().get(0).getProductionSiteAtStartTime(), eAnt); 				
			}
			timeForLastExpAnt = currTime;
		}		
	}
	private void sendIntAnts(long startTime) { 
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);//send int ants to book again the whole schedule..
		if (currTime.compareTo(timeForLastIntAnt) > 0) {
		if (currTime.minusMinutes(timeForLastIntAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.INTENTION_INTERVAL_MIN ) {
			processExplorationAnts(startTime);
			if (bestAnt != null) {
				if (b.scheduleStillValid(b.schedule, bestAnt.getSchedule())){
					ArrayList<communicateAbleUnit> tmp = makeCommunicateAbleSchedule(bestAnt.getSchedule());
					IntAnt iAnt = new IntAnt(this, tmp, currTime);
					logger.debug(this.getId()+"T int sent by Truck");
					checkArgument(bestAnt.getSchedule().get(0).getTimeSlot().getProductionSiteAtStartTime() != null, true); 
					cApi.send(bestAnt.getSchedule().get(0).getTimeSlot().getProductionSiteAtStartTime(), iAnt); 
					timeForLastIntAnt = currTime;
					bestAnt = null;
					return;
				} 
			}
			if (b.schedule.size()> 0 && timeForLastIntAnt.equals(currTime) == false){//send old schedule to refresh bookings..
				ArrayList<communicateAbleUnit> tmp = makeCommunicateAbleSchedule(b.schedule);
				IntAnt iAnt = new IntAnt(this, tmp, currTime);
				logger.debug(this.getId()+"T int sent by Truck with Old schedule");
				checkArgument(b.schedule.get(0).getTimeSlot().getProductionSiteAtStartTime() != null, true);
				cApi.send(b.schedule.get(0).getTimeSlot().getProductionSiteAtStartTime(), iAnt); 
				timeForLastIntAnt = currTime; //here no need to make bestAnt = null, since it coud compete with future explorations
			}
		}
		}
	}

	private ArrayList<communicateAbleUnit> makeCommunicateAbleSchedule(ArrayList<TruckScheduleUnit> schedule) {
		checkArgument(schedule.isEmpty() == false, true);
		ArrayList<communicateAbleUnit> communicateAbleSchedule = new ArrayList<communicateAbleUnit>();
		final Cloner cl = Utility.getCloner();
		for (TruckScheduleUnit u : schedule) {
			TruckScheduleUnit newUnit = cl.deepClone(u);
			communicateAbleUnit cUnit = new communicateAbleUnit(newUnit, Reply.NO_REPLY, Reply.NO_REPLY, false);
			if (alreadyExist(u) == true)
				cUnit.setAddedInTruckSchedule(true);
			communicateAbleSchedule.add(cUnit);
		}
		checkArgument (communicateAbleSchedule.isEmpty() == false, true);
		return communicateAbleSchedule;
	}
	private void printBestAnt(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		logger.debug(this.getId()+"T Best schedule changed with total Units = " + bestAnt.getSchedule().size() + "and Score = " + bestAnt.getScheduleScore() +" & total ants="+ b.explorationAnts.size() + "currTime= " + currTime);
		for (TruckScheduleUnit unit: bestAnt.getSchedule()) {
			logger.debug(unit.getSummary());
		}
	}
	/**
	 * to check if the current truck unit overlaps with any unit already existing in the truck's schedule
	 * @return
	 */ //TODO add test cases to test  TODO a lot of cases are yet not checked
	private boolean isOverlapped(TruckScheduleUnit un) {
		for (TruckScheduleUnit u : b.schedule) {
			if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getStartTime()) >= 0) {
				if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getEndTime()) <= 0)
					return true; //if overlap with any unit, then return false
			}
			else { //means startTime is less
				if (un.getTimeSlot().getEndTime().compareTo(u.getTimeSlot().getStartTime()) >= 0)
					return true;
			}
		}
		return false;
	}
	/**
	 * The schedule unit already exists in trucks schedule?
	 * @param un
	 * @return
	 */	//TODO add test cases to test
	private boolean alreadyExist(TruckScheduleUnit un) {
		for (TruckScheduleUnit u : b.schedule) { 
			if (u.getDelivery().equals(un.getDelivery())) //is it oke, or check further details inside delivery?
				return true;
		}
		return false;
	}
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0) {
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					b.explorationAnts.add((ExpAnt)m);
				}
				else if (m.getClass() == IntAnt.class) {
					b.intentionAnts.add((IntAnt)m);
				}
			}
		}
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;	
		// setting startLocation 
		sites = new ArrayList<ProductionSiteInitial>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
		int rand = randomPCSelector.nextInt(0, sites.size()-1);
		//in the begining truck is at start location
		b.getAvailableSlots().get(0).setLocationAtStartTime(sites.get(rand).getLocation(), sites.get(rand)); //setting start location
		b.setStartLocation( sites.get(rand).getLocation());
		roadModel.addObjectAt(this, b.getStartLocation());
	}
	public shaz.rmc.core.domain.Vehicle getTruck() {
		return truck;
	}

	public int getId() {
		return id;
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
	public TimeSlot getTotalTimeRange() {
		return b.getTotalTimeRange();
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
		 * score concerns: lagTime (20), travelDistance(20), ST Delay (10), wasted Concrete (10), preferred station (1)
		 */
		int travelMin = 0;
		int lagTimeInMin = 0;
		int startTimeDelay = 0; //already included in lag time
		int wastedConcrete = 0;
		int deliveredConcrete =0;
		if (!b.schedule.isEmpty()) {
			for(TruckScheduleUnit u: b.schedule) {
				travelMin += u.getDelivery().getStationToCYTravelTime().getStandardMinutes();
				travelMin += u.getDelivery().getCYToStationTravelTime().getStandardMinutes();
				if (u.getDelivery().getDeliveryNo() == 0)
					startTimeDelay += (int)Math.ceil((double)u.getDelivery().getLagTime().getStandardSeconds()/60d); //since for first delivery delay means lag time delay..
				else
					lagTimeInMin += (int)Math.ceil((double)u.getDelivery().getLagTime().getStandardSeconds()/60d);
				wastedConcrete += u.getDelivery().getWastedVolume();
				deliveredConcrete += u.getDelivery().getDeliveredVolume(); //truck used to deliver all concrete, even the one that will go wastage for order site. so unloading times would also have boht values
			}
		ResultElementsTruck re = new ResultElementsTruck(b.schedule.size(), travelMin, lagTimeInMin, startTimeDelay, wastedConcrete,deliveredConcrete);
			return re; 
		}
		else 
			return null;
	}

}
