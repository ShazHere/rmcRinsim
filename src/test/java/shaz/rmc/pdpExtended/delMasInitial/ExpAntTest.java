package shaz.rmc.pdpExtended.delMasInitial;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;

public class ExpAntTest {

	ArrayList<TruckScheduleUnit> schedule;
	private ArrayList<TimeSlot> availableSlots;
	@Before
	public void setUp() throws Exception {
		schedule = new ArrayList<TruckScheduleUnit>();
		availableSlots = new ArrayList<TimeSlot>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot (GlobalParameters.START_DATETIME, GlobalParameters.START_DATETIME.plusHours(2)));
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(GlobalParameters.START_DATETIME.plusHours(4).plusMinutes(5), 
				GlobalParameters.START_DATETIME.plusHours(4).plusMinutes(20)));
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null);
		availableSlots.add(b.getTotalTimeRange());
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test //if order is interested at startTime of truck, which is already booked
	public void testIsInterested() {
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, null),  availableSlots, schedule, new DateTime(), 1);
		//System.out.println(availableSlots.get(0).toString());
		assertFalse(exp.isInterested(GlobalParameters.START_DATETIME, 0d));
	}
	@Test
	public void testIsInterestedAtMIddleTime() {
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, null),  availableSlots, schedule, new DateTime(), 1);
		//System.out.println(availableSlots.get(0).toString());
		assertTrue(exp.isInterested(GlobalParameters.START_DATETIME.plusHours(2).plusMinutes(2), 0d));
	}
	@Test //if this test is failing then check the plan size vs truck speed and the hours or time compared in isInterested method!
	public void testIsInterestedAtMIddleTimeSmallDistance() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(GlobalParameters.START_DATETIME, 
				GlobalParameters.START_DATETIME.plusHours(2)));
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null);
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, null),  availableSlots, schedule, new DateTime(), 1);
		//System.out.println(availableSlots.get(0).toString());
		assertTrue(exp.isInterested(GlobalParameters.START_DATETIME.plusHours(4), 6d));
		//System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
	}
	@Test //if this test is failing then check the plan size vs truck speed and the hours or time compared in isInterested method!
	public void testIsInterestedAtMIddleTimeLongDistance() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(GlobalParameters.START_DATETIME.plusHours(3), 
				GlobalParameters.START_DATETIME.plusHours(4))); //already there is 2 hours booking in trucks schedule
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(GlobalParameters.END_DATETIME.minusHours(1), 
				GlobalParameters.END_DATETIME)); //already there is 2 hours booking in trucks schedule
		
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null);
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, null),  availableSlots, schedule, new DateTime(), 1);
		System.out.println(availableSlots.get(0));
		assertTrue(exp.isInterested(GlobalParameters.START_DATETIME.plusHours(1), 2d));
		//with current speed of truck, the order at distance of 6 units and 2 hours gap
		assertFalse(exp.isInterested(GlobalParameters.END_DATETIME.minusHours(2), 0d)); //second slot could't be available
		//sinc at the moment expAnt only move with current time
	}
	@Test
	public void testIsInterestedAfterScheduleChange() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(GlobalParameters.START_DATETIME.plusHours(3), 
				GlobalParameters.START_DATETIME.plusHours(4))); //already there is 2 hours booking in trucks schedule
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(GlobalParameters.END_DATETIME.minusHours(1), 
				GlobalParameters.END_DATETIME)); //already there is 2 hours booking in trucks schedule
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null);
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, null),  availableSlots, schedule, new DateTime(), 1);
		System.out.println(availableSlots.get(0));
		assertTrue(exp.isInterested(GlobalParameters.START_DATETIME.plusHours(1), 2d));
		//with current speed of truck, the order at distance of 6 units and 2 hours gap
		System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
		exp.getCurrentUnit().getTimeSlot().setEndtime(exp.getCurrentUnit().getTimeSlot().getStartTime().plusMinutes(30));
		System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
		exp.getSchedule().add(exp.getCurrentUnit());
		assertFalse(exp.isInterested(GlobalParameters.START_DATETIME.plusHours(1), 2d)); //requesting for same isInterested as above, even after adding it in schedule
		//System.out.println(availableSlots.get(0).toString());
		System.out.println(exp.getAvailableSlots().get(0).toString());
		System.out.println(exp.getAvailableSlots().get(1).toString());
		
	}

}
