/**
 * 
 */
package shaz.rmc.core;

import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters.Weights;

/**
 * Statistic Attributes related to Truck
 * @author Shaza
 *
 */
public class ResultElementsTruck {
	private int travelMin = 0;
	private int lagTimeInMin = 0;
	private int startTimeDelayInMin = 0; //already included in lag time, this is delay caused during exploration ants
	private int wastedConcrete = 0; //wasted conrete is here since while exploring (and latter booking) ants fill the schedule unit if a deliveyr involves wastage of concrete. 
	private int totalConcrete = 0;  //total concrete delivered by truck(s)
	private int totalDeliveries = 0; //totoal Deliveries made by truck(s)
	private int noOfTimesACCEPTRemoved = 0;
	private int noOfTimesWEEK_ACCEPTRemoved = 0;
	private int noOfTimesUNDERPROCESSRemoved = 0;

	
	public ResultElementsTruck(int totalDeliveries, int travelMin, int lagTimeInMin, int startTimeDelay,
			int wastedConcrete, int totalConcrete, int noOfTimesACCEPTRemoved, int noOfTimesWEEK_ACCEPTRemoved, int noOfTimesUNDERPROCESSRemoved) {
		super();
		this.totalDeliveries = totalDeliveries;  
		this.travelMin = travelMin;
		this.lagTimeInMin = lagTimeInMin;
		this.startTimeDelayInMin = startTimeDelay;
		this.wastedConcrete = wastedConcrete;
		this.totalConcrete = totalConcrete;
		this.noOfTimesACCEPTRemoved = noOfTimesACCEPTRemoved;
		this.noOfTimesWEEK_ACCEPTRemoved = noOfTimesWEEK_ACCEPTRemoved;
		this.noOfTimesUNDERPROCESSRemoved = noOfTimesUNDERPROCESSRemoved;
		
	}
	public ResultElementsTruck() {
		this.travelMin = 0;
		this.lagTimeInMin = 0;
		this.startTimeDelayInMin = 0;
		this.wastedConcrete = 0;
		this.totalDeliveries = 0;
		this.totalConcrete = 0;
	}
	public int getTravelMin() {
		return travelMin;
	}

	public int getLagTimeInMin() {
		return lagTimeInMin;
	}

	public int getStartTimeDelay() {
		return startTimeDelayInMin;
	}

	public int getWastedConcrete() {
		return wastedConcrete;
	}

	public int addTravelMin(int pTravelMin) {
		travelMin += pTravelMin;
		return travelMin;
	}

	public int addLagTimeInMin(int pLagTimeInMin) {
		lagTimeInMin =+ pLagTimeInMin;
		return lagTimeInMin;
	}

	public int addStartTimeDelay(int pStartTimeDelay) {
		startTimeDelayInMin += pStartTimeDelay;
		return startTimeDelayInMin;
	}

	public int addWastedConcrete(int pWastedConcrete) {
		wastedConcrete += pWastedConcrete;
		return wastedConcrete;
	}
	
	public int getTotalConcrete() {
		return totalConcrete;
	}
	public void addTotalConcrete(int totalConcrete) {
		this.totalConcrete += totalConcrete;
	}
	public int getTotalDeliveries() {
		return totalDeliveries;
	}
	public void addTotalDeliveries(int totalDeliveries) {
		this.totalDeliveries += totalDeliveries;
	}
	public int getNoOfTimesACCEPTRemoved() {
		return noOfTimesACCEPTRemoved;
	}
	public void addNoOfTimesACCEPTRemoved(int noOfTimesACCEPTRemoved) {
		this.noOfTimesACCEPTRemoved += noOfTimesACCEPTRemoved;
	}
	public int getNoOfTimesWEEK_ACCEPTRemoved() {
		return noOfTimesWEEK_ACCEPTRemoved;
	}
	public void addNoOfTimesWEEK_ACCEPTRemoved(int noOfTimesWEEK_ACCEPTRemoved) {
		this.noOfTimesWEEK_ACCEPTRemoved += noOfTimesWEEK_ACCEPTRemoved;
	}
	public int getNoOfTimesUNDERPROCESSRemoved() {
		return noOfTimesUNDERPROCESSRemoved;
	}
	public void addNoOfTimesUNDERPROCESSRemoved(int noOfTimesUNDERPROCESSRemoved) {
		this.noOfTimesUNDERPROCESSRemoved += noOfTimesUNDERPROCESSRemoved;
	}
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TotalDeliveriesByTrucks: ").append(totalDeliveries).append("\n");
		sb.append("TotalConcreteByTrucks: ").append(totalConcrete).append("\n");
		sb.append("LagTime: ").append(lagTimeInMin).append("\n");
		sb.append("TravelDistance: ").append(travelMin).append("\n");
		sb.append("WastedConcrete: ").append(wastedConcrete).append("\n");
		sb.append("StartTimeDelay: ").append(startTimeDelayInMin).append("\n");
		sb.append("ACCEPTEDRemoved: ").append(this.noOfTimesACCEPTRemoved).append("\n");
		sb.append("WEEKACCEPTEDRemoved: ").append(this.noOfTimesWEEK_ACCEPTRemoved).append("\n");
		sb.append("UNDERPROCESSRemoved: ").append(this.noOfTimesUNDERPROCESSRemoved).append("\n");
		return sb.toString();
	}
}
