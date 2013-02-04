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

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import shaz.rmc.core.Agent;
import shaz.rmc.core.Reply;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.Utility;

import shaz.rmc.core.TruckScheduleUnit;
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
	
	private DateTime lastExpReturnTime; //time at which last Exp returned to truck
	private DateTime timeForLastExpAnt; //time at which last Exp was sent by truck
	private DateTime timeForLastIntAnt; //time at which last Int was sent by truck	
//	private DateTime lastExpDelebrationTime; // time at which last time some exp were selected and explored
	
	private RoadModel roadModel;
	private PDPModel pdpModel;
	List<ProductionSiteInitial> sites;
	//ArrayList<TruckScheduleUnit> schedule;
	
	private static int totalDeliveryTruck = 0;
	private final int id;
	private final shaz.rmc.core.domain.Vehicle truck;
	private final DeliveryTruckInitialBelief b;
	private final DeliveryTruckInitialIntention i;
	private ExpAnt bestAnt;
	
	public DeliveryTruckInitial(Point randomPosition, Vehicle pTruck) {
		setCapacity(pTruck.getNormalVolume());
		
		randomPCSelector = new RandomDataImpl(); //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		b = new DeliveryTruckInitialBelief(this, new ArrayList<TruckScheduleUnit>());
		
		i = new DeliveryTruckInitialIntention(this, b);
		
		timeForLastExpAnt = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		timeForLastIntAnt = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		lastExpReturnTime = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		
		bestAnt = null;
		truck = pTruck;
		id = ++totalDeliveryTruck;
	}
	@Override
	protected void tickImpl(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());		
		deliberate(timeLapse.getStartTime());
		sendExpAnts(timeLapse.getStartTime());
		sendIntAnts(timeLapse.getStartTime());
		//acting on intentions
		if (!b.schedule.isEmpty()) {
			assert ((ProductionSiteInitial)(b.schedule.get(b.schedule.size()-1).getTimeSlot().getProductionSiteAtStartTime())).getStation() != null : truck.getId()+"T: The return location of Truck shouldn't be null";
			i.followSchedule(timeLapse);
		}
	}	
	private void deliberate(long startTime) {
		if (//b.explorationAnts.size()>=2 && 
				//currTime.minusMinutes(timeForLastExpAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.EXPLORATION_INTERVAL_MIN )  {
				b.explorationAnts.size()> 3) {
			boolean bestAntChanged = false; //only to track if it should be printed or not
			for (ExpAnt eAnt: b.explorationAnts) { //find eAnt with smallest score, i.e least cost
				if (b.scheduleStillValid(b.schedule, eAnt.getSchedule())){				
					if (b.schedule.size() == 0) {//select the one with highest no. of units
						if (bestAnt == null || (eAnt.getSchedule().size() > bestAnt.getSchedule().size())) {
							bestAnt = eAnt;
							bestAntChanged = true;
						}
					}
					else { //decide based on lowest schedule score
						if (bestAnt != null) {
							if (eAnt.getScheduleScore() < bestAnt.getScheduleScore() ) {
								bestAnt = eAnt;
								bestAntChanged = true;
							}
						}
					}
				}
			}
			if (bestAntChanged)
				printBestAnt(startTime);
			b.explorationAnts.clear(); 
		}	
		// Checking Intention Ants
		if (!b.intentionAnts.isEmpty() && b.intentionAnts.size() > 1) { //TODO Have to change this condition. Actual should be that intention ants are still checked, 
			//if some thing interesting found that sould be added in the trucks schedule
			boolean scheduleDone = false;
			Iterator<IntAnt> i = b.intentionAnts.iterator();
			while (i.hasNext()) { //at the moment just select the first one
				IntAnt iAnt = i.next();
				if (iAnt.isConsiderable(b.schedule)) 
				{
					boolean scheduleAcceptable = false;
					for (TruckScheduleUnit u : iAnt.getSchedule()){
						scheduleAcceptable = false;
						if (!alreadyExist(u) ) {
							if (!isOverlapped(u)) {
								checkArgument(u.getDelivery().getDeliveryTime().minus(u.getDelivery().getStationToCYTravelTime()).minusMinutes(GlobalParameters.LOADING_MINUTES).isEqual(u.getTimeSlot().getStartTime()), true);
								checkArgument(u.getTimeSlot().getEndTime().compareTo(b.getTotalTimeRange().getEndTime()) <= 0 , true);
								checkArgument(u.getTimeSlot().getStartTime().compareTo(b.getTotalTimeRange().getStartTime()) >= 0 , true);
								scheduleAcceptable = true;
							}
						}
						else { //means already exists
							
						}
						if (!scheduleAcceptable)
							break;
					}
					if (scheduleAcceptable) {
						for (TruckScheduleUnit u : iAnt.getSchedule()){ 
							b.schedule.add(u);
							scheduleDone = true;
							logger.debug(this.getId()+"T Schedule unit added in Trucks schedule: " + u.getSummary());
						}
					}
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
		if (currTime.minusMinutes(timeForLastIntAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.INTENTION_INTERVAL_MIN ) {
			if (bestAnt != null) {
				if (b.scheduleStillValid(b.schedule, bestAnt.getSchedule())){
					adjustFixedCapacity(bestAnt.getSchedule());
					IntAnt iAnt = new IntAnt(this, bestAnt.getSchedule(), currTime);
					logger.debug(this.getId()+"T int sent by Truck");
					checkArgument(bestAnt.getSchedule().get(0).getTimeSlot().getProductionSiteAtStartTime() != null, true);
					cApi.send(bestAnt.getSchedule().get(0).getTimeSlot().getProductionSiteAtStartTime(), iAnt); 
					timeForLastIntAnt = currTime;
					bestAnt = null;
					return;
				}
			}
			if (b.schedule.size()> 0 && timeForLastIntAnt.equals(currTime) == false){//send old schedule to refresh bookings..
				adjustFixedCapacity(b.schedule);
				IntAnt iAnt = new IntAnt(this, b.schedule, currTime);
				logger.debug(this.getId()+"T int sent by Truck with Old schedule");
				checkArgument(b.schedule.get(0).getTimeSlot().getProductionSiteAtStartTime() != null, true);
				cApi.send(b.schedule.get(0).getTimeSlot().getProductionSiteAtStartTime(), iAnt); 
				timeForLastIntAnt = currTime; //here no need to make bestAnt = null, since it coud compete with future explorations
			}
		}
	}
	private void adjustFixedCapacity(ArrayList<TruckScheduleUnit> schedule) {
		if (schedule.isEmpty())
			return;
		for (TruckScheduleUnit u : schedule) {
			if (u.getOrderReply() != Reply.NO_REPLY){
				checkArgument(u.getPsReply() != Reply.REJECT, true);
				checkArgument(u.getOrderReply() != Reply.REJECT, true);
				checkArgument (u.getPsReply() == u.getOrderReply(), true);
				u.setFixedCapacityAmount(0);
			}
		}		
	}
	private void printBestAnt(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		logger.debug(this.getId()+"T Best schedule changed with total Units = " + bestAnt.getSchedule().size() + "and Score = " + bestAnt.getScheduleScore() +" & total ants="+ b.explorationAnts.size() + "currTime= " + currTime);
		for (TruckScheduleUnit unit: bestAnt.getSchedule()) {
			System.out.println(unit.getSummary());
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
			if (u.getDelivery().equalsWithSameTruck(un.getDelivery())) //is it oke, or check further details inside delivery?
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
					this.lastExpReturnTime = new DateTime (GlobalParameters.START_DATETIME.getMillis() + currentTime);
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
		return 4;
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
				lagTimeInMin += u.getDelivery().getLagTime().getStandardMinutes();
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
