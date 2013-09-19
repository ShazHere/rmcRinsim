/**
 * 
 */
package shaz.rmc.core;

import static com.google.common.base.Preconditions.checkArgument;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.graph.Point;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;

/**
 * @author Shaza
 *
 */
public class ScheduleHelper {
	
	private static double truckSpeed = GlobalParameters.TRUCK_SPEED;

	/**
	 * @param currentSlot
	 * @param actualInterestedTime
	 * @return
	 */
	static public Duration makeLagTime(AvailableSlot currentSlot,DateTime actualInterestedTime, ProductionSiteInitial pS) {
		Duration durationLastOrderToPS = new Duration(0);
		if (currentSlot.getLastOrderVisited() != null) {
			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
		}
		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1); 
		//currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0
		return new Duration (lastOrderToPSJustBeforeActualInterstedTime, currentSlot.getStartTime() );
	}
	/**
	 * @param currentInterestedTime 
	 * @param originator 
	 * @param currentSlot
	 * @param pOr current order under consideration
	 * @return the TruckTravelUnit to travel from last order to current PS. Should be called 
	 * - after setting currentTnterestedTime and currentLagTime
	 * - if isInterestedWithLagTime(..) or isInterestedAtExactTime(..) is true
	 */ //TODO: change accroding to utility.getTravelUnitTimeSlot..
	static public TruckTravelUnit makeNextOrderTravelUnit(DeliveryTruckInitial originator, DateTime currentInterestedTime, AvailableSlot currentSlot, OrderAgentInitial pOr) { 
		if (currentSlot.getPS4NextOrderVisitedd() == null)
			return null;
		Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
		Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
		TimeSlot travelSlot =  new TimeSlot( currentSlot.getEndTime().minus(durationOrderToPS4NextOrder).minusMinutes(1), currentSlot.getEndTime().minusMinutes(1));
		TruckTravelUnit ttu = new TruckTravelUnit(originator, travelSlot, pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition(), durationOrderToPS4NextOrder );
		checkArgument(currentInterestedTime.compareTo(ttu.getTimeSlot().getStartTime()) < 0, true);
		return ttu;	
	}
	/**
	 * @param currentInterestedTime 
	 * @param originator 
	 * @param currentSlot
	 * @param pS
	 * @return the TruckTravelUnit to travel from last order to current PS. Should be called 
	 * - after setting currentTnterestedTime and currentLagTime
	 * - if isInterestedWithLagTime(..) or isInterestedAtExactTime(..) is true
	 */
	static public TruckTravelUnit makeLastOrderTravelUnit(DeliveryTruckInitial originator, DateTime currentInterestedTime, AvailableSlot currentSlot, ProductionSiteInitial pS) {
		if (currentSlot.getLastOrderVisited() == null)
			return null;
		Double distanceLastOrderToPS = Point.distance( currentSlot.getLastOrderVisited().getPosition(), pS.getPosition());
		Duration durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
		TimeSlot travelSlot =  new TimeSlot(currentInterestedTime.minus(durationLastOrderToPS).minusMinutes(1), currentInterestedTime.minusMinutes(1));
		TruckTravelUnit ttu = new TruckTravelUnit(originator, travelSlot, currentSlot.getLastOrderVisited().getPosition(), pS.getPosition(),durationLastOrderToPS );
		checkArgument(currentInterestedTime.compareTo(ttu.getTimeSlot().getStartTime()) > 0, true);
		return ttu;
	}
	
	/**
	 * @param originator 
	 * @param interestedTime
	 * @param travelTime
	 * @param currentSlot
	 * @param actualInterestedTime
	 * @return
	 */
	static public boolean isInterestedWithLagTime(DeliveryTruckInitial originator, DateTime interestedTime, Duration travelTime, AvailableSlot currentSlot,
			DateTime actualInterestedTime, ProductionSiteInitial pS, OrderAgentInitial pOr) {
		Duration durationLastOrderToPS = new Duration(0);
		if (currentSlot.getLastOrderVisited() != null) {
			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
		}
		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1); 
		DateTime currentSlotEndTime = currentSlot.getEndTime();
		if (currentSlot.getPS4NextOrderVisitedd() != null) {
			Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
			Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
			currentSlotEndTime = currentSlotEndTime.minus(durationOrderToPS4NextOrder).minusMinutes(1);
		}
		return currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0 //making rough estimation that will order enoughÊ interesting to be visited
				&& currentSlotEndTime.compareTo(interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES).plus(getCurrentOrderEstimatedTravelTimeAndUnloadingTime(travelTime, originator))) > 0;
