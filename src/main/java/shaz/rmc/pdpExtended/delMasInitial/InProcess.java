/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.math3.random.RandomDataImpl;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import shaz.rmc.core.ProductionSite;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;

/**
 * @author Shaza
 *
 */
public class InProcess extends OrderAgentState {


	public InProcess(OrderAgentInitial orderAgent) {
		super(orderAgent);
	}

	@Override
	int getStateCode() {
		return OrderAgentState.IN_PROCESS;
	}
	
	@Override
	protected void processExplorationAnts(OrderAgentPlan orderPlan, long startTime) {
		if (explorationAnts.isEmpty()) //if no exploration has been sent by trucks, then return
			return;
		Iterator<ExpAnt> i = explorationAnts.iterator();
		logger.debug(orderAgent.getOrder().getId() + "O Checking exp ants total are= " + explorationAnts.size() );
		while (i.hasNext()) { 
			ExpAnt exp = i.next(); //i guess once reached to order, it shud just include order in its list, since order was in general ok
			long psToCyDur = (long)((Point.distance(exp.getSender().getPosition(), orderAgent.getPosition())/exp.getTruckSpeed())*60*60*1000); //hours to milli
			if (exp.getCurrentInterestedTime().minus(exp.getCurrentLagTime()).equals(orderPlan.getInterestedTime().minus(psToCyDur).minusMinutes(GlobalParameters.LOADING_MINUTES)) ) {
				logger.debug(orderAgent.getOrder().getId() + "O expStarTim = " + exp.getCurrentInterestedTime() + " & interestedTim = " + orderPlan.getInterestedTime() + " loadingTime = " + orderPlan.getInterestedTime().minus(psToCyDur).minusMinutes(GlobalParameters.LOADING_MINUTES));	
				Delivery del = prepareNewDelivery(orderAgent, orderPlan, exp, psToCyDur);
				sendToPs(orderAgent, exp, del);
			}
			i.remove(); //exp should die
		} //end while (i.hasNext())
		checkArgument (explorationAnts.isEmpty(), true);
	}
	
	/**
	 * @param pOrderAgent
	 * @param pDeliveryNo
	 * @param exp
	 * @param pDeliveryTime
	 * @param travelDur
	 * @return newly prepared deliery, it also sets the currentWastedConcrete in expAnt
	 */
	private Delivery prepareNewDelivery(OrderAgentInitial pOrderAgent,OrderAgentPlan pOrderPlan,  ExpAnt exp, long travelDur) {
		
		Delivery.Builder delBuilder = new Delivery.Builder();
		delBuilder.setOrder(pOrderAgent);
		delBuilder.setDeliveryNo(pOrderPlan.getInterestedDeliveryNo());
		delBuilder.setTruck(exp.getOriginator());
		delBuilder.setDeliveredVolume((int)(exp.getOriginator().getCapacity())); //Shouldn't it be accoriding to remainig volume..?
	
		Duration unLoadingDuration; //SET LOADIing and unloading durations
		if (pOrderPlan.getRemainingToBookVolume() < (int)(exp.getOriginator().getCapacity())) { //if remaining volume is less but truck capacity is higher
			int wastedVolume = (int)(exp.getOriginator().getCapacity() - pOrderPlan.getRemainingToBookVolume());
			delBuilder.setWastedVolume(wastedVolume);
			exp.setCurrentWastedConcrete(wastedVolume);
			unLoadingDuration = new Duration((pOrderPlan.getRemainingToBookVolume() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR));
			//delBuilder.setUnloadingDuration(new Duration((remainingToBookVolume * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)));
		}
		else {
			delBuilder.setWastedVolume(0);// no wastage 
			exp.setCurrentWastedConcrete(0); // no wastage
			unLoadingDuration = new Duration((long)(exp.getOriginator().getCapacity()* 60l*60l*1000l / GlobalParameters.DISCHARGE_RATE_PERHOUR) );
			//delBuilder.setUnloadingDuration(new Duration((long)(exp.getOriginator().getCapacity()* 60l*60l*1000l / GlobalParameters.DISCHARGE_RATE_PERHOUR) ));
		}
		delBuilder.setUnloadingDuration(unLoadingDuration);
		delBuilder.setLoadingDuration(new Duration(GlobalParameters.LOADING_MINUTES*60l*1000l));
		
		
		delBuilder.setStationToCYTravelTime(new Duration (travelDur));

		delBuilder.setDeliveryTime(pOrderPlan.getInterestedTime().plus(exp.getCurrentLagTime()));
////			if (this.deliveries.size() == 0 ) //means at the moment decesions are for first delivery so ST shud b included
////				{ 
////					//checkArgument(pDeliveryNo == 0, true);
////					delBuilder.setLagTime(this.delayFromActualInterestedTime.plus(this.delayStartTime));
////				}
////			else
////				delBuilder.setLagTime(this.delayFromActualInterestedTime); //no ST added this time
////	
//		delBuilder.setLagTime(exp.getCurrentLagTime());
		
		delBuilder.setLoadingStation((ProductionSite)exp.getSender());
		Delivery del = delBuilder.build();
		return del;
	}
	private void sendToPs(OrderAgentInitial pOrderAgent,ExpAnt exp, Delivery del) {
		ExpAnt newExp = (ExpAnt)exp.clone(pOrderAgent);
		if (newExp.makeCurrentUnit(del)) {
			logger.debug(pOrderAgent.getOrder().getId() + "O delivery added in expAnts schedule, orginator = " + newExp.getOriginator().getId());
			ProductionSiteInitial selectedPs; //For selecting the return PS
			selectedPs = selectPsToSend(pOrderAgent);
			pOrderAgent.getcApi().send(selectedPs, newExp);
		}
		else
			logger.debug(pOrderAgent.getOrder().getId() + "O FAILURE in adding delivery in expSchedule");
	}

