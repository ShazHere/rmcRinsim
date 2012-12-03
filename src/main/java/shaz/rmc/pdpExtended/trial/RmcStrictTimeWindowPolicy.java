package shaz.rmc.pdpExtended.trial;

import rinde.sim.core.model.pdp.twpolicy.TimeWindowPolicy;
import rinde.sim.util.TimeWindow;

public class RmcStrictTimeWindowPolicy implements TimeWindowPolicy {
	public RmcStrictTimeWindowPolicy()
	{}

	public boolean canPickup(TimeWindow tw, long time, long duration) {
		// TODO Auto-generated method stub
		return false;
	}

	public boolean canDeliver(TimeWindow tw, long time, long duration) {
		// TODO Auto-generated method stub
		return false;
	}

}
