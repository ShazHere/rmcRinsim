/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;

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
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.road.RoadModel;
import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Order;
import shaz.rmc.pdpExtended.delMasInitial.communication.FeaAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;


/**
 * @author Shaza
 *
 */
public class OrderAgentInitial  extends Depot implements Agent {
	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	private final Simulator sim;
	private final Point Location;
	private final Order order;
	private ArrayList<Delivery> deliveries; //virtual deliveries..
	
	private final Logger logger; //for logging
	protected ArrayList<ExpAnt> explorationAnts;
	protected ArrayList<IntAnt> intentionAnts;
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	private DateTime timeForLastFeaAnt; /** to keep track of feasibility interval */
	
	//REalted to current interest
	private DateTime interestedTime;  //time sent by FeaAnts, to indicate this is the time at which order is interested
	private Duration delayFromActualInterestedTime; //to keep recored of delay before interestedTime, for first delivery it should be zero
	private Duration delayStartTime; //delay in startTime
	private int remainingToBookVolume;
	
	protected ArrayList<DeliveryInitial> parcelDeliveries; //physical deliveries, to be created just before delivery
	
	private boolean orderReserved; //to track that all the intention ants are said ACCEPT to correstponding deliveries

	public OrderAgentInitial(Simulator pSim, Point pLocation, Order pOrder ) {
		super();
		Location = pLocation;
		super.setStartPosition(Location);
		order = pOrder;
		sim = pSim;
		logger = Logger.getLogger(OrderAgentInitial.class);
		
		orderReserved = false;
		mailbox = new Mailbox();
		timeForLastFeaAnt = new DateTime(0);
		
		
		interestedTime = order.getStartTime(); 
		delayFromActualInterestedTime = new Duration(0);
		delayStartTime = new Duration(0);
		remainingToBookVolume = order.getRequiredTotalVolume();
		
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		deliveries = new ArrayList<Delivery>();
		parcelDeliveries = new ArrayList<DeliveryInitial>();
	}
	@Override
	public void tick(TimeLapse timeLapse) {
			// send F-Ant after F_interval
		sendFeasibilityInfo(timeLapse.getStartTime());
		checkMsgs(timeLapse.getStartTime());
		processExplorationAnts(timeLapse.getStartTime());
		processIntentionAnts(timeLapse);
		generateParcelDeliveries(timeLapse.getStartTime());
		//checkAllDeliveriesReservation();
		if (this.orderReserved == true) 
			; //check that in this case Trucks should not propose it any more.. 
		
	}


