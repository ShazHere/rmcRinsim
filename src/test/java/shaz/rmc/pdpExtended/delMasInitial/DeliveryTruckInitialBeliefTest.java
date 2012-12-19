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
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.core.domain.Order;

public class DeliveryTruckInitialBeliefTest {

	ArrayList<TruckScheduleUnit> schedule;
	private ArrayList<TimeSlot> availableSlots;
	DeliveryTruckInitialBelief b;
	DateTime START_DATETIME;
	DateTime END_DATETIME;
	@Before
	public void setUp() throws Exception {
		START_DATETIME = new DateTime(2011, 1, 10, 11, 0, 0 ,0, GregorianChronology.getInstance());
		END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot (START_DATETIME, START_DATETIME.plusHours(1)));
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(4), 
				START_DATETIME.plusHours(5)));
		b = new DeliveryTruckInitialBelief(null, schedule );
		
		availableSlots = new ArrayList<TimeSlot>();
		availableSlots.add(b.getTotalTimeRange());
		availableSlots = Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange());
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test //TODO add more tests..
	public void testGetAvailableSlotsArrayListOfTruckScheduleUnit() { // schudel is booke at start and end
		

		//System.out.println(Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange()).get(0));
		//System.out.println(Utility.getAvailableSlots(schedule, availableSlots, b.getTotalTimeRange()).get(1));
		//System.out.println(b.getAvailableSlots().get(2));
		
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).size(),2);
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).get(0).toString(), "TimeSlot<start,end> = <2011-01-10T12:01:00.000+01:00, 2011-01-10T15:00:00.000+01:00>");
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).get(1).toString(),"TimeSlot<start,end> = <2011-01-10T16:01:00.000+01:00, 2011-01-10T23:55:00.000+01:00>");
	}
	
	@Test
	public void testGetAvailableSlotsStartAndEndFilled() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(2), 
				START_DATETIME.plusHours(4))); //schedule booked leaving an available slot at the startTime of truck
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(END_DATETIME.minusHours(2), 
				END_DATETIME)); //End time is booked..
		
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).get(0).toString() , "TimeSlot<start,end> = <2011-01-10T11:00:00.000+01:00, 2011-01-10T13:00:00.000+01:00>");
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).get(1).toString(), "TimeSlot<start,end> = <2011-01-10T15:01:00.000+01:00, 2011-01-10T21:55:00.000+01:00>");
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).size(), 2); //there should be two slots, one in begining, one at end
		
	}
	
	@Test //These tests could fail if total available time period for truck is less than 10 hours..
	public void testGetAvailableSlotsStartAndEndFree() {
		schedule = new ArrayList<TruckScheduleUnit>();
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(0).setTimeSlot(new TimeSlot(START_DATETIME.plusHours(2), 
				START_DATETIME.plusHours(4))); //schedule booked leaving an available slot at the startTime of truck
		
		schedule.add(new TruckScheduleUnit(null));
		schedule.get(1).setTimeSlot(new TimeSlot(END_DATETIME.minusHours(4), 
				END_DATETIME.minusHours(2).minusMinutes(5))); //There is slot even afer end time
		
		DeliveryTruckInitialBelief b = new DeliveryTruckInitialBelief(null, schedule);
		//System.out.println(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).size());

		assertEquals(Utility.getAvailableSlots(schedule,b.availableSlots, b.getTotalTimeRange()).size(), 3); //there should be three slots, at begining, middle, end
		assertEquals(Utility.getAvailableSlots(schedule,availableSlots, b.getTotalTimeRange()).get(1).toString(), "TimeSlot<start,end> = <2011-01-10T15:01:00.000+01:00, 2011-01-10T19:55:00.000+01:00>");
//		System.out.println(b.getAvailableSlots().get(0));
//		System.out.println(b.getAvailableSlots().get(1));
//		System.out.println(b.getAvailableSlots().get(2));	
	}
//	@Test 
//	public void testScheduleStillValidWithOneUnit() {
//		ArrayList<TruckScheduleUnit> oldSch = new ArrayList<TruckScheduleUnit>();
//		ArrayList<TruckScheduleUnit> newSch = new ArrayList<TruckScheduleUnit>();
//		
//		oldSch.add(new TruckScheduleUnit(null));
//		oldSch.get(0).setDelivery(new Delivery(new OrderAgentInitial(null, null, new Order("20", null, 0, 0, null, null, true, true, "", null)), 0, null, 0, null, null));
//		newSch.add(new TruckScheduleUnit(null));
//		newSch.get(0).setDelivery(new Delivery(new OrderAgentInitial(null, null, new Order("20", null, 0, 0, null, null, true, true, "", null)), 0, null, 0, null, null));
//		
//		assertTrue(new DeliveryTruckInitialBelief(null, null).scheduleStillValid(oldSch, newSch));
//	}

}
