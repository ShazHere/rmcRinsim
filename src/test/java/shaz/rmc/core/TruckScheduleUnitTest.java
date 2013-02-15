/**
 * 
 */
package shaz.rmc.core;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import shaz.rmc.core.domain.Delivery;

/**
 * @author Shaza
 *
 */
public class TruckScheduleUnitTest {

	Agent truck;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		truck = null; 
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link shaz.rmc.core.TruckScheduleUnit#TruckScheduleUnit(shaz.rmc.core.Agent)}.
	 */
	@Test
	public void testTruckScheduleUnit() {
		//fail("Not yet implemented");
		TruckScheduleUnit unit = new TruckScheduleUnit(truck);
	}

	/**
	 * Test method for {@link shaz.rmc.core.TruckScheduleUnit#getDelivery()}.
	 * @return 
	 */
	@Test
	public void testGetDelivery() {
		TruckScheduleUnit unit = new TruckScheduleUnit(truck);
		assertNull(unit.getDelivery());
		}

	/**
	 * Test method for {@link shaz.rmc.core.TruckScheduleUnit#setDelivery(shaz.rmc.core.domain.Delivery)}.
	 */
	@Test
	public void testSetDelivery() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link shaz.rmc.core.TruckScheduleUnit#getTimeSlot()}.
	 */
	@Test
	public void testGetTimeSlot() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link shaz.rmc.core.TruckScheduleUnit#setTimeSlot(shaz.rmc.core.TimeSlot)}.
	 */
	@Test
	public void testSetTimeSlot() {
		fail("Not yet implemented");
	}

	
}
