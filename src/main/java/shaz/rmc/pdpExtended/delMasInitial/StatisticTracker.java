/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;
import java.util.Set;
import static java.util.Collections.unmodifiableSet;


import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.pdp.Depot;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import shaz.rmc.core.ResultElements;
import shaz.rmc.core.ResultElementsOrder;
import shaz.rmc.core.ResultElementsTruck;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Delivery;

/**
 * @author Shaza
 *
 */
public class StatisticTracker implements Listener {
	private long startTimeReal;
	private long startTimeSim;
	private long computationTime;
	private long simulationTime;
	private long endTimeSim;
	private final Simulator sim;
	private final PDPModel pdpModel;
	private final RoadModel roadModel; 
	private final OrderManagerInitial orm; 
	private ResultElements stats;
	
	public StatisticTracker(Simulator pSim, final PDPModel pPdpModel, OrderManagerInitial pOrm, final RoadModel pRoadModel) {
		sim = pSim;
		pdpModel = pPdpModel;
		orm = pOrm;
		stats = null;
		roadModel = pRoadModel;
	}

		@Override
		public void handleEvent(Event e) {
			if (e.getEventType() == SimulatorEventType.STARTED) {
				startTimeReal = System.currentTimeMillis();
				startTimeSim = sim.getCurrentTime();
			} else if (e.getEventType() == SimulatorEventType.STOPPED) {
				computationTime = System.currentTimeMillis() - startTimeReal;
				simulationTime = sim.getCurrentTime() - startTimeSim;
				endTimeSim = sim.getCurrentTime();
			}			
		}
		public String collectDataString() {
			final StringBuilder sb = new StringBuilder();
			sb.append("StartTime Real: ").append(new DateTime(startTimeReal)).append("\r");
			sb.append("StartTime Sim: ").append(new DateTime(startTimeSim + GlobalParameters.START_DATETIME.getMillis())).append("\r");
			sb.append("EndTime Sim: ").append(new DateTime(endTimeSim + GlobalParameters.START_DATETIME.getMillis())).append("\r");
			sb.append("Computation Time Real (sec): ").append(new Duration(computationTime).getStandardSeconds()).append("\r");
			sb.append("Simulation Time (min): ").append(new Duration(simulationTime).getStandardMinutes()).append("\r");
			sb.append("Simulation Time (sec): ").append(new Duration(simulationTime).getStandardSeconds()).append("\r");
			return sb.toString();
		
		}
		public String collectStatistics() {
			final StringBuilder sb = new StringBuilder();
			ArrayList<Depot> psSet = new ArrayList<Depot>(roadModel.getObjectsOfType(ProductionSiteInitial.class));
			Set<Vehicle> truckSet = pdpModel.getVehicles();
			Set<OrderAgentInitial> orderSet = orm.getOrders();
			stats = new ResultElements(orderSet, truckSet);
			sb.append("\n");
			//order related
			sb.append("TotalOrdersGiven: ").append(stats.getTotalOrderGiven()).append("\n");
			sb.append("TotalOrdersServed: ").append(stats.getTotalOrderServed()).append("\n");
			sb.append(stats.getResultOrder().toString());
			//truck related
			sb.append("\n").append("TotalTrucksUsed: ").append(stats.getTotalTrucksUsed()).append("\n");
			sb.append("TotalTrucksGiven: ").append(stats.getTotalTrucksGiven()).append("\n");
			sb.append(stats.getResultTruck().toString());
			//objfunc
			sb.append("ObjectiveFunctionValue: ").append(stats.getTotalValue()).append("\n");
			return sb.toString();
		}
		public String hourlyStatistic () {
			final StringBuilder sb = new StringBuilder();
			Set<OrderAgentInitial> orderSet = orm.getOrders();
			
			return sb.toString();
		}
		public void writeFile() {
			Utility.wrtieInFile(true, stats);
		}
}
