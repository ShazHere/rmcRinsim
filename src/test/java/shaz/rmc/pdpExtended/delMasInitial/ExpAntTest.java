package shaz.rmc.pdpExtended.delMasInitial;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.chrono.GregorianChronology;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.Utility;
import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.pdpExtended.delMasInitial.communication.ExpAnt;

public class ExpAntTest {

	ArrayList<TruckScheduleUnit> schedule;
	private ArrayList<TimeSlot> availableSlots;
	DateTime START_DATETIME;
	DateTime END_DATETIME;
	@Before
	public void setUp() throws Exception {
		START_DATETIME = new DateTime(2011, 1, 10, 11, 0, 0 ,0, GregorianChronology.getInstance());
		END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());
		schedule = new ArrayList<TruckScheduleUnit>();
		availableSlots = new ArrayList<TimeSlot>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot (START_DATETIME, START_DATETIME.plusHours(2)));
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(4).plusMinutes(5), 
				START_DATETIME.plusHours(4).plusMinutes(20)));
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		availableSlots.add(b.getTotalTimeRange());
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test //if order is interested at startTime of truck, which is already booked
	public void testIsInterested() {
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, new Vehicle("1", null, 10000, 10000, 10000)),  availableSlots, schedule,START_DATETIME, 1);
		//System.out.println(availableSlots.get(0).toString());
		assertFalse(exp.isInterested(START_DATETIME, 0d, START_DATETIME));
	}
	@Test
	public void testIsInterestedAtMIddleTime() {
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null,  new Vehicle("1", null, 10000, 10000, 10000)),  availableSlots, schedule, START_DATETIME, 1);
		//System.out.println(availableSlots.get(0).toString());
		assertTrue(exp.isInterested(START_DATETIME.plusHours(2).plusMinutes(2), 0d, START_DATETIME));
	}
	@Test //if this test is failing then check the plan size vs truck speed and the hours or time compared in isInterested method!
	public void testIsInterestedAtMIddleTimeSmallDistance() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(START_DATETIME, 
				START_DATETIME.plusHours(2)));
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, new Vehicle("1", null, 10000, 10000, 10000)),  availableSlots, schedule, START_DATETIME, 1);
		//System.out.println(availableSlots.get(0).toString());
		assertTrue(exp.isInterested(START_DATETIME.plusHours(4), 6d, START_DATETIME));
		//System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
	}
	@Test //if this test is failing then check the plan size vs truck speed and the hours or time compared in isInterested method!
	public void testIsInterestedAtMIddleTimeLongDistance() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(3), 
				START_DATETIME.plusHours(4))); //already there is 2 hours booking in trucks schedule
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(END_DATETIME.minusHours(1), 
				END_DATETIME)); //already there is 2 hours booking in trucks schedule
		
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, new Vehicle("1", null, 10000, 10000, 10000)),  availableSlots, schedule, START_DATETIME, 1);
		System.out.println(availableSlots.get(0));
		assertTrue(exp.isInterested(START_DATETIME.plusHours(1), 2d, START_DATETIME));
		//with current speed of truck, the order at distance of 6 units and 2 hours gap
		assertFalse(exp.isInterested(END_DATETIME.minusHours(2), 0d, START_DATETIME)); //second slot could't be available
		//sinc at the moment expAnt only move with current time
	}
	@Test
	public void testIsInterestedAfterScheduleChange() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(3), 
				START_DATETIME.plusHours(4))); //already there is 2 hours booking in middle of trucks schedule
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(END_DATETIME.minusHours(1), 
				END_DATETIME)); //already there is 1 hours booking in end of trucks schedule
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
		ExpAnt exp = new ExpAnt(new DeliveryTruckInitial(null, new Vehicle("1", null, 10000, 10000, 10000)),  availableSlots, schedule, START_DATETIME, 1);
		System.out.println(availableSlots.get(0));
		assertTrue(exp.isInterested(START_DATETIME.plusHours(1), 2d, START_DATETIME)); //half hour in the begining
		//with current speed of truck, the order at distance of 6 units and 2 hours gap
//		System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
		exp.getCurrentUnit().getTimeSlot().setEndtime(exp.getCurrentUnit().getTimeSlot().getStartTime().plusMinutes(30));
		System.out.println(exp.getCurrentUnit().getTimeSlot().toString());
		System.out.println(exp.getAvailableSlots().size());
		exp.getSchedule().add(exp.getCurrentUnit()); 
		assertFalse(exp.isInterested(START_DATETIME.plusHours(1), 2d, START_DATETIME)); //requesting for same isInterested as above, even after adding it in schedule
		//System.out.println(availableSlots.get(0).toString());
		assertEquals(exp.getAvailableSlots().size(), 1); //since first slot is less than 2 hours now..
		//System.out.println(availableSlots.get(1).toString());
	}

}
