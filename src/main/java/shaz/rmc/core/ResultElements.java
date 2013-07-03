/**
 * 
 */
package shaz.rmc.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Set;

import rinde.sim.core.model.pdp.Vehicle;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters.Weights;

/**
 * General measurable statistic Elements for an RMC solution. It may have objects 
 * of ResultElementOrder and ResultElementTruck. 
 * 
 * Will be able to compute total no. of trucks and order
 * @author Shaza
 *
 */
public class ResultElements {
	
	private ResultElementsOrder resultOrder;
	private ResultElementsTruck resultTruck;
	private final int totalTrucksGiven; //that were given to the simulations
	private int totalTrucksUsed; //that serviced
	private final int totalOrderGiven; // total orders at the moment, at the moment no track of serviced order,
	//	because serviced concrte seems more important
	private int totalOrderServed;
	
	private int totalValue = 0; // total cost of objective function

	
	public ResultElements(ResultElementsOrder resultOrder,
			ResultElementsTruck resultTruck) {
		super();
		this.resultOrder = resultOrder;
		this.resultTruck = resultTruck;
		totalOrderGiven = 0;
		totalTrucksGiven =0;
		totalTrucksUsed = 0;
	}
	public ResultElements(Set<OrderAgentInitial> orderSet,
			Set<Vehicle> truckSet) {
		super();
		ArrayList<Integer> truckCapacities = new ArrayList<Integer>(); // to record total types of truck capacities to be sent to order
		// for estimating expected wasted concrete
		totalTrucksUsed = 0;
		totalTrucksGiven = truckSet.size();
		ResultElementsTruck re;
		ResultElementsTruck allResult = new ResultElementsTruck();
		for (Vehicle v: truckSet){
			re = ((DeliveryTruckInitial)v).getTruckResult();
			if (re != null) {
				totalTrucksUsed += 1;
				allResult.addLagTimeInMin(re.getLagTimeInMin());
				allResult.addStartTimeDelay(re.getStartTimeDelay());
				allResult.addTravelMin(re.getTravelMin());
				allResult.addWastedConcrete(re.getWastedConcrete());
				allResult.addTotalDeliveries(re.getTotalDeliveries());
				allResult.addTotalConcrete(re.getTotalConcrete());
			}
			boolean addIt = true;
			for (Integer capacity: truckCapacities) {
				if (capacity == ((DeliveryTruckInitial)v).getCapacity())
					addIt = false;
			}
			if (addIt)
				truckCapacities.add((int)((DeliveryTruckInitial)v).getCapacity());
		}
		
		totalOrderGiven = orderSet.size();
		totalOrderServed = 0;
		ResultElementsOrder ro;
		ResultElementsOrder allResultOrder = new ResultElementsOrder();
		for (OrderAgentInitial o: orderSet) {
			ro = o.getOrderResult(truckCapacities);
			if (ro != null) {
				allResultOrder.addDeliveredConcrete(ro.getDeliveredConcrete());
				allResultOrder.addTotalConcreteByOrder(ro.getTotalConcreteByOrder());
				allResultOrder.addTotalDeliveriesReadyByOrder(ro.getTotalDeliveriesReadyByOrder());
				allResultOrder.addUndeliveredConcrete(ro.getUndeliveredConcrete());
				allResultOrder.addHoursConcreteInList(ro.getHoursConcrteList(),ro.getStartHour(), ro.getTotalConcreteByOrder(), ro.getExpectedWastedConcrete(), ro.getActualWastedConcrete());
				if (ro.getUndeliveredConcrete() == 0)
					totalOrderServed +=1;
			}
		}	
		this.resultOrder = allResultOrder;
		this.resultTruck = allResult;
	}
	public ResultElementsOrder getResultOrder() {
		return resultOrder;
	}

//	public void setResultOrder(ResultElementsOrder resultOrder) {
//		this.resultOrder = resultOrder;
//	}

	public ResultElementsTruck getResultTruck() {
		return resultTruck;
	}

//	public void setResultTruck(ResultElementsTruck resultTruck) {
//		this.resultTruck = resultTruck;
//	}
	public int getTotalTrucksGiven() {
		return totalTrucksGiven;
	}
	public int getTotalTrucksUsed() {
		return totalTrucksUsed;
	}
	public int getTotalOrderGiven() {
		return totalOrderGiven;
	}
	public int getTotalOrderServed() {
		return totalOrderServed;
	}
	 /*
	  *	From ResultOrder 
	  */
	public int getTotalDeliveriesReadyByOrder() {
		return resultOrder.getTotalDeliveriesReadyByOrder();
	}	
	public int getTotalConcreteByOrder() {
		return resultOrder.getTotalConcreteByOrder();
	}
	public int getDeliveredConcrete() {
		return resultOrder.getDeliveredConcrete();
	}
	public int getUndeliveredConcrete() {
		return resultOrder.getUndeliveredConcrete();//this.getDeliveredConcrete() - (this.getTotalConcrete() - this.getWastedConcrete());
	}
	
	
	/*
	 * From ResultTruck
	 */	
	public int getTravelMin() {
		return resultTruck.getTravelMin();
	}
	public int getLagTimeInMin() {
		return resultTruck.getLagTimeInMin();
	}
	public int getStartTimeDelay() {
		return resultTruck.getStartTimeDelay();
	}
	public int getWastedConcrete() {
		return resultTruck.getWastedConcrete();
	}
	public int getTotalConcrete() {
		return resultTruck.getTotalConcrete();
	}
	public int getTotalDeliveries() {
		return resultTruck.getTotalDeliveries();
	}
	/**
	 * @return cost of objective function
	 */
	public int getTotalValue() {
		totalValue = (Weights.TRAVEL_TIME * resultTruck.getTravelMin()) + //(Weights.LAGTIME*resultTruck.getLagTimeInMin()) + 
				(Weights.STARTTIME_DELAY*resultTruck.getStartTimeDelay()) + (Weights.CONCRETE_WASTAGE*(resultTruck.getWastedConcrete()/1000)); 
		return totalValue;
	}
	@Override
	public String toString() {
		return "\n" + resultOrder.toString() + "\n" + resultTruck.toString();
	}
	
