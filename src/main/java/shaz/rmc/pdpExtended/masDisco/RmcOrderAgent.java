/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;

import shaz.rmc.core.Reply;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.IntentionAnt;
import shaz.rmc.core.Agent;
import shaz.rmc.core.ExplorationAnt;
import shaz.rmc.core.domain.Order;
import shaz.rmc.pdpExtended.masDisco.RmcDelivery;

/**
 * @author Shaza
 *
 */
public class RmcOrderAgent implements Agent {
	
	private final Logger logger; //for logging
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	private final OrderSite orderSite; // contains the physical site information for display purpose only.
	private final Order order;
	private final Simulator sim;
	
	private final int expAntsAmountComparedDels; //amount of expAnts, as compared to number of deliveries of an order
	private boolean deliveriesReady; //to make sure that all the deliveries are created properly
	private boolean orderReserved; //to track that all the intention ants are said ACCEPT to correstponding deliveries
	private boolean allDeliveriesReserved; //to keep track that have all the deliveries of this order reserved, but not accepted by Trucks (i.e proposals to trunks are not sent yet)
	private int waitingTicksForSTDelay;
	private int waitingTicksForIntentionAnts; //no of ticks, order should wait for intention ants to be sent by the trucks to which Order sent expAnt.reply(delivery). It keeps the
	//track if waitingTicks == IntentionEvaporation rate, then reset Order


	protected ArrayList<Delivery> deliveries;
	protected ArrayList<ExplorationAnt> explorationAnts;
	protected ArrayList<IntentionAnt> intentionAnts;
	protected ArrayList<RmcDelivery> parcelDeliveries;
	
	public RmcOrderAgent(Simulator pSim, Point pLocation, Order pOrder ) {
		super();
		logger = Logger.getLogger(RmcOrderAgent.class);

		mailbox = new Mailbox();
		orderSite = new OrderSite(pSim, pLocation, this); //for a physical building in model
		orderSite.register();
		order = pOrder;
		sim = pSim;
		
		deliveriesReady = false;
		orderReserved = false;
		this.allDeliveriesReserved = false;
		expAntsAmountComparedDels = 2;
		this.waitingTicksForSTDelay = 0;
		this.waitingTicksForIntentionAnts = 0;
		
		deliveries = new ArrayList<Delivery>();
		explorationAnts = new ArrayList<ExplorationAnt>();
		intentionAnts = new ArrayList<IntentionAnt>();
		parcelDeliveries = new ArrayList<RmcDelivery>();
	}


