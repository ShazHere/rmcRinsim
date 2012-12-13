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
import shaz.rmc.core.Reply;
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
	private boolean intAntSent;
	
	public DeliveryTruckInitial(Point randomPosition, Vehicle pTruck) {
		setCapacity(pTruck.getNormalVolume());
		
		randomPCSelector = new RandomDataImpl(); //this won't generate the exact random no. required by us..:(.
		mailbox = new Mailbox();
		b = new DeliveryTruckInitialBelief(this, new ArrayList<TruckScheduleUnit>());
		
		i = new DeliveryTruckInitialIntention(this, b);
		
		timeForLastExpAnt = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		timeForLastIntAnt = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		lastExpReturnTime = new DateTime(b.getTotalTimeRange().getLocationAtStartTime());
		
		intAntSent = false;
		bestAnt = null;
		truck = pTruck;
		id = ++totalDeliveryTruck;
	}
	@Override
	protected void tickImpl(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());
		sendExpAnts(timeLapse.getStartTime());
		
		deliberate(timeLapse.getStartTime());
		sendIntAnts(timeLapse.getStartTime());
		//acting on intentions
		if (!b.schedule.isEmpty()) {
			assert ((ProductionSiteInitial)(b.schedule.get(b.schedule.size()-1).getTimeSlot().getProductionSiteAtStartTime())).getStation() != null : truck.getId()+"T: The return location of Truck shouldn't be null";
			//TODO  if last unit is activated then change slot..15/05/2012
			//arrangeSlots(schedule.get(schedule.size()-1).getTimeSlot()); //so the end time of last schedule unit is the next available slot..
			i.followSchedule(timeLapse);
		}
				
	}
	
	private void deliberate(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (//b.explorationAnts.size()>=2 && 
				//currTime.minusMinutes(timeForLastExpAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.EXPLORATION_INTERVAL_MIN )  {
				b.explorationAnts.size()> 1) {
			
			//logger.debug(this.id + "T Investigating exp ants and total are: " +b.explorationAnts.size());
			bestAnt = b.explorationAnts.get(0);
			for (ExpAnt eAnt: b.explorationAnts) { //find eAnt with smallest score, i.e least cost
				if (eAnt.getScheduleScore() < bestAnt.getScheduleScore())
					bestAnt = eAnt;
			}
			logger.debug("Best schedule with total Units = " + bestAnt.getSchedule().size() + "and Score = " + bestAnt.getScheduleScore() +" & total ants="+ b.explorationAnts.size());
//			for (TruckScheduleUnit unit: bestAnt.getSchedule()) {
//				System.out.println(unit.getSummary());
//			}
			b.explorationAnts.clear(); //just for checking
		}
		
		// Checking Intention Ants
		if (!b.intentionAnts.isEmpty() && b.schedule.isEmpty()) { //TODO Have to change this condition. Actual should be that intention ants are still checked, 
			//if some thing interesting found that sould be added in the trucks schedule
			boolean scheduleDone = true;
			Iterator<IntAnt> i = b.intentionAnts.iterator();
			while (i.hasNext()) { //at the moment just select the first one
				IntAnt iAnt = i.next();
				for (TruckScheduleUnit u :iAnt.getSchedule()) {
					if (u.getPsReply() == Reply.REJECT || u.getOrderReply() == Reply.REJECT){
						scheduleDone = false;
						logger.debug(this.getId()+"T NOT done unitStartTime = " + u.getTimeSlot().getStartTime());
						logger.debug(this.getId()+"T Int Replies are <PS, OR> : < " +u.getPsReply() + ", " + u.getOrderReply() + " >" );
						break;
					}	
				}
				if (scheduleDone)
					b.schedule = iAnt.getSchedule(); 
				i.remove();
			}
			if (scheduleDone == true) {
				logger.debug(this.getId()+"T schedule done & details are  below:");
				for (TruckScheduleUnit u : b.schedule) {
					logger.debug(u.getSummary());
				}	
			}
		}
		/*
		 * check if size of returned expList is > 3 then start checking them
		 * {
		 * add method of score calculation within expAnt
		 * select the best schedule based on score
		 * make intended schedule (list), send booking intention ants to orders whom deliveries are intended
		 * remove exp ant
		 * 
		 * } 
		 * 
		 * check about size of intentionAnt list >0 then start checking them
		 * { //are all bookings in the schedule done?
		 * if order.delivery is weekAccepted then put it in actual schedule, there shoud b some sense of delivery id, 
		 * so that order and truck boht can communicate	about it in future
		 * } 
		 * 
		 * 
		 */
		
	}
	private void sendExpAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		//check  exp ants to be sent after particular interval only
		if (currTime.minusMinutes(timeForLastExpAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.EXPLORATION_INTERVAL_MIN ) {
			int probabilityToReturnEarly = 8;//1; default 1
			if (new DateTime(startTime + GlobalParameters.START_DATETIME.getMillis()).minusMinutes(15).compareTo(lastExpReturnTime) >= 0)
				probabilityToReturnEarly = 10;
			else if (new DateTime(startTime + GlobalParameters.START_DATETIME.getMillis()).minusMinutes(10).compareTo(lastExpReturnTime) >= 0)
				probabilityToReturnEarly = 5;
			ExpAnt eAnt = new ExpAnt(this, Utility.getAvailableSlots(b.schedule, b.availableSlots, 
					new TimeSlot(new DateTime(currTime), b.getTotalTimeRange().getEndTime())), b.schedule, currTime, probabilityToReturnEarly);
			//logger.debug(this.getId()+"T Exp sent by Truck with probabilityToReturnEarly = " + probabilityToReturnEarly);
			//Utility.getAvailableSlots(this.b.schedule, b.availableSlots, new TimeSlot(new DateTime(currTime), b.getTotalTimeRange().getEndTime()));
			if (b.getAvailableSlots().size()>0) {
				checkArgument(b.getAvailableSlots().get(0).getProductionSiteAtStartTime() != null, true);
				cApi.send(b.getAvailableSlots().get(0).getProductionSiteAtStartTime(), eAnt); 				
			}
			timeForLastExpAnt = currTime;
		}		
	}
	
	private void sendIntAnts(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		//if (bestAnt != null && intAntSent == false) {
		if (bestAnt != null && currTime.minusMinutes(timeForLastIntAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.INTENTION_INTERVAL_MIN ) {
			//DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
			IntAnt iAnt = new IntAnt(this, bestAnt.getSchedule(), currTime);
			logger.debug(this.getId()+"T int sent by Truck");
			checkArgument(bestAnt.getCurrentUnit().getTimeSlot().getProductionSiteAtStartTime() != null, true);
			cApi.send(bestAnt.getCurrentUnit().getTimeSlot().getProductionSiteAtStartTime(), iAnt); 
			//intAntSent = true; //so that at the moment intenttions are sent only for once.
			timeForLastIntAnt = currTime;
		}
				
	}
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0) {
			//logger.debug(this.getId()+"T messages received are = " + messages.size());
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					b.explorationAnts.add((ExpAnt)m);
					//logger.debug(this.getId()+"T received exp ant with schedule size = " + ((ExpAnt)m).getSchedule().size());
					this.lastExpReturnTime = new DateTime (GlobalParameters.START_DATETIME.getMillis() + currentTime);
				}
				else if (m.getClass() == IntAnt.class) {
					b.intentionAnts.add((IntAnt)m);
					//logger.debug(this.getId()+"T received int ant quantity = " + b.intentionAnts.size());
				}
				else ; 
					//logger.debug(this.getId()+"T message class is " + m.getClass() ); //Do nothing
				
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
		//setStartPosition(b.startLocation);
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
	//@Override
	//public int getCapacity() {}

	public RoadModel getRoadModel() {
		return roadModel;
	}

	public PDPModel getPdpModel() {
		return pdpModel;
	}
	public TimeSlot getTotalTimeRange() {
		return b.getTotalTimeRange();
	}

}
