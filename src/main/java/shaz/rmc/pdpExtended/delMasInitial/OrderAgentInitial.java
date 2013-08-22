/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
import rinde.sim.core.model.pdp.PDPModel.ParcelState;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.SimulationViewer;
import shaz.rmc.core.Agent;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.ResultElementsOrder;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Order;
import shaz.rmc.core.domain.Station.StationBookingUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.BreakAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.CommitmentAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.FeaAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 * Represents the agent deployed to handle a single order. 
 * 
 * Agent has different behaviour based on its current state.
 * The private members are those that require tracking throughtout agent lifecycle.  
 */
public class OrderAgentInitial  extends Depot implements Agent {
	private RoadModel roadModel;
	private PDPModel pdpModel;
	private final Simulator sim;
	private final Point Location;
	private final Order order;
	//private final ArrayList<Delivery> deliveries; //virtual deliveries..twhich stores general information of the deliveries that have been intended by other trucks
	
	private final Logger logger; //for logging
	private OrderAgentPlan orderPlan;

	private ArrayList<BreakAnt> breakAnts;
	private ArrayList<CommitmentAnt> commitmentAnts;
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	
	protected DateTime timeForLastFeaAnt; 	/** to keep track of feasibility interval */
	private final DateTime maximumPossibleStartTime; /// according to total quantity and end of simulation time and truck capacity
	
	private ArrayList<DeliveryInitial> parcelDeliveries; //physical deliveries, to be created just before delivery (5min b4 actual delivery needed to b picked up)
	private ArrayList<ProductionSiteInitial> sites; //all sites information
	
	private ArrayList<ProductionSiteInitial> possibleSites; //sites from which this order could receive delivery according to PERISH time
	
