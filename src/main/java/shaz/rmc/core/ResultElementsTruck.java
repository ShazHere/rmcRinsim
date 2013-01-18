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
	private int startTimeDelay = 0; //already included in lag time
	private int wastedConcrete = 0; //wasted conrete is here since while exploring (and latter booking) ants fill the schedule unit if a deliveyr involves wastage of concrete. 
	private int totalConcrete = 0;  //total concrete delivered by truck(s)
	private int totalDeliveries = 0; //totoal Deliveries made by truck(s)
	
	public ResultElementsTruck(int totalDeliveries, int travelMin, int lagTimeInMin, int startTimeDelay,
			int wastedConcrete, int totalConcrete) {
		super();
		this.totalDeliveries = totalDeliveries;  
		this.travelMin = travelMin;
		this.lagTimeInMin = lagTimeInMin;
		this.startTimeDelay = startTimeDelay;
		this.wastedConcrete = wastedConcrete;
		this.totalConcrete = totalConcrete;
		
	}
	public ResultElementsTruck() {
		this.travelMin = 0;
		this.lagTimeInMin = 0;
		this.startTimeDelay = 0;
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
		return startTimeDelay;
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
		startTimeDelay += pStartTimeDelay;
		return startTimeDelay;
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
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("TotalDeliveriesByTrucks: ").append(totalDeliveries).append("\n");
		sb.append("TotalConcreteByTrucks: ").append(totalConcrete).append("\n");
		sb.append("LagTime: ").append(lagTimeInMin).append("\n");
		sb.append("TravelDistance: ").append(travelMin).append("\n");
		sb.append("WastedConcrete: ").append(wastedConcrete).append("\n");
		sb.append("StartTimeDelay: ").append(startTimeDelay).append("\n");
		return sb.toString();
	}
}