	/**
	 * @param pOrderAgent
	 * @return selected Ps
	 */
	private ProductionSiteInitial selectPsToSend(OrderAgentInitial pOrderAgent) {
		ProductionSiteInitial selectedPs;
		if (pOrderAgent.getSites().size()>1) 				//select the next pC to visit at random..
			selectedPs = pOrderAgent.getSites().get(new RandomDataImpl().nextInt(0, pOrderAgent.getSites().size()-1));
		else
			selectedPs = pOrderAgent.getSites().get(0);
		return selectedPs;
	}
	
	
	@Override
	protected void sendFeasibilityInfo(OrderAgentPlan orderPlan, long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (isFeasibilityIntervalPassed(currTime) ){
			//setOrderInterests();
			orderAgent.sendFAntToPS();
			orderAgent.setTimeForLastFeaAnt(currTime);
		}	
	}

	
	
	@Override
	protected void processIntentionAnts(OrderAgentPlan orderPlan, TimeLapse timeLapse) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		if ( intentionAnts.isEmpty()) 
			return;
		sortIntentionArts(); 
		Iterator<IntAnt> i = intentionAnts.iterator();
		while (i.hasNext()) { //at the moment just select the first one
			IntAnt iAnt = i.next();
			if (newIntentionAccordingToCurrentInterest(orderPlan, iAnt)) {
				iAnt.getCurrentUnit().setOrderReply(getIAntReply(orderPlan, currTime, iAnt));
			}
			else { //order isn't interested, yet it could be refreshing of a previous booking
				Delivery d = deliveryExists(orderPlan, iAnt.getCurrentUnit().getDelivery());
				if (refreshBooking(orderPlan, currTime, iAnt, d)) { //so its not just recently added delivery. second condition is added since there could be 2 intention ants from same truck
					//else we shouldn't touch order state
					orderPlan.putInRefreshTimes(iAnt.getCurrentUnit().getDelivery(), currTime);
					iAnt.getCurrentUnit().setOrderReply(Reply.WEEK_ACCEPT); //So earlier it could be UnderProcess, but once confirmed, its Weekly accepted
					logger.debug(orderAgent.getOrder().getId() + "O int-" + iAnt.getOriginator().getId()+" booking refreshed");
				}
				else //now its sure order isn't interested!
					iAnt.getCurrentUnit().setOrderReply(Reply.REJECT);
			}
			sendAccordingToNextUnit(currTime, iAnt);
			i.remove();
		}
	}
	/**
	 * @param iAnt
	 * @return
	 */
	private boolean newIntentionAccordingToCurrentInterest(OrderAgentPlan orderPlan, IntAnt iAnt) {
		return iAnt.getCurrentUnit().getDelivery().getDeliveryTime().minus(iAnt.getCurrentUnit().getTunit().getLagTime()).equals(orderPlan.getInterestedTime()) //so iAnt is according to order's current interest or according to currentInterest + lagTime
				//|| (iAnt.getCurrentUnit().getDelivery().getDeliveryTime().compareTo(this.interestedTime ) > 0 && iAnt.getCurrentUnit().getDelivery().getDeliveryTime().compareTo(this.interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES) ) <= 0))
				&& iAnt.getCurrentUnit().getTunit().getDelivery().getDeliveryNo() == orderPlan.getInterestedDeliveryNo() 
				&& iAnt.getCurrentUnit().isAddedInTruckSchedule() == false;
	}
	/**
	 * @param currTime
	 * @param iAnt
	 * @return 
	 */
	private Reply getIAntReply(OrderAgentPlan orderPlan, DateTime currTime, IntAnt iAnt) {
		if (orderPlan.getRefreshTimes().containsKey(iAnt.getCurrentUnit().getDelivery()) == false ) {//refreshTimes.get(iAnt.getCurrentUnit().getDelivery().getDeliveryNo()).equals(currTime) == false) {
			if (iAnt.getCurrentUnit().getPsReply() == Reply.UNDER_PROCESS) { //PS is ok with this delivery
					checkArgument(iAnt.getCurrentUnit().isAddedInTruckSchedule() == false, true);
					orderPlan.acceptIntentionArangementInOrder(iAnt, currTime);
					orderAgent.setOrderState(OrderAgentState.WAITING);
					return Reply.UNDER_PROCESS;
			}
		} 
			return Reply.REJECT;
	}
}
