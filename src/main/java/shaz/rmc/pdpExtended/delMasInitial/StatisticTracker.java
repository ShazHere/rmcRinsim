/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import rinde.sim.core.Simulator;
import rinde.sim.core.Simulator.SimulatorEventType;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;

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
	private Simulator sim; 
	
	public StatisticTracker(Simulator pSim) {
		sim = pSim;
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
}