	@Override
	public void tick(TimeLapse timeLapse) {
		if (deliveriesReady) {
			checkMsgs(timeLapse.getStartTime());
			processIntentionAnts(timeLapse);
			processExplorationAnts(timeLapse);
			checkAllDeliveriesReservation();
			if (this.orderReserved == true) 
				; //check that in this case Trucks should not propose it any more.. 
		}
		else //only called for the first time, or when deliveries not ready..
		{
			initialize();
		}
		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub

	}

	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0)
			System.out.println(this.order.getId()+"O received messages quantity = " + messages.size());
		for (Message m : messages) {
			if (m.getClass() == ExplorationAnt.class) {
				this.explorationAnts.add((ExplorationAnt)m);
			}
			else if (m.getClass() == IntentionAnt.class) {
				this.intentionAnts.add((IntentionAnt)m);
			}
				
		}
	}
	
	/**
	 * @description The method expects that exploration ants of some trucks have been replied, as a response, the trucks will send the intention ants. 
	 * At the moment it is not considered that truck can reject, and may not propose as a response to the Postive reply of an exploration ant.
	 * Currently, the implementation of method considers that intention ants will always be be proposed for all the deliveries. 
	 * 
	 * Currently, the method doesn't consider that truck can propose some new time for the delivery, rather it will make situation very difficult. This
	 * could only be allowed (I think) when I allow >0 lag time. 
	 * 
	 * As a result: If all the execution of this method, if all intention ants are not proposed yet, the order will be keep on waiting and will make the
	 * status of so far received ants as 'UNDER_PROCESS'. When all ants (i.e related to all deliveries of the order) are proposed, the order will set 
	 * orderReserved = true, and jointly accept all the proposals. 
	 * 
	 */
	private void processIntentionAnts(TimeLapse timeLapse) {
		if (this.orderReserved == true) 
			return;
		if (intentionAnts.isEmpty() && orderReserved == false && allDeliveriesReserved == true) 
			waitingTicksForIntentionAnts ++;
		if ( allDeliveriesReserved == true && waitingTicksForIntentionAnts == GlobalParameters.INTENTION_EVAPORATION_RATE ){
			waitedEnoughForIntentions();
			return;
		}
		//logger.debug(order.getId() + "O: processing Intention ants and there number is = " + intentionAnts.size());
		Iterator<IntentionAnt> i = intentionAnts.iterator();
		boolean restDeliveriesBooked = false;
		while (i.hasNext())  {
			IntentionAnt intAnt = i.next();
			deliveries.get(deliveries.indexOf(intAnt.getDelivery())).setConfirmed(true);//so what ever proposal comes, just accept it!
			deliveries.get(deliveries.indexOf(intAnt.getDelivery())).setTruck(intAnt.getDeliveryTruck());
			deliveries.get(deliveries.indexOf(intAnt.getDelivery())).setLoadingStation(intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime());
			deliveries.get(deliveries.indexOf(intAnt.getDelivery())).setLoadingDuration(intAnt.getIntendedTimeSlot().getProductionSiteAtStartTime().getStation().getLoadingDuration());
			//so that latter parcle deliveries could be created at the loadingStation locaiton ..
			restDeliveriesBooked = true;
			for (Delivery d: deliveries) {
				if (d.isConfirmed() == false) {
					restDeliveriesBooked = false;
					intAnt.setReply(Reply.UNDER_PROCESS);
					waitingTicksForIntentionAnts ++;
					break;
					//i guess wait further for confirmations from other trucks
				}
			}
			if (restDeliveriesBooked && intAnt.getReply() == Reply.NO_REPLY) {
				intAnt.setReply(Reply.ACCEPT);
				logger.debug(order.getId() + "O: propsoal of delivery no. "+ deliveries.indexOf(intAnt.getDelivery()) + " by truck" + ((RmcDeliveryTruck)intAnt.getDeliveryTruck()).getId()+" is ACCEPTED");
				break;
			}
		}
		if (restDeliveriesBooked) {
			orderReserved = true;
			logger.debug(order.getId() + "O: fully Booked");
			
			Iterator<IntentionAnt> j = intentionAnts.iterator();
			while (j.hasNext())  {
				IntentionAnt ia = j.next();

				if (ia.getReply() == Reply.UNDER_PROCESS){
					ia.setReply(Reply.ACCEPT);
					logger.debug(order.getId() + "O: propsoal of delivery no. "+ deliveries.indexOf(ia.getDelivery()) + " by truck" + ((RmcDeliveryTruck)ia.getDeliveryTruck()).getId()+" is ACCEPTED and CURRENT TIME IS = " +
							new DateTime(GlobalParameters.START_DATETIME.getMillis() + (timeLapse.getStartTime())) );
				} 
				else if (ia.getReply() == Reply.NO_REPLY) {
					ia.setReply(Reply.REJECT);
					logger.debug(order.getId() + "O: propsoal of delivery no. "+ deliveries.indexOf(ia.getDelivery()) + " by truck" + ((RmcDeliveryTruck)ia.getDeliveryTruck()).getId()+" is REJECTED");
					j.remove();
				}
			}
			//create Deliveries/parcels
			
			for (Delivery del : deliveries) {
				RmcDelivery d = new RmcDelivery(this, del, deliveries.indexOf(del), del.getLoadingStation().getLocation(), 
						this.getPosition(), (int)del.getLoadingDuration().getMillis(), (int)del.getUnloadingDuration().getMillis(), 0); // i can use del.getDeliveredVolume() but for it i should take care of truck capcities as well
				
				parcelDeliveries.add(d);
				sim.register(d);
			}
			//RmcDelivery d1 = new RmcDelivery(this, 1, previousSite!=null?roadModel.getPosition(previousSite):null, Location, 5, 5, 10); // at the moment
			//deliveries.add(d1);		
			//register deliveries
			//sim.register(d1);
			return;
		}
		if (waitingTicksForIntentionAnts == GlobalParameters.INTENTION_EVAPORATION_RATE ) { //Order waited alot for intention ants to arrive, but they didn't so again exploration is allowed
			waitedEnoughForIntentions();
		}
	}
	void waitedEnoughForIntentions(){
		removeIntentionAnts();
		reSetOrder();
		waitingTicksForIntentionAnts = 0;
		orderReserved = false;
		allDeliveriesReserved = false;
		logger.debug(order.getId() + "O: Intentions evaporated and Order is Reset");
	}

	
	/**
	 * @Description processes Exploration ants if any. Evaporates according to Evaporation rate, removes exploration ants if Order is fully booked.
	 * It is expected that trucks have given their available slot and the time to reach the CY. The time to reach already includes the loading time at PC. 
	 * The order checks that if the trucks available time+ time to reach < delivery time => Delivery is possible by the truck. So both truck and 
	 * the specific delivery will be assigned to each other. The trucks available slots will be changes so that it can reserve the PC at proper time before delivery.
	 *  
	 *  Also if Order is not reserved, despite several proposals from truck and MINUTES_TO_CHANGE_ST4ORDER has been elapsed, the order's startTime will be changed.
	 */
	private void processExplorationAnts(TimeLapse timeLapse) {
		waitingTicksForSTDelay++;
		evaporateExplorationAnts(timeLapse);
		if (explorationAnts.isEmpty()) //if no exploration has been sent by trucks, then return
			return;
		else if(orderReserved == true && explorationAnts.isEmpty() == false)  //If order is fully booked, but still proposals of truck are there
			removeExplorationAnts();
		else if (explorationAnts.size() < expAntsAmountComparedDels*deliveries.size()) //enough exploration ants have been received, i.e equal to no. of deliveries
			return;
		else if (allDeliveriesReserved) { 
			removeExplorationAnts();
			return;
		} 
		//logger.debug(order.getId() + "O: processing exploration ants, total is = " + explorationAnts.size() + ", CURRENT_TIME IS = " + new DateTime(GlobalParameters.START_DATETIME.getMillis() + timeLapse.getStartTime()) 
		//+ "w = " + waitingTicksForSTDelay + "nw = " + (waitingTicksForSTDelay*200)/(1000*60));
		try {
			for (int i = 0; i < deliveries.size() ; i++) {
				Delivery d = deliveries.get(i);
				
				if (i == 0) //means first delivery
					d.setDeliveryTime(order.getStartTime());
				int k=i+1;
				if (k < deliveries.size()) // means next delivery exists..
					deliveries.get(k).setDeliveryTime(d.getDeliveryTime().plus(d.getUnloadingDuration())); //lag time considered 0
				Iterator<ExplorationAnt> j = explorationAnts.iterator();
				while (j.hasNext()) { 
					ExplorationAnt expAnt = j.next();
					if (d.isReserved() == false &&  expAnt.getCurrentDelivery() == null && d.getDeliveryTime().compareTo(expAnt.getAvailableTimeSlot().getStartTime().plus(expAnt.getTimeToReach())) >= 0 //means delivery's start time is > then truck availabletime + laoding + duration to reach CY
							&& (d.getDeliveryTime().plus(d.getUnloadingDuration())).compareTo(expAnt.getAvailableTimeSlot().getEndTime()) <= 0 
							 ) { // so we intend to give a +ve reply
						expAnt.setCurrentDelivery(d);
						d.setReserved(true);
						expAnt.arrangeTimeSlots(d.getDeliveryTime().minus(expAnt.getTimeToReach())); //so delivery time minus timeToREach (which already includes loading time)..the truck should be filled at station (so book station)
						//Also this should be truck's unit start time 
						//but the units end time should depend on unloading time of delivery and selection of next station..					
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();//e.printStackTrace();
		}
		allDeliveriesReserved = true;
		for (Delivery d: deliveries) { //check if all the deliveries of Order have got a trucks proposal accepted?
			if (d.isReserved() == false)
				allDeliveriesReserved = false;
		}
		if (allDeliveriesReserved) { //If true, then all trucks should be informed to finalize their proposals 
			Iterator<ExplorationAnt> j = explorationAnts.iterator();
			while (j.hasNext()) { 
				ExplorationAnt expAnt = j.next();
				if (expAnt.getCurrentDelivery() != null) {
					expAnt.reply(expAnt.getCurrentDelivery());
					j.remove();
					logger.debug(order.getId() + "O: Expant replied to T" + ((RmcDeliveryTruck)expAnt.getDeliveryTruck()).getId() + " for deliver No. " + deliveries.indexOf(expAnt.getDelivery()) + " out of " + deliveries.size() + " delivery(s) ");
				}
			}
			//waitingTicksForSTDelay=0;
		}
		else { //back to default values for each delivery of the Order
			reSetOrder();
			//allDeliveriesReserved = false;
		}
		if(GlobalParameters.MINUTES_TO_CHANGE_ST4ORDER <= (waitingTicksForSTDelay*200)/(1000*60) && this.orderReserved == false ){//&& allDeliveriesReserved == true) {
			// 5 because each tick is of 0.2sec. so 5*0.2 = 1sec. * by 60 makes it one mintues and then we use the global variable.
			//change the ST of order according to MINUTES_TO_DELAY_ST
			order.setStartTime(order.getStartTime().plusMinutes(GlobalParameters.MINUTES_TO_DELAY_ST));
			logger.debug(order.getId() + "O: START_TIME of Order is changed to " + order.getStartTime());
			waitingTicksForSTDelay = 0; //reset waiting minutes
			reSetOrder();
			//removeExplorationAnts();
			//removeIntentionAnts();
			//allDeliveriesReserved = false;
		}
	}
	/**
	 * @Description Evaporates Exploration ants, according to GlobalVariables.EXPLORATION_EVAPORATION_RATE
	 */
	private void evaporateExplorationAnts(TimeLapse timeLapse) {
		if (explorationAnts.isEmpty())
			return;
		Iterator<ExplorationAnt> i = explorationAnts.iterator();
		while (i.hasNext()) {
			ExplorationAnt expAnt = i.next();
			if (timeLapse.getStartTime() == expAnt.getCreatedTime()* GlobalParameters.EXPLORATION_EVAPORATION_RATE) {
				logger.debug(order.getId() + "O: Expant evaporated sent by T" + ((RmcDeliveryTruck)expAnt.getDeliveryTruck()).getId());
				i.remove();
			}
		}	
	}

	/**
	 * @Description removes  ants, latter, is shud use these remove and evaporate for boht intention and exploration, by specifying params..
	 * 
	 */
	private void removeIntentionAnts() {
		if (intentionAnts.size() == 0)
			return;
		logger.debug(order.getId() + "O: removing Intention ants and there number is = " + intentionAnts.size());
		Iterator<IntentionAnt> i = intentionAnts.iterator();
		while (i.hasNext()) {
			IntentionAnt intAnt = i.next();
			i.remove();
		}
	}
	/**
	 * @Description remove exprloration ants, latter, is shud use these remove and evaporate for boht intention and exploration, by specifying params..
	 * 
	 */
	private void removeExplorationAnts() {
		if (explorationAnts.size() == 0)
			return;
		logger.debug(order.getId() + "O: removing exploration ants and there number is = " + explorationAnts.size());
		Iterator<ExplorationAnt> i = explorationAnts.iterator();
		while (i.hasNext()) {
			ExplorationAnt expAnt = i.next();
			expAnt.reply(false); //all the deliveries are already reserved or time slot doesn't match..
			logger.debug(order.getId() + "O: Expant replied to T" + ((RmcDeliveryTruck)expAnt.getDeliveryTruck()).getId() + " that all deliveries already reserved or slot doesn't match!!");
			i.remove();
		}
	}
	/**
	 * Reset the order in to its initial state, means ready to accept proposals from Truck like after initialization.
	 */
	private void reSetOrder() {
		for (Delivery d : deliveries) {
			d.setDeliveryTime(null);
			d.setReserved(false);	
		}

	}
	/**
	 * @description Generates the deliveries of the order based on discharge rate of the truck. The waste calculation will be done in truck itself.
	 * The method assumets that order is already registered to OM and any one of the truck has fixed the fixedTruck volume in the OM. 
	 * Total no. of deliveries based on discharge rate could be more if discharge rate is less then truck capacity.
	 */
	private void deliveriesByOrdersDischargeRate() {
		if (deliveries.size() == 0) { //means deliveries are not created yet
			if (GlobalParameters.FIXED_VEHICLE_CAPACITY != 0) {  //means fixed volume amount has been set by a truck agent
			assert this.order.getRequiredDischargePerHour() <= GlobalParameters.FIXED_VEHICLE_CAPACITY : order.getId() + "O: Required Discharge Rate by Order is greater then fixed Truck Capacity!!";
			int totalDeliveries = 0;
			int remainingVolume = 0;
			if (GlobalParameters.FIXED_VEHICLE_CAPACITY/order.getRequiredDischargePerHour() > GlobalParameters.MINUTES_TO_PERISH_CONCRETE/60) {	//if fixedTruckVoluem / requireddischargerate is greater than hours to Perish, this
				//means that total deliveries should depend on  Hours to perish. Otherwise total deliveris shoud depend on requireddischargerate. 
			totalDeliveries = order.getRequiredTotalVolume()/(this.order.getRequiredDischargePerHour()*GlobalParameters.MINUTES_TO_PERISH_CONCRETE/60); //every thing is in thousands
				remainingVolume = 0;
				//logger.debug(order.getId() + "O: Total deliveries depending on PerishablRate are: " + totalDeliveries);
				if ((float)order.getRequiredTotalVolume()%(this.order.getRequiredDischargePerHour()*GlobalParameters.MINUTES_TO_PERISH_CONCRETE/60) > 0) {
					totalDeliveries += 1;
					remainingVolume = order.getRequiredTotalVolume()%(this.order.getRequiredDischargePerHour()*GlobalParameters.MINUTES_TO_PERISH_CONCRETE/60);
				}
				//logger.debug(order.getId() + "O: Total deliveries depending on Persihable rate, //after adding remaining are: " + totalDeliveries);
			}
			else { //totalDeliveris shud depend on requredDischargeRate by order
				totalDeliveries = order.getRequiredTotalVolume()/this.order.getRequiredDischargePerHour(); //every thing is in thousands
				remainingVolume = 0;
				//logger.debug(order.getId() + "O: Total deliveries are: " + totalDeliveries);
				if ((float)order.getRequiredTotalVolume()%this.order.getRequiredDischargePerHour() > 0) {
					totalDeliveries += 1;
					remainingVolume = order.getRequiredTotalVolume()%this.order.getRequiredDischargePerHour();
				}
				//logger.debug(order.getId() + "O: Total deliveries after adding remaining are: " + totalDeliveries);
				
			}
				for (int i = 0; i < totalDeliveries; i ++) {
					Delivery d = new Delivery(this, null, 0, null, null, this);
					//logger.debug(order.getId() + "O: order.getReqDischargePerHour = " + order.getRequiredDischargePerHour() + " & fixed truck volume = " + (long)om.getFixedTruckVolume());
					//logger.debug(" unloading duration =  " + (long)(((float)om.getFixedTruckVolume()/(float)order.getRequiredDischargePerHour())));//*60*60*1000)); //converting to hours to milliseconds
					
					if (i == totalDeliveries -1 && remainingVolume != 0) { //for last delivery
//						d.setLastDelivery(true);
						d.setDeliveredVolume(remainingVolume);
						d.setUnloadingDuration(new Duration((long)(((float)remainingVolume/(float)order.getRequiredDischargePerHour())*60*60*1000)));
					}
					else { //for other then last deliveries
//						d.setLastDelivery(false);
						d.setDeliveredVolume((GlobalParameters.MINUTES_TO_PERISH_CONCRETE/60) * this.order.getRequiredDischargePerHour()); //so each delivery should deliver this amount of volume..
						d.setUnloadingDuration(new Duration((long)((float)GlobalParameters.MINUTES_TO_PERISH_CONCRETE*60*1000))); //so normal duration will become the duration when concrete could be delivered correctly.
						//The rest of the concrete that will be wastage should be cleared at loading time for the next delivery. 
						//logger.debug(order.getId() + "O: Minutes to Perish = " + (float)GlobalVariables.MINUTES_TO_PERISH_CONCRETE/60 + 
							//	" and w.r.t DischargeRate = " + (float)d.getDeliveredVolume()/this.order.getRequiredDischargePerHour());
					}
					deliveries.add(d);
				}
			}
		}	
	}
	
	/**
	 * @description Called only for the first time. 
	 * 
	 * After execution, order should be registered to OM and the deliveries should have been created according to the fixed capacity of the truck
	 */
	private void initialize() {
		deliveriesByOrdersDischargeRate();
		if (deliveries.size() > 0 && deliveriesReady == false) {
			deliveriesReady = true;
			logger.debug(order.getId() + "O: Deliveries Ready & total deliveries are = " + deliveries.size() + "StartTime = " + order.getStartTime());
		}
	}
	
	/**
	 * @Description sets Order.allDeliveriesReserved = true, if all the deliveries of Order are reserved.
	 */
	private void checkAllDeliveriesReservation() {
		if (!deliveries.isEmpty()) {
			this.allDeliveriesReserved = true;
			for (Delivery d: deliveries) {
				if (d.isReserved() == false) {
					this.allDeliveriesReserved = false;
					break;
				}
			} //logger.debug(order.getId() + "O: allDeliveriesReserved = " + this.allDeliveriesReserved);
		}
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;
	}

	@Override
	public Point getPosition() {
		return this.orderSite.getPosition();
	}

	@Override
	public double getRadius() {
		return 0;
	}

	@Override
	public double getReliability() {
		return 1;
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}
	
	public OrderSite getOrderSite() {
		return orderSite;
	}

	public Order getOrder() {
		return order;
	}

	public boolean isOrderReserved() {
		return orderReserved;
	}
	
	public ArrayList<Delivery> getDeliveries() {
		return this.deliveries;
	}
	
	public RmcDelivery getDeliveryForDomainDelivery(Delivery pdelivery){
		if (parcelDeliveries.size() > 0) {
			for (RmcDelivery rmcDel: parcelDeliveries) {
				if (rmcDel.getDelivery().equals(pdelivery)) {
					return rmcDel;
				}
			}
		}
		return null;
	}
	

	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
