package shaz.rmc.core.domain;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import shaz.rmc.core.Agent;
import shaz.rmc.core.TimeSlot;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;

import java.util.Comparator;
import java.util.logging.Logger;

import com.sun.org.apache.bcel.internal.generic.*;

public class Station implements Location {

	private final String id;
	private final Duration loadingDuration;
	private final int loadingDurationInMin;
	private List<StationBookingUnit> availabilityList;
	
	public Station(final String id, final Duration loadingDuration) {
		this.id = id;
		this.loadingDuration = loadingDuration;
		this.loadingDurationInMin = 5;//considering 5min loading duraion since for all stations its same..
			//(int)loadingDuration.getMillis()/(1000000*60);
		availabilityList = new ArrayList<StationBookingUnit>();
	}
	
	public String getId() {
		return id;
	}
	
	public Duration getLoadingDuration() {
		return loadingDuration;
	}
	
	
	public List<StationBookingUnit> getAvailabilityList() {
		return availabilityList;
	}
	
	/**
	 * @param dt
	 * @param truck
	 * @return DATETIME, the time aloted to the truck for loading. This could be exact time according to truck's own specified time (i.e same return value as the parameter given), 
	 * or it will be nearest available time available for loading at the station.
	 */
	public synchronized DateTime makeBookingAt (DateTime dt, Agent truck, Delivery del, DateTime currTime){
		//evaporateBookings(currTime);
		int slotNo = dt.getHourOfDay()*12 + (dt.getMinuteOfHour()/loadingDurationInMin)-1; 
		StationBookingUnit unit = new StationBookingUnit(truck, new TimeSlot(dt, new DateTime (dt.plus(loadingDuration ))), del, currTime);
		int sameUnitLoc = unitWithSlotExist(slotNo); 
		int hour = 0,min = 0;
		if ( sameUnitLoc >= 0) { //means some other booking at the same slot..
			if (availabilityList.get(sameUnitLoc).getTruck().equals(truck) ) // same truck made the previous booking
			{
				if (del.equalsWithSameTruck(availabilityList.get(sameUnitLoc).getDelivery())) { //means same truck with same delivery
					availabilityList.get(sameUnitLoc).setRefreshTime(currTime); //refresh the booking time..
					System.out.println("Station bookings Refreshed!");
					return new DateTime(0); //so that PS keeps the reply=WEEK_ACCEPT which could be previously UNDER_PROCESS
				}
			}
			else
				return null;
			///no fsearch of a good time slot if the required slot is booked.. fixed at 20/08/2012
			/*slotNo = getNearestNextAvailableSlot(sameUnitLoc);
			hour = (((slotNo+1)*loadingDurationInMin) - (((slotNo+1)*loadingDurationInMin)%60))/60;
			min = ((slotNo+1)*loadingDurationInMin)%60 + loadingDurationInMin -1  ;
			unit.getTimeSlot().setStartTime((unit.getTimeSlot().getStartTime().plusHours(hour)).plusMinutes(min));
			unit.getTimeSlot().setEndtime(unit.getTimeSlot().getStartTime().plusMinutes(loadingDurationInMin));
*/
		}
		//TODO check what to do if station fully booked..
		unit.setSlotNo(slotNo);	
		if (hour > 23)  //basically this means over booking problem
			return null;
		availabilityList.add(unit);
		//sort the availabilityList according to slotNo.
		Collections.sort(availabilityList, new Comparator<StationBookingUnit>(){
	        public int compare( StationBookingUnit a, StationBookingUnit b ){
	            return a.slotNo - b.slotNo;
	        }
		});
		if (sameUnitLoc >= 0) { //this wont execute any way..24/01/2013
			DateTime bookingTime = new DateTime(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth(), hour, min, 0, 0, dt.getChronology());
			return bookingTime; //the slotNo that is actually booked for the truck..
		}
		else
			return dt;
	}

	/**
	 * @param slotNo
	 * @return returns location of the unit having same slotNo in the availabilityList, else returns -1
	 */
	private int unitWithSlotExist(int slotNo) {
		int i = 0;
		if (!availabilityList.isEmpty()) {
			for (StationBookingUnit u : availabilityList) {
				if (u.getSlotNo() == slotNo)
					return i;
				i++;
			}
		}
		return -1;
	}

	/**
	 * @param unitId , idOfUnit having same slot no.
	 * @return returns next nearest available slot according to units in availabilityList
	 */
	private int getNearestNextAvailableSlot(int unitId) {
		if (!availabilityList.isEmpty()) {
			if(availabilityList.size() == 1)
				return availabilityList.get(0).getSlotNo() + 1;
			int i = unitId;
			for (i = unitId; i<availabilityList.size()-1; i ++){
				if (availabilityList.get(i+1).getSlotNo() - availabilityList.get(i).getSlotNo() > 1) //if diff is >1 then means next available can be returned
					return availabilityList.get(i).getSlotNo() + 1;
			}
			return availabilityList.get(i).getSlotNo() + 1;
		}
		return -1; //means station is fully booked after unitId
	}

