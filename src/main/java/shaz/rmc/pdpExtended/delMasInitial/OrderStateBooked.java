/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;


import org.joda.time.DateTime;
import static com.google.common.base.Preconditions.checkArgument;


import rinde.sim.core.TimeLapse;
import rinde.sim.core.model.pdp.PDPModel.ParcelState;

/**
 * @author Shaza
 *
 */
public class OrderStateBooked extends OrderAgentState {
	boolean checkedCorrectlyBooked = false;

	public OrderStateBooked(OrderAgentInitial orderAgent) {
		super(orderAgent);
	}
	@Override
	public
	int getStateCode() {
		return OrderAgentState.BOOKED;
	}
	
	@Override
	public void processExplorationAnts(OrderAgentPlan orderPlan,long startTime) {
	//If order is fully booked, but still explorations are there then ignore 
		explorationAnts.clear();
	}
	
	@Override
	public void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		//already order booked, no need to send feasibility info now.
		return;
	}
	@Override
	public void processIntentionAnts(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		refreshDeliveryBookings(orderPlan, timeLapse);
	}
	@Override
	public void changeOrderPlan(OrderAgentPlan orderPlan, long startTime) {
		if(checkedCorrectlyBooked == false) {
			DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
			if (orderAgent.getDeliveryForDomainDelivery(orderPlan.getDeliveries().get(0)) != null) {
				if (orderAgent.getPDPModel().getParcelState(orderAgent.getDeliveryForDomainDelivery(orderPlan.getDeliveries().get(0))) == ParcelState.IN_CARGO
				|| orderAgent.getPDPModel().getParcelState(orderAgent.getDeliveryForDomainDelivery(orderPlan.getDeliveries().get(0))) == ParcelState.PICKING_UP
				|| orderAgent.getPDPModel().getParcelState(orderAgent.getDeliveryForDomainDelivery(orderPlan.getDeliveries().get(0))) == ParcelState.DELIVERING
				|| orderAgent.getPDPModel().getParcelState(orderAgent.getDeliveryForDomainDelivery(orderPlan.getDeliveries().get(0))) == ParcelState.DELIVERED)
					checkedCorrectlyBooked = true; //since it has come from TeamNeed to this state so no need to check it
			}
			else
				checkArgument(currTime.compareTo(orderPlan.getDeliveries().get(0).getLoadingTime()) < 0,true);
			checkedCorrectlyBooked = true;
		}//if this check is caught too often then it means there is need to re-visit different interval times and 'MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED' values.
		
	}
	@Override
	public void checkDeliveryStatuses(OrderAgentPlan orderPlan, long startTime) {
		if (!orderPlan.areDeliveriesRefreshing(startTime)){
			orderAgent.setOrderState(OrderAgentState.IN_PROCESS);
			if (!makeOrderPlanAdjustment(orderPlan, startTime))
				orderAgent.setOrderState(OrderAgentState.UNDELIVERABLE);
		}
	}
	@Override
	public void doSpecial(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public String toString(){
		return "BOOKED";
	}
}
