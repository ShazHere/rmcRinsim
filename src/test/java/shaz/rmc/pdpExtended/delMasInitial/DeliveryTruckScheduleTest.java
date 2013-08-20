/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.chrono.GregorianChronology;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.core.graph.Point;
import shaz.rmc.core.Reply;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.core.TruckDeliveryUnit;
import shaz.rmc.core.TruckScheduleUnit;
import shaz.rmc.core.TruckTravelUnit;
import shaz.rmc.core.domain.Delivery;

/**
 * @author Shaza
 *
 */
public class DeliveryTruckScheduleTest {

	DateTime START_DATETIME;
	DateTime END_DATETIME;
	Delivery dummyDel;
	Delivery dummyDel1;
	Delivery dummyDel2;
	OrderAgentInitial dummyOrd;
	OrderAgentInitial dummyOrd1;
	ProductionSiteInitial dummyPS;
	DeliveryTruckInitial dummyTruck;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		START_DATETIME = new DateTime(2011, 1, 10, 11, 0, 0 ,0, GregorianChronology.getInstance());
		END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());
		dummyDel = mock(Delivery.class);
		dummyDel1 = mock(Delivery.class);
		dummyDel2 = mock(Delivery.class);
		dummyOrd = mock (OrderAgentInitial.class);
		dummyOrd1 = mock (OrderAgentInitial.class);
		dummyPS = mock(ProductionSiteInitial.class);
		dummyTruck = mock(DeliveryTruckInitial.class);
		
		when(dummyOrd.getPosition()).thenReturn(new Point(4,4));
		when(dummyOrd1.getPosition()).thenReturn(new Point(5,5));
		when(dummyPS.getLocation()).thenReturn(new Point(6,6));
		when(dummyTruck.getSpeed()).thenReturn(GlobalParameters.TRUCK_SPEED);
		
		when(dummyDel.getOrder()).thenReturn(dummyOrd);
		when(dummyDel1.getOrder()).thenReturn(dummyOrd1);
		when(dummyDel2.getOrder()).thenReturn(dummyOrd);
		
		when(dummyDel.getLoadingStation()).thenReturn(dummyPS);
		when(dummyDel1.getLoadingStation()).thenReturn(dummyPS);
		when(dummyDel2.getLoadingStation()).thenReturn(dummyPS);
		
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAdd_isSortedAfterAdding() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
		TimeSlot ts =  new TimeSlot( START_DATETIME.plusMinutes(70), START_DATETIME.plusMinutes(170));
		schedule.add(new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(70), START_DATETIME.plusMinutes(170)), dummyDel, 0, new Duration(0) ));
		//schedule.add(new TruckTravelUnit(null, ts, new Point (3,3), new Point (4,4), new Duration(400)));
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(0) );
		TruckTravelUnit ttu = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), null, null, new Duration(500));
		dts.add(tdu);
		dts.add(tdu1);
		dts.add(ttu);

		assertTrue(dts.getSchedule().size() == 4);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		assertTrue(dts.getSchedule().get(2).equals(ttu));
		assertTrue(dts.getSchedule().get(3).equals(tdu1));
	}
	
	@Test
	public void testRemove_middleTTUbeforeTSURemoved() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
		//TimeSlot ts =  new TimeSlot( START_DATETIME.plusMinutes(70), START_DATETIME.plusMinutes(170));
		//schedule.add(new TruckTravelUnit(null, ts, new Point (3,3), new Point (4,4), new Duration(400)));
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(200) );
		TruckTravelUnit ttu = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), null, null, new Duration(500));
		dts.add(tdu);
		dts.add(tdu1);
		dts.add(ttu);
		
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		assertTrue(dts.getSchedule().get(1).equals(ttu));
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		
		dts.remove(tdu1);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
	}
	
	@Test
	public void testRemove_middleTTUafterTSURemoved() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
		//TimeSlot ts =  new TimeSlot( START_DATETIME.plusMinutes(70), START_DATETIME.plusMinutes(170));
		//schedule.add(new TruckTravelUnit(null, ts, new Point (3,3), new Point (4,4), new Duration(400)));
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(21) );
		TruckTravelUnit ttu = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), null, null, new Duration(500));
		dts.add(tdu);
		dts.add(tdu1);
		dts.add(ttu);
		
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		assertTrue(dts.getSchedule().get(1).equals(ttu));
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		
		dts.remove(tdu);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu1));
	}
	
	@Test
	public void testadjustTruckSchedule_isTTUAdjustedAfterRemovingMiddleTDU() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
	
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(600) );
		TruckDeliveryUnit tdu2 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(1000), START_DATETIME.plusMinutes(2000)), dummyDel2, 0, new Duration(900) );

		TruckTravelUnit ttu_tduANDtdu1 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), dummyDel.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		TruckTravelUnit ttu_tdu1ANDtdu2 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(301), START_DATETIME.plusMinutes(600)), dummyDel1.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		
		dts.add(tdu);
		dts.add(tdu1);
		dts.add(ttu_tduANDtdu1);
		dts.add(tdu2);
		dts.add(ttu_tdu1ANDtdu2);
		
		//scheduleCondition
		assertTrue(dts.getSchedule().size() == 5);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		assertTrue(dts.getSchedule().get(1).equals(ttu_tduANDtdu1));
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		assertTrue(dts.getSchedule().get(3).equals(ttu_tdu1ANDtdu2));
		assertTrue(dts.getSchedule().get(4).equals(tdu2));
		
		dts.remove(tdu1);
		
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(1) instanceof TruckTravelUnit);
		assertTrue(dts.getSchedule().get(1).getStartLocation().equals(new Point(4,4))
				&& dts.getSchedule().get(1).getEndLocation().equals(new Point(6,6)));

	}

	@Test
	public void testadjustTruckSchedule_withRoughtSequence() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
	
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(600) );
		TruckDeliveryUnit tdu2 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(1000), START_DATETIME.plusMinutes(2000)), dummyDel2, 0, new Duration(900) );

		TruckTravelUnit ttu_tduANDtdu1 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), dummyDel.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		TruckTravelUnit ttu_tdu1ANDtdu2 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(301), START_DATETIME.plusMinutes(600)), dummyDel1.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		TruckTravelUnit ttu_tduANDtdu2 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(800), START_DATETIME.plusMinutes(900)), dummyDel.getOrder().getPosition(), dummyPS.getLocation(), new Duration(900));
		
		dts.add(tdu);
		dts.add(ttu_tduANDtdu2);
		dts.add(tdu2);
		
		dts.remove(tdu);
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu2));
		
		dts.add(tdu);
		dts.add(ttu_tduANDtdu2);
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		
		dts.remove(tdu);
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu2));
		
		dts.add(tdu);
		dts.add(ttu_tduANDtdu2);
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		
		dts.remove(tdu);
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu2));
		
		dts.add(tdu);
		dts.add(ttu_tduANDtdu1);
		dts.add(ttu_tdu1ANDtdu2);
		dts.add(tdu1);
		assertTrue(dts.getSchedule().size() == 5);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		assertTrue(dts.getSchedule().get(4).equals(tdu2));
		
		dts.remove(tdu);
		dts.adjustTruckSchedule(dummyTruck);
		dts.remove(tdu1);
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu2));
		
		dts.add(tdu);
		dts.add(ttu_tduANDtdu2);
		assertTrue(dts.getSchedule().size() == 3);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		
		dts.remove(tdu2);
		dts.adjustTruckSchedule(dummyTruck);
		assertTrue(dts.getSchedule().size() == 1);
		assertTrue(dts.getSchedule().get(0).equals(tdu));
		
		dts.add(tdu1);
		dts.add(ttu_tduANDtdu1);
		dts.add(ttu_tdu1ANDtdu2);
		dts.add(tdu2);
		assertTrue(dts.getSchedule().size() == 5);
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		
	}
	
	@Test
	public void testMakePracticalSchedule_With5TSU() {
		ArrayList<TruckScheduleUnit> schedule = new ArrayList<TruckScheduleUnit>();
		
		DeliveryTruckSchedule dts = new DeliveryTruckSchedule(schedule);
		TruckDeliveryUnit tdu = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(7), START_DATETIME.plusMinutes(60)), dummyDel, 0, new Duration(0) );
		TruckDeliveryUnit tdu1 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(200), START_DATETIME.plusMinutes(300)), dummyDel1, 0, new Duration(600) );
		TruckDeliveryUnit tdu2 = new TruckDeliveryUnit(null,new TimeSlot( START_DATETIME.plusMinutes(1000), START_DATETIME.plusMinutes(2000)), dummyDel2, 0, new Duration(900) );

		TruckTravelUnit ttu_tduANDtdu1 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(175), START_DATETIME.plusMinutes(199)), dummyDel.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		TruckTravelUnit ttu_tdu1ANDtdu2 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(301), START_DATETIME.plusMinutes(600)), dummyDel1.getOrder().getPosition(), dummyPS.getLocation(), new Duration(500));
		TruckTravelUnit ttu_tduANDtdu2 = new TruckTravelUnit(null, new TimeSlot(START_DATETIME.plusMinutes(800), START_DATETIME.plusMinutes(900)), dummyDel.getOrder().getPosition(), dummyPS.getLocation(), new Duration(900));
		
		dts.add(tdu, Reply.ACCEPT);
		dts.add(tdu1, Reply.ACCEPT);
		dts.add(ttu_tduANDtdu1);
		dts.add(ttu_tdu1ANDtdu2);
		dts.add(tdu2, Reply.ACCEPT);
		assertTrue(dts.getSchedule().size() == 5);
		assertTrue(dts.getSchedule().get(2).equals(tdu1));
		
		dts.makePracticalSchedule(dummyTruck);
		
		assertTrue(dts.getSchedule().size() == dts.getPracticalSchedule().size());
		assertTrue(dts.getSchedule().get(2).getStartLocation().equals(dts.getPracticalSchedule().get(2).getStartLocation())
				&& dts.getSchedule().get(2).getEndLocation().equals(dts.getPracticalSchedule().get(2).getEndLocation())
				&& dts.getSchedule().get(2).getTimeSlot().compareTo(dts.getPracticalSchedule().get(2).getTimeSlot()) == 0);
		
		assertTrue(dts.getSchedule().get(3).getStartLocation().equals(dts.getPracticalSchedule().get(3).getStartLocation())
				&& dts.getSchedule().get(3).getEndLocation().equals(dts.getPracticalSchedule().get(3).getEndLocation())
				&& dts.getSchedule().get(3).getTimeSlot().compareTo(dts.getPracticalSchedule().get(3).getTimeSlot()) == 0);
		
	}

}
