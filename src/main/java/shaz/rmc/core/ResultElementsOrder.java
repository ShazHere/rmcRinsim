/**
 * 
 */
package shaz.rmc.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Hours;

import shaz.rmc.core.domain.Delivery;
/**
 * The statistic attributes related to Order
 * @author Shaza
 *
 */
public class ResultElementsOrder {
	
	//private final ArrayList<Delivery> deliveries;
	private int totalDeliveriesReadyByOrder =0; //so orders made these deliveries ready
	private int totalConcreteByOrder = 0; //total amount of concrete orderd byt the customer
	private int deliveredConcrete = 0; //totoal amount of concrete delivered to customer
	private int undeliveredConcrete = 0; //amount of concrete, not furnished by any of trucks
	private ArrayList<HourConcreteDetail> hourConcreteDetail; //arrayList to handle the case when ResultElementsOrder is used to store commulative data of all..
	// if ResultElementOrder is used to store store result for a single order, this arrayList should have only one object.
	public ResultElementsOrder () {
		//deliveries = null;
		totalDeliveriesReadyByOrder =0;
		totalConcreteByOrder = 0;
		deliveredConcrete = 0;
		undeliveredConcrete = 0;
		//hoursConcrete = new int[24];
		hourConcreteDetail = new ArrayList<ResultElementsOrder.HourConcreteDetail>();
		
	}
	
	public ResultElementsOrder(ArrayList<Delivery> deliveriesReadyByOrder,
			int totalConcreteByOrder, int deliveredConcrete, int []hoursConcrete, DateTime startTime, int expectedWastedConcrete, int actualWastedConcrete) {
		super();
		//this.deliveries = deliveriesReadyByOrder;
		
		this.totalConcreteByOrder = totalConcreteByOrder;
		this.deliveredConcrete = deliveredConcrete;
		int actualServedConcrete = 0;
		if (deliveriesReadyByOrder != null) {
			for (Delivery d : deliveriesReadyByOrder) {
				actualServedConcrete += d.getDeliveredVolume() - d.getWastedVolume();
			}
			this.totalDeliveriesReadyByOrder = deliveriesReadyByOrder.size();
		} //else default values are already ok.
			
		this.undeliveredConcrete = this.totalConcreteByOrder - actualServedConcrete;
		//this.hoursConcrete = hoursConcrete;
		hourConcreteDetail = new ArrayList<ResultElementsOrder.HourConcreteDetail>();
		HourConcreteDetail hcd = new HourConcreteDetail(hoursConcrete, startTime.getHourOfDay(),this.totalConcreteByOrder, expectedWastedConcrete, actualWastedConcrete);
		hourConcreteDetail.add(hcd);
	}

	public int getTotalDeliveriesReadyByOrder() {
		return totalDeliveriesReadyByOrder;
	}

	public void addTotalDeliveriesReadyByOrder(int totalDeliveriesReadyByOrder) {
		this.totalDeliveriesReadyByOrder += totalDeliveriesReadyByOrder;
	}

	public int getTotalConcreteByOrder() {
		return totalConcreteByOrder;
	}

	public void addTotalConcreteByOrder(int totalConcreteByOrder) {
		this.totalConcreteByOrder += totalConcreteByOrder;
	}

	public int getDeliveredConcrete() {
		return deliveredConcrete;
	}

	public void addDeliveredConcrete(int deliveredConcrete) {
		this.deliveredConcrete += deliveredConcrete;
	}
	public int getUndeliveredConcrete() {
		return undeliveredConcrete;
	}
	public void addUndeliveredConcrete(int undeliveredConcrete) {
		this.undeliveredConcrete += undeliveredConcrete;
	} 
	public void addHoursConcreteInList( int []hoursConcrete, int startHour, int concreteByOrder, int expectedWastedConcrete, int actualWastedConcrete) {
		this.hourConcreteDetail.add(new HourConcreteDetail(hoursConcrete, startHour, concreteByOrder, expectedWastedConcrete, actualWastedConcrete));
	}
	
	public int[] getHoursConcrteList() {
		checkArgument(this.hourConcreteDetail.size() == 1); 
		return this.hourConcreteDetail.get(0).getHourDelivered();
	}
	
	public int getStartHour() {
		checkArgument(this.hourConcreteDetail.size() == 1); 
		return this.hourConcreteDetail.get(0).getStartHour();
	}
	
	public int getExpectedWastedConcrete() {
		checkArgument(this.hourConcreteDetail.size() == 1);
		return this.hourConcreteDetail.get(0).getExpectedWastedConcrete();
	}
	
	public int getActualWastedConcrete() {
		checkArgument(this.hourConcreteDetail.size() == 1);
		return this.hourConcreteDetail.get(0).getActualWastedConcrete();
	}
	
