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
import java.util.Map;
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
import shaz.rmc.core.ResultElementsOrder;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Order;
import shaz.rmc.core.domain.Station.StationBookingUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.FeaAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class OrderAgentInitial  extends Depot implements Agent {
	private RoadModel roadModel;
	private PDPModel pdpModel;
	private final Simulator sim;
	private final Point Location;
	private final Order order;
	private final ArrayList<Delivery> deliveries; //virtual deliveries..twhich stores general information of the deliveries that have been intended by other trucks
	
	private final Logger logger; //for logging
	private ArrayList<ExpAnt> explorationAnts;
	private ArrayList<IntAnt> intentionAnts;
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	private DateTime timeForLastFeaAnt; /** to keep track of feasibility interval */
	private final Map<Delivery, DateTime> refreshTimes; //for keepting track of when to evaporate intentions
	private final Map<Delivery, Boolean> isPhysicallyCreated;
	private final Map<Delivery, Boolean> isConfirmed;
	
	//REalted to current interest
	private DateTime interestedTime;  //time sent by FeaAnts, to indicate this is the time at which order is interested
	private int interestedDeliveryNo; //the no. of current delivery 
	private Duration delayFromActualInterestedTime; //to keep recored of delay before interestedTime, for first delivery it should be zero
	private Duration delayStartTime; //delay in startTime
	private int remainingToBookVolume;
	
	private ArrayList<DeliveryInitial> parcelDeliveries; //physical deliveries, to be created just before delivery (5min b4 actual delivery needed to b picked up)
	ArrayList<ProductionSiteInitial> sites; 
	
	//private boolean orderReserved; //to track that all the intention ants are said ACCEPT to correstponding deliveriesm, means order is fully confirmed and no dleivery is remaining
	private ORDER_STATE state;
	public OrderAgentInitial(Simulator pSim, Point pLocation, Order pOrder ) {
		super();
		Location = pLocation;
		super.setStartPosition(Location);
		order = pOrder;
		sim = pSim;
		logger = Logger.getLogger(OrderAgentInitial.class);
		
		state = ORDER_STATE.IN_PROCESS;
		mailbox = new Mailbox();
		timeForLastFeaAnt = new DateTime(0);
		
		interestedTime = order.getStartTime(); 
		interestedDeliveryNo = 0;
		delayFromActualInterestedTime = new Duration(0);
		delayStartTime = new Duration(0);
		remainingToBookVolume = order.getRequiredTotalVolume();
		
		explorationAnts = new ArrayList<ExpAnt>();
		intentionAnts = new ArrayList<IntAnt>();
		deliveries = new ArrayList<Delivery>();
		parcelDeliveries = new ArrayList<DeliveryInitial>();
		
		refreshTimes = new LinkedHashMap<Delivery, DateTime>();
		isConfirmed =new LinkedHashMap<Delivery, Boolean>();
		isPhysicallyCreated = new LinkedHashMap<Delivery, Boolean>();
	}
	@Override
	public void tick(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());
		processExplorationAnts(timeLapse.getStartTime());
		processIntentionAnts(timeLapse);
		generateParcelDeliveries(timeLapse.getStartTime());
		sendFeasibilityInfo(timeLapse.getStartTime());
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
			int cushionMinutes = 5; //mintues before pickup time, when delivery should be created.
			for (Delivery d : deliveries) { //5 min cushion time, 5min for PS fillling time
				DateTime createTime = d.getDeliveryTime().minus(d.getStationToCYTravelTime()).minusMinutes(cushionMinutes).minusMinutes(GlobalParameters.LOADING_MINUTES); 
				if (currTime.compareTo(createTime) >= 0) {
					if (isPhysicallyCreated.get(d) == false && isConfirmed.get(d) == true){ //isReservd means isPhysically created..
						DeliveryInitial pd = new DeliveryInitial(this, d, dNo, d.getLoadingStation().getPosition(), 
								this.getPosition(), d.getLoadingDuration().getMillis(), d.getUnloadingDuration().getMillis(), (double)d.getDeliveredVolume());
						sim.register(pd);
						parcelDeliveries.add(pd);
						logger.debug(order.getId() + "O Delivery physically created..del NO = " + dNo + "current time = " + currTime + ", Loading time = " + createTime.plusMinutes(cushionMinutes));
						isPhysicallyCreated.put(d, true);
					}
				}
				dNo ++;
			}
		}
	}

	private boolean processExplorationAnts(long startTime) {
		if (explorationAnts.isEmpty()) //if no exploration has been sent by trucks, then return
			return false;
		else if(this.state == ORDER_STATE.BOOKED || this.state == ORDER_STATE.WAITING) { //If order is fully booked, but still proposals of truck are there
			explorationAnts.clear();
			return false;
		}
		Iterator<ExpAnt> i = explorationAnts.iterator();
		logger.debug(order.getId() + "O Checking exp ants total are= " + explorationAnts.size() );
		while (i.hasNext()) { 
			ExpAnt exp = i.next(); //i guess once reached to order, it shud just include order in its list, since order was in general ok
			long psToCyDur = (long)((Point.distance(exp.getSender().getPosition(), this.getPosition())/exp.getTruckSpeed())*60*60*1000); //hours to milli
			if (exp.getCurrentInterestedTime().minus(exp.getLagTime()).equals(interestedTime.minus(psToCyDur).minusMinutes(GlobalParameters.LOADING_MINUTES)) ) {
				logger.debug(order.getId() + "O expStarTim = " + exp.getCurrentInterestedTime() + " & interestedTim = " + interestedTime + " loadingTime = " + interestedTime.minus(psToCyDur).minusMinutes(GlobalParameters.LOADING_MINUTES));	
				Delivery del = prepareNewDelivery(interestedDeliveryNo, exp, interestedTime, psToCyDur);
				sendToPs(exp, del);
			}
			i.remove(); //exp should die
		} //end while (i.hasNext())
		checkArgument (explorationAnts.isEmpty(), true);
		return true;
	}
	private Delivery prepareNewDelivery(int pDeliveryNo, ExpAnt exp, DateTime pDeliveryTime, long travelDur) {
		
		ProductionSiteInitial selectedPs;
		if (exp.nextAfterCurrentUnit()!= null) {//next slot in schedule exists, select the start PS of next slot
			selectedPs = (ProductionSiteInitial)exp.nextAfterCurrentUnit().getTimeSlot().getProductionSiteAtStartTime();
		}
		else {//next slot in schedule doesn't exist, so select randomly
			if (sites.size()>1) 				//select the next pC to visit at random..
				selectedPs = sites.get(new RandomDataImpl().nextInt(0, sites.size()-1));
			else
				selectedPs = sites.get(0);
		} 
		Delivery.Builder delBuilder = new Delivery.Builder();
		delBuilder.setOrder(this);
		delBuilder.setDeliveryNo(pDeliveryNo);
		delBuilder.setTruck(exp.getOriginator());
		delBuilder.setDeliveredVolume((int)(exp.getOriginator().getCapacity()));
		//delBuilder.setLoadingStation(exp.getCurrentUnit().getTimeSlot().getProductionSiteAtStartTime());
		delBuilder.setLoadingStation((ProductionSite)exp.getSender());
		delBuilder.setReturnStation(selectedPs);
		
		delBuilder.setStationToCYTravelTime(new Duration (travelDur));
		travelDur = (long)((Point.distance(selectedPs.getPosition(), this.getPosition())/exp.getTruckSpeed())*60*60*1000); //CY to returnStation distance
		delBuilder.setCYToStationTravelTime(new Duration(travelDur));
		delBuilder.setDeliveryTime(pDeliveryTime.plus(exp.getLagTime()));
//			if (this.deliveries.size() == 0 ) //means at the moment decesions are for first delivery so ST shud b included
//				{ 
//					//checkArgument(pDeliveryNo == 0, true);
//					delBuilder.setLagTime(this.delayFromActualInterestedTime.plus(this.delayStartTime));
//				}
//			else
//				delBuilder.setLagTime(this.delayFromActualInterestedTime); //no ST added this time
//	
		delBuilder.setLagTime(exp.getLagTime());
		if (remainingToBookVolume < (int)(exp.getOriginator().getCapacity())) { //if remaining volume is less but truck capacity is higher
			int wastedVolume = (int)(exp.getOriginator().getCapacity() - remainingToBookVolume);
			delBuilder.setWastedVolume(wastedVolume);
			delBuilder.setUnloadingDuration(new Duration((remainingToBookVolume * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)));
		}
		else {
			delBuilder.setWastedVolume(0);// no wastage 
			delBuilder.setUnloadingDuration(new Duration((long)(exp.getOriginator().getCapacity()* 60l*60l*1000l / GlobalParameters.DISCHARGE_RATE_PERHOUR) ));
		}
		delBuilder.setLoadingDuration(new Duration(GlobalParameters.LOADING_MINUTES*60l*1000l));
		Delivery del = delBuilder.build();
		return del;
		
	}

	private void sendToPs(ExpAnt exp, Delivery del) {
		ExpAnt newExp = (ExpAnt)exp.clone(this);
		if (newExp.makeCurrentUnit(del)) {
			logger.debug(order.getId() + "O delivery added in expAnts schedule, orginator = " + newExp.getOriginator().getId());
			cApi.send(del.getReturnStation(), newExp);
		}
		else
			logger.debug(order.getId() + "O FAILURE in adding delivery in expSchedule");
	}
	private void processIntentionAnts(TimeLapse timeLapse) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		if ( intentionAnts.isEmpty()) 
			return;
		 //aparently it will reach beyond this point depending intention_interval
		if (GlobalParameters.LAG_TIME_ENABLE)
			this.sortIntentionArts();
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //at the moment just select the first one
			IntAnt iAnt = i.next();
			if (iAnt.getCurrentUnit().getDelivery().getDeliveryTime().minus(iAnt.getCurrentUnit().getDelivery().getLagTime()).equals(this.interestedTime) //so iAnt is according to order's current interest or according to currentInterest + lagTime
					//|| (iAnt.getCurrentUnit().getDelivery().getDeliveryTime().compareTo(this.interestedTime ) > 0 && iAnt.getCurrentUnit().getDelivery().getDeliveryTime().compareTo(this.interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES) ) <= 0))
					&& iAnt.getCurrentUnit().getTunit().getDelivery().getDeliveryNo() == this.interestedDeliveryNo && this.state == ORDER_STATE.IN_PROCESS
					&& iAnt.getCurrentUnit().isAddedInTruckSchedule() == false) {
				boolean iAntAccepted = false;
				checkArgument(this.state == ORDER_STATE.IN_PROCESS, true);
				if (refreshTimes.containsKey(iAnt.getCurrentUnit().getDelivery()) == false ) {//refreshTimes.get(iAnt.getCurrentUnit().getDelivery().getDeliveryNo()).equals(currTime) == false) {
					if (iAnt.getCurrentUnit().getPsReply() == Reply.UNDER_PROCESS) { //PS is ok with this delivery
							checkArgument(iAnt.getCurrentUnit().isAddedInTruckSchedule() == false, true);
							acceptIntention(iAnt, currTime);
							iAntAccepted = true;
							this.state = ORDER_STATE.WAITING;
					}
				} //else could be case if a previous delivery was reopended..
//				else if (refreshTimes.get(iAnt.getCurrentUnit().getDelivery()).equals(currTime) == false) { //TODO, correct this if-else, since else is
//					checkArgument(this.deliveries.size() > 0 == true, true);			//only to express that if Containskey == true, then check whats the value in refreshTimes
//					if (iAnt.getCurrentUnit().getPsReply() == Reply.UNDER_PROCESS) { //PS is ok with this delivery
//							checkArgument(iAnt.getCurrentUnit().isAddedInTruckSchedule() == false, true);
//							acceptIntention(iAnt, currTime);
//							iAntAccepted = true;
//					}
//				} 
				if (!iAntAccepted) {
					iAnt.getCurrentUnit().setOrderReply(Reply.REJECT);
					checkArgument(this.state == ORDER_STATE.IN_PROCESS);  //this could be wrong..haven't thought too much..
				}
			}
			else { //order isn't interested, yet it could be refreshing of a previous booking
				Delivery d = deliveryExists(iAnt.getCurrentUnit().getDelivery());
				if (d != null && refreshTimes.get(d).compareTo(currTime) < 0 
						&& iAnt.getCurrentUnit().getPsReply() == Reply.WEEK_ACCEPT  
						&& iAnt.getCurrentUnit().isAddedInTruckSchedule() == true) { //so its not just recently added delivery. second condition is added since there could be 2 intention ants from same truck
					if (isConfirmed.get(iAnt.getCurrentUnit().getDelivery()) == false && this.state == ORDER_STATE.WAITING) {
						isConfirmed.put(iAnt.getCurrentUnit().getDelivery(), true);
						this.state = ORDER_STATE.IN_PROCESS;
					} //else we shouldn't touch order state
					refreshTimes.put(iAnt.getCurrentUnit().getDelivery(), currTime);
					iAnt.getCurrentUnit().setOrderReply(Reply.WEEK_ACCEPT); //So earlier it could be UnderProcess, but once confirmed, its Weekly accepted
					logger.debug(order.getId() + "O int-" + iAnt.getOriginator().getId()+" booking refreshed");
				}
				else //now its sure order isn't interested!
					iAnt.getCurrentUnit().setOrderReply(Reply.REJECT);
			}
			logger.debug(order.getId() + "O int-" + iAnt.getOriginator().getId()+" reply is " + iAnt.getCurrentUnit().getOrderReply() + " currTime= " + currTime);
			IntAnt newAnt = iAnt.clone(this);
			newAnt.setNextCurrentUnit();
			cApi.send(iAnt.getCurrentUnit().getDelivery().getReturnStation(), newAnt);
			i.remove();
		}
	}
	//a temproary sorting mechanism to sort intention ants, so that we can execute lag time..TEsted as well
	private void sortIntentionArts() {
		//this.intentionAnts
		Collections.sort(this.intentionAnts, new Comparator<IntAnt>(){
	        public int compare( IntAnt a, IntAnt b ){
	            return (int)(b.getScheduleLagTime().minus(a.getScheduleLagTime()).getStandardSeconds());
	        }
		});
		
	}
	/**
	 * should not be called for refresh deliveries, rather it should be called for the deliveries for which order was really interested.
	 * @param iAnt the ant under process
	 * @param currTime
	 */
	private void acceptIntention(IntAnt iAnt, DateTime currTime) {
		iAnt.getCurrentUnit().setOrderReply(Reply.UNDER_PROCESS); // at the momnet its under consideration, once refreshed, it will be week_accept
		refreshTimes.put(iAnt.getCurrentUnit().getDelivery(), currTime);
		deliveries.add(iAnt.getCurrentUnit().getDelivery());
		isPhysicallyCreated.put(iAnt.getCurrentUnit().getDelivery(), false);
		isConfirmed.put(iAnt.getCurrentUnit().getDelivery(), false);
		//setOrderInterests();
	}
	/**
	 * @param iAnt te ant under process
	 */
	private void setOrderInterests() {
		checkArgument(this.state == ORDER_STATE.IN_PROCESS);
		remainingToBookVolume = calculateRemainingVolume();
		if (!deliveries.isEmpty()) {
			Delivery lastDelivery = deliveries.get(deliveries.size()-1);
			interestedTime = lastDelivery.getDeliveryTime().plus(lastDelivery.getUnloadingDuration());
			interestedDeliveryNo = lastDelivery.getDeliveryNo() +1;
		}
		else{
			interestedTime = order.getStartTime(); 
			interestedDeliveryNo = 0;
		}			
		if (remainingToBookVolume <= 0) {
			this.state = ORDER_STATE.BOOKED;
			logger.debug(order.getId() + "O fully BOOKED");
		}
	}
	private int calculateRemainingVolume() {
		int remainingVolume = order.getRequiredTotalVolume();
		if (!deliveries.isEmpty()){
			for (Delivery d : deliveries) {
				remainingVolume -= d.getDeliveredVolume()-d.getWastedVolume();
			}
		}
		if (remainingVolume < 0)
			remainingVolume = 0;
		return remainingVolume;
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

	private Delivery deliveryExists(Delivery newDel) {
		for (Delivery existingDel: deliveries) {
			if (existingDel.equals(newDel))
				return existingDel;
		}
		return null;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {

	}
	private void sendFeasibilityInfo(long startTime) {
		if (this.state == ORDER_STATE.BOOKED)
			return;
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (currTime.minusMinutes(timeForLastFeaAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.FEASIBILITY_INTERVAL_MIN ){
			if (this.state == ORDER_STATE.IN_PROCESS) { // && remainingToBookVolume > 0)  {
				setOrderInterests();
				FeaAnt fAnt = new FeaAnt(this, this.interestedTime); //the distance is calculated when ant reaches the PS
				for (ProductionSiteInitial p : sites) {
					if (new Duration ((long)((Point.distance(p.getPosition(), this.getPosition())/GlobalParameters.TRUCK_SPEED)*60l*60l*1000l)).getStandardMinutes() <= GlobalParameters.MINUTES_TO_PERISH_CONCRETE  )
						cApi.send(p, fAnt);
				}
				timeForLastFeaAnt = currTime;
			}
			else {
				Delivery d = checkOrderStatus(startTime);
				if (d != null){
					interestedTime = d.getDeliveryTime();
					interestedDeliveryNo = d.getDeliveryNo();
					FeaAnt fAnt = new FeaAnt(this, interestedTime);  //the distance is calculated when ant reaches the PS
					deliveries.remove(d);
					refreshTimes.remove(d);
					isConfirmed.remove(d); //TODO add if isphyc.contains key, then remove
					isPhysicallyCreated.remove(d);
					cApi.broadcast(fAnt);
				}
				timeForLastFeaAnt = currTime;
			}
		}
	}
	
	private Delivery checkOrderStatus(long currMilli) {
		checkArgument(this.state == ORDER_STATE.WAITING, true);
		long currMilliInterval;
		for (Delivery d : deliveries){
			currMilliInterval = GlobalParameters.START_DATETIME.plusMillis((int)currMilli).getMillis() - refreshTimes.get(d).getMillis();
			if (isConfirmed.get(d) == false 
					&& (new Duration (currMilliInterval)).getStandardMinutes() > GlobalParameters.INTENTION_EVAPORATION_MIN) {
				this.state = ORDER_STATE.IN_PROCESS;
				logger.debug(order.getId() + "O Order Re-set");
				return d;
			}
		}
		return null;
	} 
	/**
	 * @param pdelivery the domaim.delivery object which stores general information of the deliveries that have been intended by other trucks
	 * @return parcelDelivery, i.e actual physicial delivery
	 */
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
		sites = new ArrayList<ProductionSiteInitial>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
	}
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0) {
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					this.explorationAnts.add((ExpAnt)m);
				}
				else if (m.getClass() == IntAnt.class) {
					this.intentionAnts.add((IntAnt)m);
				}		//can get f-ants from other orders but ignore
			}
		}
	}
	
	public ResultElementsOrder getOrderResult() { 
		int deliveredConcrete = 0;
		if (deliveries.size() > 0) {
			for (Delivery d : deliveries) { //there might b some error in this calculation.
				DeliveryInitial di = this.getDeliveryForDomainDelivery(d);
				if (di != null) {
					deliveredConcrete += di.getDelivery().getDeliveredVolume();
				}
			}
			return new ResultElementsOrder(deliveries, this.order.getRequiredTotalVolume(), deliveredConcrete);
		}
		return new ResultElementsOrder(null, this.order.getRequiredTotalVolume(), deliveredConcrete);
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

	@Override
	public int getId() {
		return Integer.getInteger(order.getId());
	}
	@Override
	public String toString() {
		return "OrderAgentInitial [ "
				+ "order=" + order.toString() + "\n , deliveries=" + deliveries
				+ ", timeForLastFeaAnt=" + timeForLastFeaAnt
				+ ", refreshTimes=" + refreshTimes
				+ ", interestedTime=" + interestedTime
				+ ", interestedDeliveryNo=" + interestedDeliveryNo
				+ ", delayFromActualInterestedTime="
				+ delayFromActualInterestedTime + ", delayStartTime="
				+ delayStartTime + ", remainingToBookVolume="
				+ remainingToBookVolume + ", parcelDeliveries="
				+ parcelDeliveries + ", orderState="
				+ state + "]";
	}
	
	private enum ORDER_STATE {
		IN_PROCESS, // The normal and general state
		WAITING, // order is waiting for a delivery's confirmation from TruckAgent
		BOOKED //whole concrete of order is booked.
	}
	
}
