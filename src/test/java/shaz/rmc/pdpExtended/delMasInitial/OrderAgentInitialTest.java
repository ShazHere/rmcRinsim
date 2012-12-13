/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.joda.time.DateTime;
import org.joda.time.chrono.GregorianChronology;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationModel;
import rinde.sim.core.model.road.PlaneRoadModel;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;

/**
 * @author Shaza
 *
 */
public class OrderAgentInitialTest {
	
	protected ArrayList<ExpAnt> explorationAnts;
	ArrayList<TimeSlot> availableSlots; 
	PlaneRoadModel prm;
	CommunicationModel communicationModel;
	DateTime START_DATETIME;
	DateTime END_DATETIME;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		RmcSimulation rmSim = new RmcSimulation(); 
		rmSim.loadProblem(null);
		START_DATETIME = new DateTime(2011, 1, 10, 11, 0, 0 ,0, GregorianChronology.getInstance());
		END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());
		availableSlots = new ArrayList<TimeSlot>();
		availableSlots.add(new TimeSlot(START_DATETIME, END_DATETIME));
		explorationAnts = new ArrayList<ExpAnt>();
		
		final RandomGenerator rng = new MersenneTwister(250);
		prm = new PlaneRoadModel(new Point(0, 0), new Point(10, 10), false, 10);//10by10 km plane
		CommunicationModel communicationModel = new CommunicationModel(rng, true);
		prm.addObjectAt(new ProductionSiteInitial(rng, GlobalParameters.PROBLEM.getStations().get(0) ), new Point (5,5));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testProcessExplorationAntsEmpty() {
		OrderAgentInitial orAg = new OrderAgentInitial(null, null, GlobalParameters.PROBLEM.getOrders().get(1));
		orAg.explorationAnts = explorationAnts;
		assertFalse(orAg.processExplorationAnts((long)START_DATETIME.plusSeconds(2).getMillis()));
		
	}
	
	@Test
	public void testProcessExplorationAntsReservedFalse() {
		long currTime = (long)START_DATETIME.plusSeconds(2).getMillis();
		OrderAgentInitial orAg = new OrderAgentInitial(null, null, PROBLEM.getOrders().get(1));
		orAg.explorationAnts = explorationAnts;
		ExpAnt eAnt = new ExpAnt(new DeliveryTruckInitial(null, null), new ArrayList(availableSlots), new ArrayList<TruckScheduleUnit>(), GlobalParameters.START_DATETIME.plusSeconds(2), 1);
		orAg.explorationAnts.add(eAnt);
		orAg.roadModel = prm;
		orAg.setCommunicationAPI(communicationModel);
		assertTrue(eAnt.isInterested(orAg.getOrder().getEarliestStartTime(), 2d));
		assertTrue(orAg.processExplorationAnts(currTime));
		
	}
	@Test //Comment the cApi.send line if failing TODO: fix this testing bug
	public void testProcessExplorationAnts1Exp() {
		long currTime = (long)GlobalParameters.START_DATETIME.plusSeconds(2).getMillis();
		OrderAgentInitial orAg = new OrderAgentInitial(null, null, GlobalParameters.PROBLEM.getOrders().get(1));
		orAg.explorationAnts = explorationAnts;
		ExpAnt eAnt = new ExpAnt(new DeliveryTruckInitial(null, null), new ArrayList(availableSlots), new ArrayList<TruckScheduleUnit>(), GlobalParameters.START_DATETIME.plusSeconds(2), 1);
		orAg.explorationAnts.add(eAnt);
		orAg.roadModel = prm;
		assertTrue(eAnt.isInterested(orAg.getOrder().getEarliestStartTime(), 1d));
		System.out.println(orAg.getInterestedTime());
		System.out.println(eAnt.getCurrentUnit().getTimeSlot().toString());
	}

}