	/**
	 * @param columnSeperator seperator between columns
	 * @return String that represents all the column names related to per hour concrete generation in Order. 
	 * Actual string should be according to no. of orders in ResultElementOrder
	 * String format should be: "Hours seperator startHour1 Delivered1 startHour2 Delivered2...."
	 */
	public String getColumnNamesHoursConcrete(String columnSeperator) {
		StringBuilder colNames = new StringBuilder();
		colNames.append("Hours").append(columnSeperator);
		int index = 1;
		for (HourConcreteDetail hourList : this.hourConcreteDetail) { //to make it no. of times = total orders
			colNames.append("startHour" + index).append(columnSeperator);
			colNames.append("Delivered" + index).append(columnSeperator);
			index++;
		}
		
		return colNames.toString();
	}
	/**
	 * @param seperator between columns
	 * @return String that represents a table with 25 rows (1st header row, then 24 hours).
	 * The detailsa are related to per hour concrete generation in all the Orders of resultOrder. 
	 * Actual string should be according to no. of orders in ResultElementOrder
	 * String format should be kind of tabular: "Hours seperator startHour1 Delivered1 startHour2 Delivered2...."
	 * one row represents Xth hour's data for all oll orders.
	 */
	public String getResultDetailsHourConcrteInColumnForm (String seperator) {
		StringBuilder hourDataTable = new StringBuilder();
		
		for (int hourIndex = 1; hourIndex <= 24; hourIndex++ ) { //for each row, 
			hourDataTable.append(hourIndex).append(seperator);
			for (HourConcreteDetail hourList : this.hourConcreteDetail) {
				hourDataTable.append(hourList.getStartHourForIndex(hourIndex-1)).append(seperator);
				hourDataTable.append(hourList.getHourDelivered()[hourIndex-1]).append(seperator);
			}
			hourDataTable.append("\n");
		}
		
		return hourDataTable.toString();
	}
	
	/**
	 * @param columnSeperator
	 * @return String that represents all column names related to wasted concrete in Order
	 * Actual String should be according to no. of orders in ResultElementOrder
	 * String format should be: "totalConcrete1 sperator ExpectedWasted1 seperator ActualWasted1...."
	 */
	public String getColumnNamesWastedConcrete(String columnSeperator) {
		StringBuilder colNames = new StringBuilder();	
		int index = 1;
		for (HourConcreteDetail wastedList : this.hourConcreteDetail) { //to make it no. of times = total orders
			colNames.append("totalConcrete" + index).append(columnSeperator);
			colNames.append("ExpectedWasted" + index).append(columnSeperator);
			colNames.append("ActualWasted" + index).append(columnSeperator);
			index++;
		}
		return colNames.toString();
	}
	
	/**
	 * @param seperator between columns
	 * @return String that represents data in column format for wasted concrete in Order.
	 * The details are related to per hour concrete generation in all the Orders of resultOrder. 
	 * Actual String should be according to no. of orders in ResultElementOrder
	 * The data should be in format format should be: "totalConcrete1 sperator ExpectedWasted1 seperator ActualWasted1...."
	 */
	public String getResultDetailsWastedConcrteInColumnForm (String seperator) {
		StringBuilder wastedDetails = new StringBuilder();
		
			for (HourConcreteDetail wastedList : this.hourConcreteDetail) {
				wastedDetails.append(wastedList.getTotalConcrete()).append(seperator);
				wastedDetails.append(wastedList.getExpectedWastedConcrete()).append(seperator);
				wastedDetails.append(wastedList.getActualWastedConcrete()).append(seperator);
			}
		return wastedDetails.toString();
	}
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TotalDeliveriesByOrder: ").append(totalDeliveriesReadyByOrder).append("\n");
		sb.append("TotalConcreteByOrder: ").append(totalConcreteByOrder).append("\n");
		sb.append("DeliveredConcrete: ").append(deliveredConcrete).append("\n");
		sb.append("UndeliveredConcrete: ").append(undeliveredConcrete).append("\n");
		return sb.toString();
	}
	/**
	 * @author Shaza
	 * @date 19/06/2013
	 * To record per hour concrete record for a single order. The main class can be used to store commulative
	 * information as well single order. But this inner class's one object can store informatio related to oneorder. 
	 */
	private class HourConcreteDetail {
		final int hourDelivered []; // to record 24 hours of an order
		//for storing delivered concrete w.r.t hours. For an Xth hour, the bucket will contain concrete with delivery.unloadTime 
		// between (X-1,X] interval
		final int startHour; //considering total hour range 1 to 23
		final int totalConcrete;
		final int expectedWastedConcrete;
		final int actualWastedConcrete;
		
		private HourConcreteDetail() {//not used
			hourDelivered = new int[24];
			startHour = 0;
			totalConcrete = 0;
			expectedWastedConcrete = 0;
			actualWastedConcrete = 0;
		}
		public HourConcreteDetail(int[] hourDelivered, int startHour, int totalConcrete,
				int expectedWastedConcrete, int actualWastedConcrete) {
			super();
			if (hourDelivered != null)
				this.hourDelivered = hourDelivered;
			else
				this.hourDelivered = new int[24];
			this.startHour = startHour;
			this.totalConcrete = totalConcrete;
			this.expectedWastedConcrete = expectedWastedConcrete;
			this.actualWastedConcrete = actualWastedConcrete;
		}
		public int[] getHourDelivered() {
			return hourDelivered;
		}
		public int getStartHour() {
			return startHour;
		}
		public int getStartHourForIndex(int x) {
			if (x == startHour)
				return this.totalConcrete;
			else
				return 0;
		}
		public int getTotalConcrete() {
			return totalConcrete;
		}
		public int getExpectedWastedConcrete() {
			return expectedWastedConcrete;
		}
		public int getActualWastedConcrete() {
			return actualWastedConcrete;
		}
		
	}
	
}
