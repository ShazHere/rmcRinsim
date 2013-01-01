/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Vehicle;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import shaz.rmc.core.ResultElements;

/**
 * @author Shaza
 *
 */
public class StatisticTracker implements Listener {
	protected long startTimeReal;
	protected long startTimeSim;
	protected long computationTime;
	protected long simulationTime;
	protected long endTimeSim;
	final private Simulator sim;
	final PDPModel pdpModel;
	
	public StatisticTracker(Simulator pSim, final PDPModel pPdpModel) {
		sim = pSim;
		pdpModel = pPdpModel;
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
			Set<Vehicle> truckSet = pdpModel.getVehicles();
			int objValue = 0;
			int totalTrucksUsed = 0;
			ResultElements re;
			ResultElements allResult = new ResultElements();
			for (Vehicle v: truckSet){
				re = ((DeliveryTruckInitial)v).getScheduleScore();
				if (re != null) {
					totalTrucksUsed += 1;
					//objValue = re.getTotalValue();
					allResult.addLagTimeInMin(re.getLagTimeInMin());
					allResult.addStartTimeDelay(re.getStartTimeDelay());
					allResult.addTravelMin(re.getTravelMin());
					allResult.addWastedConcrete(re.getWastedConcrete());
				}
			}
			sb.append("Total Trucks used: ").append(totalTrucksUsed).append("\n");
			sb.append("Objective Function Value: ").append(allResult.getTotalValue()).append("\n");
			sb.append(allResult.toString());
			return sb.toString();
		}
}