//		return currentSlot.getStartTime().compareTo(actualInterestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES))< 0 //making rough estimation that will order enoughÊ interesting to be visited
//				&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(GlobalParameters.MAX_LAG_TIME_MINUTES).plusMinutes(CAUTION_MINUTES).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0;
	}
	/**
	 * @param originator 
	 * @param interestedTime
	 * @param travelTime
	 * @param currentSlot
	 * @param actualInterestedTime
	 * @return
	 */
	static public boolean isInterestedAtExactTime(DeliveryTruckInitial originator, DateTime interestedTime, Duration travelTime, AvailableSlot currentSlot,
			DateTime actualInterestedTime, ProductionSiteInitial pS, OrderAgentInitial pOr) {
		Duration durationLastOrderToPS = new Duration(0);
		if (currentSlot.getLastOrderVisited() != null) {
			Double distanceLastOrderToPS = Point.distance(pS.getPosition(), currentSlot.getLastOrderVisited().getPosition());
			durationLastOrderToPS = new Duration((long)((distanceLastOrderToPS/truckSpeed)*60*60*1000));
		}
		DateTime lastOrderToPSJustBeforeActualInterstedTime = actualInterestedTime.minus(durationLastOrderToPS).minusMinutes(1);
		DateTime currentSlotEndTime = currentSlot.getEndTime();
		if (currentSlot.getPS4NextOrderVisitedd() != null) {
			Double distanceOrderToPS4NextOrder = Point.distance(pOr.getPosition(), currentSlot.getPS4NextOrderVisitedd().getPosition());
			Duration durationOrderToPS4NextOrder = new Duration((long)((distanceOrderToPS4NextOrder/truckSpeed)*60*60*1000));
			currentSlotEndTime = currentSlotEndTime.minus(durationOrderToPS4NextOrder).minusMinutes(1);
		}
		return currentSlot.getStartTime().compareTo(lastOrderToPSJustBeforeActualInterstedTime)<= 0 //making rough estimation that will order enoughÊ interesting to be visited, one hour added since we donot not know how much could be travel distance from selected PS. PS is selected latter
				&& currentSlotEndTime.compareTo(interestedTime.plus(getCurrentOrderEstimatedTravelTimeAndUnloadingTime(travelTime, originator))) > 0;
//		return currentSlot.getStartTime().compareTo(actualInterestedTime)<= 0 //making rough estimation that will order enoughÊ interesting to be visited, one hour added since we donot not know how much could be travel distance from selected PS. PS is selected latter
//				&& currentSlot.getEndTime().compareTo(interestedTime.plusMinutes(CAUTION_MINUTES).plus(travelTime).plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)))) > 0;
	}
	
	static private Duration getCurrentOrderEstimatedTravelTimeAndUnloadingTime(Duration pTravelTime, DeliveryTruckInitial originator) {
		return pTravelTime.plus(new Duration((long)(originator.getCapacity() * 60l*60l*1000l/ GlobalParameters.DISCHARGE_RATE_PERHOUR)));
	}
	/**
	 * @param interestedTime
	 * @param currPSToCurrOrderTravelTime
	 * @return
	 */
	static public DateTime getActualInterestedTime(DateTime interestedTime,
			Duration currPSToCurrOrderTravelTime) {
		return interestedTime.minus(currPSToCurrOrderTravelTime).minusMinutes(GlobalParameters.LOADING_MINUTES);
	}
}
