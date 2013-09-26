/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;

import org.joda.time.DateTime;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.Ant;
import shaz.rmc.core.Utility;
import shaz.rmc.pdpExtended.delMasInitial.communication.CommitmentAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;

/**
 * @author Shaza
 * State to be used only in case of TruckStrategyCoalition
 */
public class TruckStateTeamCommitment extends TruckAgentState {

	/**
	 * 
	 */
	public TruckStateTeamCommitment(DeliveryTruckInitial truckAgent) {
		super(truckAgent);
		checkArgument(GlobalParameters.ENABLE_JI, true);
	}

	@Override
	public void processIntentionAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendExpAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendIntAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void followPracticalSchedule(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void letMeBreak(long startTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processGeneralAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (generalAnts.isEmpty() ) {
			truckAgent.setTruckState(TruckAgentState.IN_PROCESS);
			return;
		}
		Iterator<Ant> i = generalAnts.iterator();
		while (i.hasNext()) { 
			CommitmentAnt cAnt = (CommitmentAnt)i.next(); 
			if (truckAgent.getTruckSchedule().isOverlapped(cAnt.getCommUnit().getTunit())) {
				logger.debug(truckAgent.getId()+"T OVERLAP after team member breakdown, So dropping overlapped unit");
				truckAgent.getTruckSchedule().removeOverlappedUnit(cAnt.getCommUnit().getTunit(), truckAgent, currTime);
				addInTruckSchedule(cAnt);
			}
			else {
				addInTruckSchedule(cAnt);
			}
		}
		generalAnts.clear();
		
	}

	/**
	 * @param cAnt
	 */
	private void addInTruckSchedule(CommitmentAnt cAnt) {
		truckAgent.getTruckSchedule().add(cAnt.getCommUnit().getTunit(), cAnt.getCommUnit().getOrderReply());
		truckAgent.getTruckSchedule().fillTravelUnitsInSchedule(truckAgent);
		logger.debug(truckAgent.getId()+"T Schedule unit added in Trucks schedule (status= " +cAnt.getCommUnit().getOrderReply()+ ": " + cAnt.getCommUnit().getTunit().toString());
	}

	@Override
	int getStateCode() {
		return TruckAgentState.TEAM_COMMITMENT;
	}

	@Override
	public void processOrderPlanInformerAnts(long startTime) {
		processOrderPlanInformerAnt(startTime);		
	}
	@Override
	public String toString(){
		return "TEAM_COMMITMENT";
	}
}