	/**
	 * @param seperator between columns
	 * @return string that represents all the column names related to problem. Does not include the simulation run details
	 */
	public String getColumnNames(String seperator) {
		StringBuilder colNames = new StringBuilder();
		//collective details
		colNames.append("ObjectiveFunction").append(seperator);
		colNames.append("TotalTrucksGiven").append(seperator);
		colNames.append("TotalTrucksUsed").append(seperator);
		colNames.append("TotalOrdersGiven").append(seperator);
		colNames.append("TotalOrdersServed").append(seperator);
		
		//truck detail
		colNames.append("TravelMin").append(seperator);
		colNames.append("LagTimeInMin").append(seperator);
		colNames.append("StartTimeDelay").append(seperator);
		colNames.append("WastedConcrete").append(seperator);
		colNames.append("TotalConcrete").append(seperator);
		colNames.append("TotalDeliveries").append(seperator);
		
		//order details
		colNames.append("TotalDeliveriesReadyByOrder").append(seperator);
		colNames.append("TotalConcreteByOrder").append(seperator);
		colNames.append("DeliveredConcrete").append(seperator);
		colNames.append("UnDeliveredConcrete");
		
		return colNames.toString();
	}
	/**
	 * @param columnSeperator seperator between columns
	 * @return String that represents all the column names related to per hour concrete generation in Order. 
	 *
	 */
	public String getColumnNamesHoursConcrete(String columnSeperator) {
		return resultOrder.getColumnNamesHoursConcrete(columnSeperator);
	}
	/**
	 * @param seperator between columns
	 * @return String that represents a table with 25 rows (1st header row, then 24 hours).
	 * The detailsa are related to per hour concrete generation in all the Orders of resultOrder. 
	 */
	public String getResultDetailsHourConcrteInColumnForm (String seperator) {
		return resultOrder.getResultDetailsHourConcrteInColumnForm(seperator);
	}
	/**
	 * @param columnSeperator seperator between columns
	 * @return String that represents all the column names related to waseted concrete generation in Order. 
	 *
	 */
	public String getColumnNamesWastedConcrete(String columnSeperator) {
		return resultOrder.getColumnNamesWastedConcrete(columnSeperator);
	}
	/**
	 * @param seperator between columns
	 * @return String that represents data in column format for wasted concrete in Order.
	 * The details are related to per hour concrete generation in all the Orders of resultOrder. 
	 * Actual String should be according to no. of orders in ResultElementOrder
	 * The data should be in format format should be: "totalConcrete1 sperator ExpectedWasted1 seperator ActualWasted1...."
	 */
	public String getResultDetailsWastedConcrteInColumnForm (String seperator) {
		return resultOrder.getResultDetailsWastedConcrteInColumnForm(seperator);
	}
	/**
	 * 
	 * @param seperator between columns, should be corresponding to seperator given to getColumnNames
	 * @return string that represents all the column names related to problem.
	 * The order and no. of elements should be corresponding to getColumnNames() method 
	 * Does not include the simulation run details
	 */
	public String getResultDetailsInColumnForm(String seperator) {
		StringBuilder resultDetails = new StringBuilder();
		//collective details
		resultDetails.append(getTotalValue()).append(seperator);
		resultDetails.append(getTotalTrucksGiven()).append(seperator);
		resultDetails.append(getTotalTrucksUsed()).append(seperator);
		resultDetails.append(getTotalOrderGiven()).append(seperator);
		resultDetails.append(getTotalOrderServed()).append(seperator);
		
		//truck detail
		resultDetails.append(getTravelMin()).append(seperator);
		resultDetails.append(getLagTimeInMin()).append(seperator);
		resultDetails.append(getStartTimeDelay()).append(seperator);
		resultDetails.append(getWastedConcrete()).append(seperator);
		resultDetails.append(getTotalConcrete()).append(seperator);
		resultDetails.append(getTotalDeliveries()).append(seperator);
		
		//order details
		resultDetails.append(getTotalDeliveriesReadyByOrder()).append(seperator);
		resultDetails.append(getTotalConcreteByOrder()).append(seperator);
		resultDetails.append(getDeliveredConcrete()).append(seperator);
		resultDetails.append(getUndeliveredConcrete());
		
		return resultDetails.toString();
	}
	
}
