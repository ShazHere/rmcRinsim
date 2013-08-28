/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Iterator;

import org.joda.time.DateTime;

import com.rits.cloning.Cloner;

import rinde.sim.core.TimeLapse;
import shaz.rmc.core.AvailableSlot;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.communicateAbleUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.IntAnt;
import shaz.rmc.pdpExtended.delMasInitial.communication.OrderPlanInformerAnt;

/**
 * @author Shaza
 *
 */
public class TruckStateInProcess extends TruckAgentState {

	//since most of exp and int ants are tackedle by INPROCESS state, it doestn't matter that these variables are hrere. 
	//But if in future some other states handle them, then I need to change this.
	private DateTime timeForLastExpAnt; //time at which last Exp was sent by truck
	private DateTime timeForLastIntAnt; //time at which last Int was sent by truck	
	private ExpAnt bestAnt;
	
	public TruckStateInProcess(DeliveryTruckInitial truckAgent) {
		super(truckAgent);
		timeForLastExpAnt = new DateTime(getTotalTimeRange().getStartTime().plusMinutes(truckAgent.getDePhaseByMin()));
		timeForLastIntAnt = new DateTime(getTotalTimeRange().getStartTime().plusMinutes(truckAgent.getDePhaseByMin()).plusMinutes(GlobalParameters.INTENTION_INTERVAL_MIN));
		bestAnt = null;
	}


	@Override
	int getStateCode() {
		return TruckAgentState.IN_PROCESS;
	}


