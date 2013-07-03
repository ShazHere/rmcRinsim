/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

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
import shaz.rmc.pdpExtended.delMasInitial.communication.BreakAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.CommitmentAnt;
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
	private final RandomGenerator randomPCSelector;
	
	private DateTime timeForLastExpAnt; //time at which last Exp was sent by truck
	private DateTime timeForLastIntAnt; //time at which last Int was sent by truck	
	
	private RoadModel roadModel;
	private PDPModel pdpModel;
	List<ProductionSiteInitial> sites;
	private final Map<Delivery, Reply> unitStatus;
	
	private static int totalDeliveryTruck = 0;
	private final int id;
	private final int dePhaseByMin;
	private final shaz.rmc.core.domain.Vehicle truck;
	private final DeliveryTruckInitialBelief b;
	private final DeliveryTruckInitialIntention i;
	private ExpAnt bestAnt;
	private TRUCK_STATE state;
	//private boolean truckBroke;
	
	public DeliveryTruckInitial( Vehicle pTruck, int pDePhaseByMin, RandomGenerator pRandomPCSelector) {
		setCapacity(pTruck.getNormalVolume());
		dePhaseByMin = pDePhaseByMin;
		//System.out.println("Dephase no. is " + dePhaseByMin);
		randomPCSelector = pRandomPCSelector; //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		unitStatus = new LinkedHashMap<Delivery, Reply>();
		b = new DeliveryTruckInitialBelief(this, new ArrayList<TruckScheduleUnit>());
		
		i = new DeliveryTruckInitialIntention(this, b);
		
		timeForLastExpAnt = new DateTime(b.getTotalTimeRange().getStartTime().plusMinutes(dePhaseByMin));
		timeForLastIntAnt = new DateTime(b.getTotalTimeRange().getStartTime().plusMinutes(dePhaseByMin).plusMinutes(GlobalParameters.INTENTION_INTERVAL_MIN));
		
		bestAnt = null;
		truck = pTruck;
		id = ++totalDeliveryTruck;
		state = TRUCK_STATE.IN_PROCESS;
		//truckBroke = false;  //for tracking if truck is broken or not
	}
	@Override
	protected void tickImpl(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());		
		if (this.state == TRUCK_STATE.BROKEN)
			return;
		processIntentionAnts(timeLapse.getStartTime());
		sendExpAnts(timeLapse.getStartTime());
		sendIntAnts(timeLapse.getStartTime());
		//acting on intentions
		if (!b.schedule.isEmpty()) {
			assert ((ProductionSiteInitial)(b.schedule.get(b.schedule.size()-1).getTimeSlot().getProductionSiteAtStartTime())).getStation() != null : truck.getId()+"T: The return location of Truck shouldn't be null";
			i.followSchedule(timeLapse);
		}
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN)
			sendBreakDownEvent(timeLapse.getStartTime());
		if (GlobalParameters.ENABLE_JI)
			processCommitmentAnts(timeLapse.getStartTime());
	}
	/**
	 * Used to send the break Down evet
	 * @param currTime Time at which breakDown event message would be sent.
	 */ //already tested..
	private void sendBreakDownEvent(long startTime) {
		
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (currTime.compareTo(b.getTotalTimeRange().getStartTime().plusHours(4)) != 0 )
			//(currTime.compareTo(b.getTotalTimeRange().getStartTime().plusHours(4)) < 0 )
			//	&& currTime.compareTo(b.getTotalTimeRange().getStartTime().plusHours(4).plusMinutes(1)) < 0)) //if X hours are not passed, then simply return
			return;
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN == false || this.state == TRUCK_STATE.BROKEN 
				||b.schedule.isEmpty())
			return;
		//if (this.id >2 )//!= 1 && this.id != 2)
		
		//means X hours are passed
		
		//for (TruckScheduleUnit tu : b.schedule) {
		Iterator<TruckScheduleUnit> j = b.schedule.iterator();
		while (j.hasNext()) {
			TruckScheduleUnit tu = j.next();
			if ((((OrderAgentInitial)tu.getDelivery().getOrder()).getOrder().getId().equals("4")
					&& tu.getDelivery().getDeliveryNo() == 10) //if 40O del2 or
					|| (((OrderAgentInitial)tu.getDelivery().getOrder()).getOrder().getId().equals("30")//or 30O del3
							&& tu.getDelivery().getDeliveryNo() == 5 )) {
				if (this.unitStatus.get(tu.getDelivery()) == Reply.ACCEPT) { //means JI team is formed
					BreakAnt bAnt = new BreakAnt(this, currTime, tu.getDelivery());
					logger.info(this.getId()+"T Sending BREAKDOWN signal to " + ((OrderAgentInitial)tu.getDelivery().getOrder()).getOrder().getId() + "O for delivery No " + tu.getDelivery().getDeliveryNo());
					cApi.send(tu.getDelivery().getOrder(), bAnt);
					this.state = TRUCK_STATE.BROKEN;
					this.unitStatus.remove(tu);
					b.schedule.remove(tu);
					break;
				}
			}
		}
		
	}
	private void processExplorationAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (b.explorationAnts.isEmpty())
			return;
	
		//first remove invalid schedules, and get whats the maxScore size
		int maxSizeFound = 0;
		Iterator<ExpAnt> i = b.explorationAnts.iterator();
		while (i.hasNext()) { 
			ExpAnt eAnt = i.next();
			if (b.scheduleStillValid(b.schedule,  eAnt.getSchedule()) == false)
				i.remove();
			else if (eAnt.getSchedule().size() > maxSizeFound)
				maxSizeFound = eAnt.getSchedule().size();
		}
		if (b.explorationAnts.isEmpty())
			return;
		//prune the one with lesser schedule size
		Iterator<ExpAnt> j = b.explorationAnts.iterator();
		while (j.hasNext()) { 
			ExpAnt eAnt = j.next();
			if (eAnt.getSchedule().size() < maxSizeFound)
				j.remove();
		}
		checkArgument(b.explorationAnts.isEmpty() == false, true);
		
		//handle the selection based on schedule score
		if (GlobalParameters.EXP_RANKING_WITH_SCORE_ENABLE) {
			//select 1st schedule as best
			bestAnt = b.explorationAnts.get(0);
			for (ExpAnt eAnt: b.explorationAnts) { //find eAnt with smallest score, i.e least cost
				//if (b.scheduleStillValid(b.schedule, eAnt.getSchedule())){				
					if (eAnt.getScheduleScore()< bestAnt.getScheduleScore()) {
							bestAnt = eAnt;
						}
				//}
			}
		}
		else {
			bestAnt = b.explorationAnts.get(randomPCSelector.nextInt(b.explorationAnts.size()));
//			if (GlobalParameters.LAG_TIME_ENABLE) { //actually this isn't required, but here its only when we require to test lagTime
//				Collections.sort(b.explorationAnts, new Comparator<ExpAnt>(){ //sort w.r.t descending scheduleLagTime
//			        public int compare( ExpAnt a, ExpAnt b ){
//			            return (int)(b.getScheduleLagTime().minus(a.getScheduleLagTime()).getStandardSeconds());
//			        }
//				});
//			}
//			bestAnt = b.explorationAnts.get(0);
			
		}
		printBestAnt(startTime);
		b.explorationAnts.clear(); 
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
							unitStatus.put(tu.getDelivery(), u.getOrderReply());
							logger.info(this.getId()+"T Schedule unit added in Trucks schedule (status= " +u.getOrderReply()+ ": " + u.getTunit().getSummary());
						}
						else//the unit already exists in the truck's schedule, check its status
						{
							unitStatus.put(u.getTunit().getDelivery(), u.getOrderReply()); //update status, either it could be WEEK_ACCEPT, or STRONG_ACCEPT
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
			if (b.availableSlots.size()>0) {
				checkArgument(b.availableSlots.get(0).getProductionSiteAtStartTime() != null, true);
				cApi.send(b.availableSlots.get(0).getProductionSiteAtStartTime(), eAnt); 				
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
		logger.debug(this.getId()+"T Best schedule changed: total Units= " + bestAnt.getSchedule().size() +" , lagTime= "+ bestAnt.getScheduleLagTime()+ " and Score= " + bestAnt.getScheduleScore() +" & total ants="+ b.explorationAnts.size() + "currTime= " + currTime);
		for (TruckScheduleUnit unit: bestAnt.getSchedule()) {
			logger.debug(unit.getSummary());
		}
	}
	/**
	 * to check if the current truck unit overlaps with any unit already existing in the truck's schedule
	 * @param un The schedule that need to be checked with existing schedule of truck
	 * @return true if there is overlap with schedule, false if there is no overlap.
	 */ //TODO add test cases to test  TODO a lot of cases are yet not checked
	private boolean isOverlapped(TruckScheduleUnit un) {
		for (TruckScheduleUnit u : b.schedule) {
			if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getStartTime()) >= 0) {
				if (un.getTimeSlot().getStartTime().compareTo(u.getTimeSlot().getEndTime()) <= 0)
					return true; //if overlap with any unit, then return true
			}
			else { //means startTime is less
				if (un.getTimeSlot().getEndTime().compareTo(u.getTimeSlot().getStartTime()) >= 0)
					return true; //overlap exists so return true
			}
		}
		return false; //found no overlap with any unit so return false
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
	private void processCommitmentAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (b.commitmentAnts.isEmpty() )
			return;
		Iterator<CommitmentAnt> i = b.commitmentAnts.iterator();
		//checkArgument(b.commitmentAnts.size() == 0, true); // at the moment truck shud receive only one commitment ant
		
		while (i.hasNext()) { 
			CommitmentAnt cAnt = i.next();
			Utility.getAvailableSlots(b.schedule, b.availableSlots, //recent available slots 
					new TimeSlot(new DateTime(currTime), b.getTotalTimeRange().getEndTime()));
			Delivery d = cAnt.getFailedDelivery();
			DateTime actualTime = d.getDeliveryTime().minusMinutes(GlobalParameters.LOADING_MINUTES);
			for (TimeSlot t :b.availableSlots) {
				//if (cAnt.getPossibleSites().contains(t.getProductionSiteAtStartTime())) {//means delivery could be feasible w.r.t PS
				if (cAnt.getFailedDelivery().getLoadingStation().equals(t.getProductionSiteAtStartTime())) {//means delivery could be feasible w.r.t PS
					Duration StToCy = new Duration ((long)(Point.distance(t.getProductionSiteAtStartTime().getPosition(), cAnt.getOriginator().getPosition())));
					if (t.getStartTime().compareTo(actualTime.minus(StToCy))<0  //30 min since not sure which ps the truck will return
							&& t.getEndTime().compareTo(d.getDeliveryTime().plus(StToCy).plus(d.getUnloadingDuration()).plusMinutes(10)) >0) {
						//means time slot is oK and loading PS is also ok;
						cAnt.setTruckReply(Reply.UNDER_PROCESS);
						break;
					}
				}
			}
			if (cAnt.getTruckReply() == Reply.NO_REPLY)
				cAnt.setTruckReply(Reply.REJECT);
			logger.info(this.getId() + "T com-" + cAnt.getOriginator().getOrder().getId()+" reply is " + cAnt.getTruckReply() + " currTime= " + currTime);
			CommitmentAnt newAnt = cAnt.clone(this);
			cApi.send(cAnt.getOriginator(), newAnt);
			i.remove();
		}
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
				else if (m.getClass() == CommitmentAnt.class) {
					b.commitmentAnts.add((CommitmentAnt)m);
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
		int rand = randomPCSelector.nextInt(sites.size());
		//in the begining truck is at start location
		b.availableSlots.get(0).setLocationAtStartTime(sites.get(rand).getLocation(), sites.get(rand)); //setting start location
		b.setStartLocation( sites.get(rand).getLocation());
		roadModel.addObjectAt(this, b.getStartLocation());
		logger.info(this.getId()+"T capacity is = " + this.truck.getNormalVolume() + " & startLocation = " + sites.get(rand).getStation().getId());
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
	
	private enum TRUCK_STATE {
		IN_PROCESS, // The normal and general state
		BROKEN,
		TEAM_NEED, //Truck is in a transition state w.r.t team. shouldn't send exp or int ants, neither process them. 
	}

}