	/**
	 * Generates actual parcels corresponding to the deliveries, to be picked up physically by trucks. 
	 * Parcels are created at the production Site where they have to be picked. 
	 * @param startTime
	 */
	private void generateParcelDeliveries(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (!deliveries.isEmpty()) {
			int dNo = 0;
			for (Delivery d : deliveries) {
				DateTime deliveryTime = d.getDeliveryTime().minus(d.getStationToCYTravelTime().minus(5l*60l*1000l)); 
				if (currTime.compareTo(deliveryTime) >= 0) {
					if (d.isReserved() == false){
						parcelDeliveries.add(new DeliveryInitial(this, d, dNo, d.getLoadingStation().getPosition(), 
								this.getPosition(), (int)(d.getLoadingDuration().getMillis()), (int)(d.getUnloadingDuration().getMillis()), (double)d.getDeliveredVolume()));
						logger.debug("Delivery physically created..del NO = " + dNo + "current time = " + currTime + ", Loading time = " + deliveryTime);
						d.setReserved(true);
					}
				}
				dNo ++;
			}
		}
	}
	protected boolean processExplorationAnts(long startTime) {
		if (explorationAnts.isEmpty()) //if no exploration has been sent by trucks, then return
			return false;
		else if(orderReserved == true && explorationAnts.isEmpty() == false)  //If order is fully booked, but still proposals of truck are there
			return false;
		Iterator<ExpAnt> i = explorationAnts.iterator();
		logger.debug(order.getId() + "O Checking exp ants total are= " + explorationAnts.size() );
		while (i.hasNext()) { 
			ExpAnt exp = i.next();
			logger.debug("expStarTim = " + exp.getCurrentUnit().getTimeSlot().getStartTime() + " & interestedTim = " + interestedTime	);
			if (interestedTime.compareTo(exp.getCurrentUnit().getTimeSlot().getStartTime().plusMinutes(60)) <= 0
					&& interestedTime.compareTo(exp.getCurrentUnit().getTimeSlot().getStartTime()) >= 0) { //means exp shud include this orderk's delivery
				ArrayList<ProductionSiteInitial> sites = new ArrayList<ProductionSiteInitial>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
				ProductionSiteInitial selectedPs;
				if (sites.size()>1) 				//select the next pC to visit at random..
					selectedPs = sites.get(new RandomDataImpl().nextInt(0, sites.size()-1));
				else
					selectedPs = sites.get(0);
				Delivery del=  new Delivery(this, exp.getOriginator().getTruck(), (int)(exp.getOriginator().getCapacity()), 
						exp.getCurrentUnit().getTimeSlot().getProductionSiteAtStartTime(),selectedPs );
				long dur = (long)((Point.distance(exp.getSender().getPosition(), this.getPosition())/exp.getTruckSpeed())*60*60*1000); //hours to milli
				del.setStationToCYTravelTime(new Duration (dur));
				dur = (long)((Point.distance(selectedPs.getPosition(), this.getPosition())/exp.getTruckSpeed())*60*60*1000); //CY to returnStation distance
				del.setCYToStationTravelTime(new Duration(dur));
				del.setDeliveryTime(interestedTime);
				if (this.deliveries.size() == 0) //means at the moment decesions are for first delivery so ST shud b included
					del.setLagTime(this.delayFromActualInterestedTime.plus(this.delayStartTime));
				else
					del.setLagTime(this.delayFromActualInterestedTime); //no ST added this time
				if (remainingToBookVolume < (int)(exp.getOriginator().getCapacity())) { //if remaining volume is less but truck capacity is higher
					int requiredVolume = (int)(exp.getOriginator().getCapacity() - remainingToBookVolume);
					del.setWastedVolume(requiredVolume);
					del.setUnloadingDuration(new Duration((long)((requiredVolume / GlobalParameters.DISCHARGE_RATE_PERHOUR) * 60*60*1000)));
				}
				else {
					del.setWastedVolume(0);// no wastage 
					del.setUnloadingDuration(new Duration((long)((exp.getOriginator().getCapacity() / GlobalParameters.DISCHARGE_RATE_PERHOUR) * 60*60*1000)));
				}
				del.setLoadingDuration(new Duration(GlobalParameters.LOADING_MINUTES*60l*1000l));
				ExpAnt newExp = (ExpAnt)exp.clone(this);
				newExp.getCurrentUnit().setDelivery(del);
				newExp.getCurrentUnit().getTimeSlot().setEndtime(interestedTime.plus(del.getUnloadingDuration()).plus(del.getCYToStationTravelTime()));
				logger.debug(order.getId() + "O delivery added in expAnts schedule, orginator = " + newExp.getOriginator().getId());
				newExp.getSchedule().add(newExp.getCurrentUnit());//Actual addition in schedule
				cApi.send(del.getReturnStation(), newExp); 
				//deliveries.add(del); At intentionAnt this shud b added at actual deliveries of order
			}
			else ; //will be removed any way..:)
			i.remove(); //exp should die
		}
		return true;
		/* 
		 * here add the logic 
		 * while (all expants);
		 * chkargument(order insterestedTime <= exp.currentTime + 10min)
		 * if expAnt.continueExploration == true
		 * 	make a delivery accroding to exp.truck.capacity, set its attributes
		 *  select PS at random, and makeit it return station
		 *  // Latter give flexibility of 2min, before and after delivery
		 *  sendExp to selected PS
		 *  
		 */
		
	}
	private void processIntentionAnts(TimeLapse timeLapse) {
		if (this.orderReserved == true || intentionAnts.isEmpty()) 
			return;
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //at the moment just select the first one
			IntAnt iAnt = i.next();
			if (iAnt.getCurrentUnit().getDelivery().getDeliveryTime().equals(this.interestedTime)) { //means select it..say ok
				iAnt.getCurrentUnit().setOrderReply(Reply.WEEK_ACCEPT); //strong accept if whole voluem done..
				Duration dur =  iAnt.getCurrentUnit().getDelivery().getUnloadingDuration();
				interestedTime = iAnt.getCurrentUnit().getDelivery().getDeliveryTime().plus(dur); //send feasibility ants..
				remainingToBookVolume = (int)(remainingToBookVolume - iAnt.getOriginator().getCapacity());
				deliveries.add(iAnt.getCurrentUnit().getDelivery());
				
			}
			else {
				iAnt.getCurrentUnit().setOrderReply(Reply.REJECT); //means order will not be feasible
				
			}
			logger.debug("int Ant reply of order is " + iAnt.getCurrentUnit().getOrderReply());
			IntAnt newAnt = iAnt.clone(this);
			newAnt.setNextCurrentUnit();
			cApi.send(iAnt.getCurrentUnit().getDelivery().getReturnStation(), newAnt);
			i.remove();
		}
		/*
		 * chk for which delivery truck is intending
		 *  if for first deliver, then make it week.Accept
		 *  if not for first delivery, then chk are rest of deliveries before this delivery are intended? if no then
		 *  wait and see rest of intentions
		 *  
		 *  if a delivery week accepted, order shud change its interestedTime at the the PS by sending feasiblity ants
		 *   
		 *  Also set the logic, if no one is sending intention for long time
		 *  adjust the starttime delay and interestedtime delay..
		 */
	}
	@Override
	public void afterTick(TimeLapse timeLapse) {

	}
	//TODO: add Test method that it really sends after specified interval
	private void sendFeasibilityInfo(long startTime) {
		//create f-ant 
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (currTime.minusMinutes(timeForLastFeaAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.FEASIBILITY_INTERVAL_MIN ) {
			FeaAnt fAnt = new FeaAnt(this, this.interestedTime );
			//logger.debug(order.getId() + "O Feasibility sent by order");
			cApi.broadcast(fAnt);
			timeForLastFeaAnt = currTime;
		}
	}
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0) {
			//logger.debug(order.getId() + "O Exp re);
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					this.explorationAnts.add((ExpAnt)m);
					//logger.debug(order.getId() + "O Exp received");
				}
				else if (m.getClass() == IntAnt.class) {
					this.intentionAnts.add((IntAnt)m);
					logger.debug(order.getId() + "O Int received");
				}
				else {
					; //Do nothing i.e there is chance to receive feasiblity from other orders..
				}
					
			}
		}
	}
	public DeliveryInitial getDeliveryForDomainDelivery(Delivery pdelivery){
		if (parcelDeliveries.size() > 0) {
			for (DeliveryInitial rmcDel: parcelDeliveries) {
				if (rmcDel.getDelivery().equals(pdelivery)) {
					return rmcDel;
				}
			}
		}
		return null;
	}
	
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
	}
	
	/* 
	 * Should serve same as getLocation, returns location in PDP model
	 */
	public Point getPosition() {
		return pdpModel.getPosition(this);
	}
	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;
	}

	@Override
	public double getRadius() {
		return 100;
	}

	@Override
	public double getReliability() {
		return 1;
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}

	public Order getOrder() {
		return order;
	}

	public boolean isOrderReserved() {
		return orderReserved;
	}
	@Override
	public int getId() {
		return Integer.getInteger(order.getId());
	}
	public DateTime getInterestedTime() {
		return interestedTime;
	}
	
}