	@Override
	public
	void processIntentionAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		if (!intentionAnts.isEmpty() ) { 
			checkArgument(intentionAnts.size() == 1, true); //since only one intention is send out at one time!
			IntAnt iAnt = intentionAnts.get(0);
			checkArgument(iAnt.isConsistentWithExistingSchedule(truckAgent.getTruckSchedule().getSchedule()) == true, true);
			if (iAnt.hasNoREJECTTunit() ) 
			{
				addOrUpdateTUnits(iAnt);;
			} //no need of else,coz it will be removed any way..
			else {
				handleREJECTEDTunits(iAnt);
			}
			truckAgent.getTruckSchedule().makePracticalSchedule(truckAgent);
			intentionAnts.clear();  //remove the ant
		}
	}

	private void processExplorationAnts(long startTime) {
		if (explorationAnts.isEmpty())
			return;
		//first remove invalid schedules, and get whats the maxScore size
		int maxSizeFound = 0;
		Iterator<ExpAnt> i = explorationAnts.iterator();
		while (i.hasNext()) { 
			ExpAnt eAnt = i.next();
			if (truckAgent.getTruckSchedule().scheduleStillValid(eAnt.getSchedule()) == false)
				i.remove();
			else if (eAnt.getSchedule().size() > maxSizeFound)
				maxSizeFound = eAnt.getSchedule().size();
		}
		if (explorationAnts.isEmpty())
			return;
		//prune the one with lesser schedule size
		Iterator<ExpAnt> j = explorationAnts.iterator();
		while (j.hasNext()) { 
			ExpAnt eAnt = j.next();
			if (eAnt.getSchedule().size() < maxSizeFound)
				j.remove();
		}
		checkArgument(explorationAnts.isEmpty() == false, true);
		
		//handle the selection based on schedule score
		if (GlobalParameters.EXP_RANKING_WITH_SCORE_ENABLE) {
			//select 1st schedule as best
			bestAnt = explorationAnts.get(0);
			for (ExpAnt eAnt: explorationAnts) { //find eAnt with smallest score, i.e least cost
				//if (b.scheduleStillValid(b.schedule, eAnt.getSchedule())){				
					if (eAnt.getScheduleScore()< bestAnt.getScheduleScore()) {
							bestAnt = eAnt;
						}
				//}
			}
		}
		else {
			bestAnt = explorationAnts.get(truckAgent.getRandomPCSelector().nextInt(explorationAnts.size()));
		} //soem condition if bestAnt == b.shedule..than better bestAnt = null.
		printBestAnt(startTime);
		explorationAnts.clear(); 
	}
	
	private boolean addOrUpdateTUnits(IntAnt iAnt){
		boolean newDeliveryUnitAdded = false;
		final Cloner cl = Utility.getCloner();
		for (communicateAbleUnit u : iAnt.getSchedule()){
			if (u.isAddedInTruckSchedule() == false) {
				checkArgument(truckAgent.getTruckSchedule().isOverlapped(u.getTunit()) == false, true);
				checkArgument(u.getTunit().getDelivery().getDeliveryTime().minus(u.getTunit().getDelivery().getStationToCYTravelTime()).minusMinutes(GlobalParameters.LOADING_MINUTES).isEqual(u.getTunit().getTimeSlot().getStartTime()), true);
				checkArgument(u.getTunit().getTimeSlot().getEndTime().compareTo(getTotalTimeRange().getEndTime()) <= 0 , true);
				checkArgument(u.getTunit().getTimeSlot().getStartTime().compareTo(getTotalTimeRange().getStartTime()) >= 0 , true);
				TruckDeliveryUnit tu = cl.deepClone(u.getTunit());
				checkArgument(truckAgent.getTruckSchedule().alreadyExist(tu) == false, true);
				truckAgent.getTruckSchedule().add(tu, u.getOrderReply());
				logger.debug(truckAgent.getId()+"T Schedule unit added in Trucks schedule (status= " +u.getOrderReply()+ ": " + u.getTunit().toString());
				
				for(TruckScheduleUnit tsu: iAnt.getFullSchedule()) {
					if (tsu instanceof TruckTravelUnit){
						if (truckAgent.getTruckSchedule().alreadyExist(tsu) == false){
							TruckTravelUnit ttu = cl.deepClone((TruckTravelUnit)tsu);
							truckAgent.getTruckSchedule().add(ttu);
							logger.debug(truckAgent.getId()+"T Travel unit added: " + ttu.toString());
						}
					}
				}
				newDeliveryUnitAdded =true;
			}
			else //update already existing unit
			{
				truckAgent.getTruckSchedule().updateUnitStatus(u.getTunit(), u.getOrderReply());
			}
		}
		return newDeliveryUnitAdded;
	}
	private void handleREJECTEDTunits(IntAnt iAnt) {
		for (communicateAbleUnit u : iAnt.getSchedule()){
			removeRejectedUnitFromSchedule(u);
		}
	}

	private void sentToPS(ExpAnt eAnt, ArrayList<AvailableSlot> pAvSlots) {
		checkArgument(pAvSlots.isEmpty() == false, true);
		if (truckAgent.getTruckSchedule().size() > 0) {
			if (pAvSlots.get(0).getStartTime().compareTo(truckAgent.getTruckSchedule().getSchedule().get(0).getTimeSlot().getEndTime()) >= 0){
				 //send to all PS
				for (ProductionSiteInitial ps : truckAgent.getSites()) {
					truckAgent.getcApi().send(ps, eAnt);
				}
				return;
			}			
		} //else to both if
		truckAgent.getcApi().send(truckAgent.getStartPS(), eAnt);//in the begining truck is at a PS, if we send ot all PS then latter PS to PS travel may be required, 
//		//which is'nt seem desirable at the moment
	}
	@Override
	public
	void sendExpAnts(long startTime) {
		final DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		//check  exp ants to be sent after particular interval only
		if (currTime.minusMinutes(timeForLastExpAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.EXPLORATION_INTERVAL_MIN ) {
			ExpAnt eAnt = new ExpAnt(truckAgent, Utility.getAvailableSlots(truckAgent.getTruckSchedule().getSchedule(), availableSlots,  
					new TimeSlot(new DateTime(currTime), getTotalTimeRange().getEndTime())), truckAgent.getTruckSchedule().getSchedule(), currTime);
			if (availableSlots.size()>0) {
				sentToPS(eAnt, availableSlots );
			}
			timeForLastExpAnt = currTime;
		}		
	}

	@Override
	public
	void sendIntAnts(long startTime) { 
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);//send int ants to book again the whole schedule..
		if (currTime.compareTo(timeForLastIntAnt) > 0) {
		if (currTime.minusMinutes(timeForLastIntAnt.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.INTENTION_INTERVAL_MIN ) {
			processExplorationAnts(startTime);
			if (bestAnt != null) {
				if (truckAgent.getTruckSchedule().scheduleStillValid(bestAnt.getSchedule())){
					ArrayList<communicateAbleUnit> tmp = truckAgent.getTruckSchedule().makeCommunicateAbleSchedule(bestAnt.getSchedule());
					ArrayList<TruckScheduleUnit> originalfullSchedule = truckAgent.getTruckSchedule().makeOriginalSchedule(bestAnt.getSchedule());
					IntAnt iAnt = new IntAnt(truckAgent, tmp,originalfullSchedule, currTime);
					logger.debug(truckAgent.getId()+"T int sent by Truck");
					//checkArgument(bestAnt.getSchedule().get(0) instanceof TruckDeliveryUnit, true); //truck should start from its start PS
					checkArgument(bestAnt.getSchedule().get(0).getStartLocation().equals(truckAgent.getStartLocation()));
					truckAgent.getcApi().send(((TruckDeliveryUnit)bestAnt.getSchedule().get(0)).getDelivery().getLoadingStation(), iAnt); 
					timeForLastIntAnt = currTime;
					bestAnt = null;
					return;
				}  
			}
			if (truckAgent.getTruckSchedule().size()> 0 && timeForLastIntAnt.equals(currTime) == false){//send old schedule to refresh bookings..
				ArrayList<communicateAbleUnit> tmp = truckAgent.getTruckSchedule().makeCommunicateAbleSchedule(truckAgent.getTruckSchedule().getSchedule());
				ArrayList<TruckScheduleUnit> originalfullSchedule = truckAgent.getTruckSchedule().makeOriginalSchedule(truckAgent.getTruckSchedule().getSchedule());
				IntAnt iAnt = new IntAnt(truckAgent, tmp,originalfullSchedule, currTime);
				logger.debug(truckAgent.getId()+"T int sent by Truck with Old schedule");
				TruckDeliveryUnit sendToTdu;
				if (truckAgent.getTruckSchedule().getSchedule().get(0) instanceof TruckDeliveryUnit)
					sendToTdu = (TruckDeliveryUnit)truckAgent.getTruckSchedule().getSchedule().get(0);
				else
					sendToTdu = (TruckDeliveryUnit)truckAgent.getTruckSchedule().getSchedule().get(1);
				truckAgent.getcApi().send(sendToTdu.getDelivery().getLoadingStation(), iAnt);  
				timeForLastIntAnt = currTime; //here no need to make bestAnt = null, since it coud compete with future explorations
			}
			//checkArgument(bestAnt.getSchedule().get(0).getStartLocation().equals(truckAgent.getStartLocation()));
		}
		}
	}

	private void printBestAnt(long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		logger.debug(truckAgent.getId()+"T Best schedule changed: total Units= " + bestAnt.getSchedule().size() +" , lagTime= "+ bestAnt.getScheduleLagTime()+ " and Score= " + bestAnt.getScheduleScore() +" & total ants="+ explorationAnts.size() + "currTime= " + currTime);
		for (TruckScheduleUnit unit: bestAnt.getSchedule()) {
			logger.debug(unit.toString());
		}
	}

	@Override
	public
	void followPracticalSchedule(TimeLapse timeLapse) {
		if (!truckAgent.getTruckSchedule().isEmpty()) {
			truckAgent.followSchedule(timeLapse);
		}		
	}

	@Override
	public
	void letMeBreak(long startTime) {
		shouldIBreak(startTime);		
	}

	@Override
	public void processGeneralAnts(long startTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void processOrderPlanInformerAnts(long startTime) {
		super.processOrderPlanInformerAnt();
	}

}