	//May be check latter if its is required or not..3/5/2012
	public boolean availableAtTimeT(StationBookingUnit ts) {
		return false;
	}
	
	public void evaporateBookings(DateTime currTime) {
		ArrayList<StationBookingUnit> removeAbleBookings = new ArrayList<StationBookingUnit>();
		for (StationBookingUnit sbu : availabilityList) {
			long currMilli = currTime.getMillis() - sbu.getRefreshTime().getMillis();
			if (new Duration (currMilli).getStandardMinutes() >= GlobalParameters.INTENTION_EVAPORATION_MIN) { //evaporate if never reAssured by truck
				removeAbleBookings.add(sbu);
			}
		}
		int previousAvailabilitySize = availabilityList.size();
		if (!removeAbleBookings.isEmpty()) {
			for (StationBookingUnit u : removeAbleBookings) {
				availabilityList.remove(u);
			}
		}
		checkArgument(availabilityList.size() == previousAvailabilitySize - removeAbleBookings.size(), true);
	}
	public class StationBookingUnit {
		//private int reAssuredTimes; //to keep track, how many times the truck re-assured the same booking of PS.
		private DateTime refreshTime;
		final private Agent truck;
		final private Delivery delivery;
		private TimeSlot timeSlot; //think should it be directly timeslot of delivery or it shud include travel time etc.
		private int slotNo; /*This is a special no. which represents, which 5min slot of station this is? The value ranges from 0 to 288-1. (Since 24 hours will have 12*24 5min slots)
				//which makes in total 288 bookable slots for a station). Each time when a booking is added in schedule array of station, the slot no. should be calculated in the following way:
				 * let bt represent booking start time,			10:30 to 10:35 is for instance loading time
				 * then bt.hours part = h						10
				 * bt.min part = m								30
				 * => h*12 = 12h								12*10 = 120
				 * => slotNo = 12h + m/5 -1						12 + (30/5) = 126
				 *											=>slotNo = 126-1 = 125 					
				*/
		
//		public StationBookingUnit() {
//			truck = null;
//			timeSlot = null;
//			slotNo = -1;
//		}
		
		public StationBookingUnit(Agent truck , TimeSlot ts, Delivery del, DateTime curTime) {
			this.truck = truck;
			this.timeSlot = ts;
			this.slotNo = -1;
			this.delivery = del;
			this.refreshTime = curTime;
			//reAssuredTimes = 0;
		}

		public Agent getTruck() {
			return truck;
		}

//		public void setTruck(Agent truck) {
//			this.truck = truck;
//		}

		public TimeSlot getTimeSlot() {
			return timeSlot;
		}

		public void setTimeSlot(TimeSlot timeSlot) {
			this.timeSlot = timeSlot;
		}

		public int getSlotNo() {
			return slotNo;
		}

		public void setSlotNo(int slotNo) {
			this.slotNo = slotNo;
		}

//		public DateTime getReAssuredTimes() {
//			return reAssuredTimes;
//		}
//		/**
//		 * increases by one
//		 */
//		public void setReAssuredTimes() {
//			this.reAssuredTimes += 1;
//		}

		public Delivery getDelivery() {
			return delivery;
		}

		public DateTime getRefreshTime() {
			return refreshTime;
		}

		public void setRefreshTime(DateTime refreshTime) {
			this.refreshTime = refreshTime;
		}

//		public void setDelivery(Delivery delivery) {
//			this.delivery = delivery;
//		}
		
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[id="+id+",loadingDuration="+loadingDuration+"]";
	}
	public String pringAvailabilityList() {
		StringBuilder sb = new StringBuilder();
		
		sb.append("List[");
		for (int i = 0; i< availabilityList.size();i++){
			sb.append("\n  truck=").append(availabilityList.get(i).truck.getId() + ", slotNo=" + (availabilityList.get(i).slotNo) + " at time=" + availabilityList.get(i).getTimeSlot().getStartTime());
		}
		sb.append("]");
		
		return sb.toString();
	}

	public boolean removeBookingAt(DateTime startTime, Agent truck) {
		int slotNo = startTime.getHourOfDay()*12 + (startTime.getMinuteOfHour()/loadingDurationInMin)-1;
		Iterator<StationBookingUnit> i = availabilityList.iterator();
		while (i.hasNext())  {
			StationBookingUnit u = i.next();
			if (u.slotNo == slotNo) {
				i.remove();
				return true;
			}
		}
		return false;
	}
	
	
}
