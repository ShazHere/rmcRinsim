package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckCostForDelivery;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.CommitmentAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.OrderStrategyCoalition;

public class OrderStateTeamNeed extends OrderAgentState {
	
	OrderStrategyCoalition strategy;
	private Delivery failedDelivery;
	DateTime assignDeliveryTime;
	private final Logger logger; //for logging


	public OrderStateTeamNeed(OrderAgentInitial orderAgent) {
		super(orderAgent);
		checkArgument(orderAgent.getStrategy() instanceof OrderStrategyCoalition,true);
		strategy = (OrderStrategyCoalition)orderAgent.getStrategy();
		logger = Logger.getLogger(OrderStateTeamNeed.class);

	}

	@Override
	public int getStateCode() {
		return OrderAgentState.TEAM_NEED;
	}

	@Override
	public void processExplorationAnts(OrderAgentPlan orderPlan, long startTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processIntentionAnts(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		refreshDeliveryBookings(orderPlan, timeLapse);
		
	}

	@Override
	public void doSpecial(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		if (failedDelivery == null) {
			failedDelivery = orderPlan.getFailedDelivery(timeLapse.getStartTime());
			checkArgument(failedDelivery != null, true);
			TimeSlot ts = orderPlan.getTimeWindowFailedDelivery(failedDelivery);//time slot at order, w.r.t time of delivery
			
			DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
			logger.info(orderAgent.getOrder().getId() + "O Delivery Failure detected; DeliveryNO = " + failedDelivery.getDeliveryNo() + ", Order in TeamNeed! ");
			strategy.getTc().removeMember((DeliveryTruckInitial) failedDelivery.getTruck());
			TruckCostForDelivery cost = strategy.getTc().getMostSuitableTruck(failedDelivery, ts, currTime);
			boolean deliveryAssigned = false;
			if (cost != null) 
				if (assignDelivery(orderPlan, cost, currTime) == true)
					deliveryAssigned = true;
			if (deliveryAssigned){
				orderPlan.removeDelivery(failedDelivery);
				logger.info(orderAgent.getOrder().getId() + "O Delivery Failure handled (JI); DeliveryNO = " + failedDelivery.getDeliveryNo());
			}
			else // cost is null or assignDelivery was unsuccessful
				normalDelMASWay(orderPlan, timeLapse.getStartTime());
		}
	}

	private boolean assignDelivery(OrderAgentPlan orderPlan,TruckCostForDelivery cost, DateTime currTime) {
		//if (orderPlan.isOrderDeliveryingStarted(currTime)== false) {
			if (cost.getDeliveryCost() <= 3) {
				CommitmentAnt cAnt = new CommitmentAnt(orderAgent, currTime, cost.getTruckDeliveryUnit());
				putInOrderPlan(orderPlan, cost.getTruckDeliveryUnit().getDelivery(), currTime);
				orderAgent.getcApi().send(cost.getTruckAgent(), cAnt);
				return true;
			}
			else { //think about resetPlan..or what to do?
				;
			}
//	}
//		else { //means order has already been started to be delivered
//			if (cost.getDeliveryCost() <= 3){
//				//assign delivery
//				//send commitment ant
//			}
//			else {
//				; //think how to deal  with cost 3 & > 4..
//				//may be first check how much this situation occurs..
//			}
//		}
		return false;
	}
	private void putInOrderPlan(OrderAgentPlan orderPlan, Delivery del, DateTime currTime) {
		orderPlan.acceptIntentionArangementInOrder(del, currTime, true);
		assignDeliveryTime = currTime;
	}
	@Override
	public void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
		// TODO Should orderPlan be changed in Team need state?

	}

	@Override
	public void checkDeliveryStatuses(OrderAgentPlan orderPlan, long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (assignDeliveryTime == null)
			;//will be already doing as  normal DelMas
		else if (orderPlan.areDeliveriesRefreshing(startTime)// && orderPlan.calculateRemainingVolume() == 0 
				&& assignDeliveryTime.plusSeconds(GlobalParameters.INTENTION_INTERVAL_SEC).compareTo(currTime) < 0 ) {
			checkArgument (orderPlan.calculateRemainingVolume() == 0, true);
			orderAgent.setOrderState(OrderAgentState.BOOKED);
			logger.info(orderAgent.getOrder().getId() + "O delivery assignig to TEAM MEMBER completed");
		}
		//else if (assignDeliveryTime.plusSeconds(GlobalParameters.INTENTION_INTERVAL_SEC).compareTo(currTime) < 0 ){
			//checkArgument(false,true);//i think this should not occur..i hope so..:P
//do as in normalDelMAS
		//}
		//else seems it will staty in OrderStateTeam
	}
	private void normalDelMASWay(OrderAgentPlan orderPlan, long startTime) {
		if (!orderPlan.areDeliveriesRefreshing(startTime)){
			orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
			if (!makeOrderPlanAdjustment(orderPlan, startTime))
				orderAgent.setOrderState(OrderAgentState.UNDELIVERABLE);
		}
	}
	@Override
	public String toString(){
		return "TEAM_NEED";
	}
}