	private OrderAgentState state;
	public OrderAgentInitial(Simulator pSim, Point pLocation, Order pOrder ) {
		super();
		Location = pLocation;
		super.setStartPosition(Location);
		order = pOrder;
		sim = pSim;
		logger = Logger.getLogger(OrderAgentInitial.class);
		
		setOrderState(OrderAgentState.IN_PROCESS);
		mailbox = new Mailbox();
		timeForLastFeaAnt = new DateTime(0);
		
		breakAnts = new ArrayList<BreakAnt>();
		commitmentAnts = new ArrayList<CommitmentAnt>();
		//deliveries = new ArrayList<Delivery>();
		parcelDeliveries = new ArrayList<DeliveryInitial>();
		orderPlan = new OrderAgentPlan(new Duration(0), this, GlobalParameters.START_DATETIME);
		maximumPossibleStartTime = makeMaximumPossibleStartTime();

	}
	private DateTime makeMaximumPossibleStartTime() {
		int possibleDeliveries =  (int)Math.ceil(((double)order.getRequiredTotalVolume()) / GlobalParameters.FIXED_CAPACITY);
		DateTime maxST = GlobalParameters.END_DATETIME.minusHours((int) (possibleDeliveries*(1)));

		return maxST;
	}
	@Override
	public void tick(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getStartTime());
		state.processExplorationAnts(orderPlan, timeLapse.getStartTime()); 
		state.processIntentionAnts(orderPlan, timeLapse);
		orderPlan.generateParcelDeliveries(this, timeLapse.getStartTime()); 
		state.sendFeasibilityInfo(orderPlan, timeLapse.getStartTime());
		state.changeOrderPlan(orderPlan, timeLapse.getStartTime());
		state.checkDeliveryStatuses(orderPlan, timeLapse.getStartTime());
//		if (GlobalParameters.ENABLE_JI)
//			processBreakAnts(timeLapse);
//		else //means JI isn't enabled so DelMAS should handle it independently
//			processBreakAntDelMAS(timeLapse);
		checkIfOrderServed(timeLapse);
	}
	/**
	 * @param timeLapse
	 */
	protected void checkIfOrderServed(TimeLapse timeLapse) {
		if (parcelDeliveries.isEmpty() == false && !(state instanceof OrderStateServed)) {
			Collection<Parcel> deliveredDeliveries = pdpModel.getParcels(ParcelState.DELIVERED);
			ArrayList<DeliveryInitial> orderDeliveries = new ArrayList<DeliveryInitial>();
			for(Parcel pd: deliveredDeliveries) {
				if (((DeliveryInitial)pd).getOrder().getOrder().getId().equals(this.getOrder().getId()))
					orderDeliveries.add((DeliveryInitial)pd);
			}
			if (orderDeliveries.size() == parcelDeliveries.size()) {
				logger.info(order.getId() + "O FULLY SERVED  and CURRENT TIME is = "  +  new DateTime(GlobalParameters.START_DATETIME.getMillis() + timeLapse.getStartTime())  );
				setOrderState(OrderAgentState.SERVED); 
			}
		}
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {}

	
	/**
	 * Send feasiblility info to only those PS, that comes within range according to concrete Perish limit
	 */ //TODO may be move to orderAgentState
	protected void sendFAntToPS() {
		FeaAnt fAnt = new FeaAnt(this, orderPlan.getInterestedTime()); //the distance is calculated when ant reaches the PS
		for (ProductionSiteInitial p : possibleSites) {
			//if (new Duration ((long)((Point.distance(p.getPosition(), this.getPosition())/GlobalParameters.TRUCK_SPEED)*60l*60l*1000l)).getStandardMinutes() <= GlobalParameters.MINUTES_TO_PERISH_CONCRETE  )
				cApi.send(p, fAnt);
		}
	}
	
	/**
	 * makes new orderPlan, by changing ST delay
	 * @param currTime 
	 */
	protected void makeNewOrderPlan(DateTime currTime) { 
		Duration dur = new Duration(GlobalParameters.MINUTES_TO_DELAY_ST * 60 * 1000);
		orderPlan = new OrderAgentPlan(orderPlan.getDelayStartTime().plus(dur), this, currTime);
		parcelDeliveries = new ArrayList<DeliveryInitial>();
		logger.info(order.getId() + "O NewOrder Plan StDelay = " + orderPlan.getDelayStartTime().getStandardMinutes());
	}
	
	/**
	 * @param currTime
	 * @return true if according to maximumPossibleStartTime and Global.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED the Order could be served..
	 */
	protected boolean isOrderDeliverable(DateTime currTime) {
		if (currTime.plusMinutes(GlobalParameters.MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED).compareTo(maximumPossibleStartTime) < 0)
			return true;
		else 
			return false;
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
	protected int getTimeForLastFeaAntInMin() { 
		return timeForLastFeaAnt.getMinuteOfDay();
	}
	protected void setTimeForLastFeaAnt(DateTime timeForLastFeaAnt) {
		this.timeForLastFeaAnt = timeForLastFeaAnt;
	}
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		this.roadModel = pRoadModel;
		this.pdpModel = pPdpModel;
		sites = new ArrayList<ProductionSiteInitial>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
		possibleSites = new ArrayList<ProductionSiteInitial>();
		for (ProductionSiteInitial p : sites) {
			if (new Duration ((long)((Point.distance(p.getPosition(), this.getPosition())/GlobalParameters.TRUCK_SPEED)*60l*60l*1000l)).getStandardMinutes() <= GlobalParameters.MINUTES_TO_PERISH_CONCRETE  )
				possibleSites.add(p);
		}
	}
	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0) {
			for (Message m : messages) {
				if (m.getClass() == ExpAnt.class) {
					state.addExpAnt((ExpAnt)m);
				}
				else if (m.getClass() == IntAnt.class) {
					state.addIntAnt((IntAnt)m);
				}		
				else if (m.getClass() == BreakAnt.class) {
					logger.info(order.getId() + "O BREAKDOWN signal received");
					this.breakAnts.add((BreakAnt)m);
				}
				else if (m.getClass() == CommitmentAnt.class) {
					logger.info(order.getId() + "O COMMITMENT reply received");
					this.commitmentAnts.add((CommitmentAnt)m);
				}
			}
		}
	}
	
	public ResultElementsOrder getOrderResult(ArrayList<Integer> truckCapacities) { //truck capacities
		int deliveredConcrete = 0;
		if (orderPlan.getDeliveries().size() > 0) {
			for (Delivery d : orderPlan.getDeliveries()) { //there might b some error in this calculation.
				DeliveryInitial di = this.getDeliveryForDomainDelivery(d);
				if (di != null) {
					deliveredConcrete += di.getDelivery().getDeliveredVolume();
				}
			}
			return new ResultElementsOrder(new ArrayList<Delivery>(orderPlan.getDeliveries()), this.order.getRequiredTotalVolume(), 
					deliveredConcrete, this.recordHourlyConcrete(), this.order.getEarliestStartTime(), getExpectedWastedConcrete(truckCapacities), getActualWastedConcrete(), orderPlan.getDelayStartTimeInMin());
		}
		//else if no deliveries..means order wasn't delivered.
		return new ResultElementsOrder(null, this.order.getRequiredTotalVolume(), deliveredConcrete, null, this.order.getEarliestStartTime(), 
				getExpectedWastedConcrete(truckCapacities), getActualWastedConcrete(), 0);
	}
	/**
	 * @return array of 24 elements, where each integer specifies amount of concrete delivered in the index-1th hour
	 */
	private int[] recordHourlyConcrete() {
		int hoursConcrete[] = new int[24];
		if (orderPlan.getDeliveries().size() < 0)
			return null;
		int index = 0;
		for (DeliveryInitial di : this.parcelDeliveries) {
			index = di.getDelivery().getDeliveryTime().minusMinutes(1).getHourOfDay();
			hoursConcrete[index] = di.getDelivery().getDeliveredVolume();
		}
		return hoursConcrete;
	}
	/**
	 * @return actual wasted concrete = sum of wasted concrete by all deliveries
	 */
	private int getActualWastedConcrete() {
		int wastedConcrete = 0;
		if (orderPlan.getDeliveries().size() < 0)
			return 0;
		for (DeliveryInitial di : this.parcelDeliveries) {
			wastedConcrete += di.getDelivery().getWastedVolume();
		}
		return wastedConcrete;
	}
	/**
	 * @return the concrete that is expected to be wasted anyway, even if smallest size truck is used
	 *  for last delivery.  
	 */
	private int getExpectedWastedConcrete(ArrayList<Integer> truckCapacities) {
		Collections.sort(truckCapacities, new Comparator<Integer>(){
	        public int compare( Integer a, Integer b ){ //sort in ascending order based on schedule score
	            return a - b;
	        }
		});
		return truckCapacities.get(0) - this.order.getRequiredTotalVolume()%truckCapacities.get(0);
		
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
	
//	private void checkStartTimeDelayRequirement() {
//		if (this.remainingToBookVolume > 0 && this.refreshTimes.get(deliveries.get(deliveries.size()-1)).plusHours(1) < currentTime)
//			this.delayStartTime = new Duration(3600000); //delay one hour
//			
//	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}
	public ArrayList<ProductionSiteInitial> getSites() {
		return sites;
	}
	public Order getOrder() {
		return order;
	}
	public CommunicationAPI getcApi() {
		return cApi;
	}
	
	protected int getOrderState() {
		return this.state.getStateCode();
	}
	protected void setOrderState (int newState) {
		this.state = OrderAgentState.newState(newState, this);
	}
	protected boolean registerParcel (DeliveryInitial pDel) {
		sim.register(pDel);
		parcelDeliveries.add(pDel);
		return true;
	}

	@Override
	public int getId() {
		return Integer.getInteger(order.getId());
	}
	@Override
	public String toString() {
		return "OrderAgentInitial [ "
				+ "order=" + order.toString() + "\ndeliveries=" + orderPlan.getDeliveries().size()
	//			+ ", timeForLastFeaAnt=" + timeForLastFeaAnt
				+ ", state=" + state
				+ ",\ninterestedTime=" + orderPlan.getInterestedTime()
				+ ", interestedDeliveryNo=" + orderPlan.getInterestedDeliveryNo()
				+ ", delayFromStartTime="
				+ orderPlan.getDelayStartTime() + ", remainingToBookVolume="
				+ orderPlan.getRemainingToBookVolume() + ", parcelDeliveries="
				+ parcelDeliveries + ", orderState="
				+ getOrderState() + "]";
	}
	
}
