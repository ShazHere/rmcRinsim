/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import static java.util.Collections.unmodifiableSet;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.Simulator;
import rinde.sim.core.model.road.PlaneRoadModel;
import shaz.rmc.core.domain.Order;


/**
 * This class should handle dynamic orders as well. It should create orderAgents, at appropriate times during simulation. 
 * @author Shaza
 *
 */
//TODO to make sure only one instance of this class is created, other wise throw exception on creation of second instance.
public class OrderManagerInitial implements TickListener {
	private final Logger logger; //for logging
	
	private final Simulator sim;
	private final RandomGenerator rng;
	private final PlaneRoadModel prm;
	Set<OrderAgentInitial> orderSet = new HashSet<OrderAgentInitial>();
	private DateTime lastTimeOrdersChecked; /** to keep track of Order startTime check, based on which orders should be created */
	private boolean loadBasicOrders = false;
	
	public OrderManagerInitial(Simulator pSim, RandomGenerator pRand, PlaneRoadModel pPrm) {
		rng = pRand;
		sim = pSim;
		prm = pPrm;
		lastTimeOrdersChecked = new DateTime(0);
		loadBasicOrders = false; // make it true if we only need the basic 3 orders to be loaded..
		logger = Logger.getLogger(OrderManagerInitial.class);
	}
	@Override
	public void tick(TimeLapse timeLapse) {
		if (loadBasicOrders == false)
			addOrderInTick(timeLapse);
	}
	private void addOrderInTick(TimeLapse timeLapse) {
		ArrayList<String> orderIdList = new ArrayList<String>(); 
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		if (currTime.minusMinutes(lastTimeOrdersChecked.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.FEASIBILITY_INTERVAL_MIN ){
			Iterator<Order> i = GlobalParameters.PROBLEM.getOrders().iterator();
			while(i.hasNext()) {
				Order ord = i.next();
				if (currTime.plusHours(2).compareTo(ord.getEarliestStartTime()) >= 0)  { //Xhours earlier the from their start times, orders will be added
					logger.debug("index of order to be added is = " + GlobalParameters.PROBLEM.getOrders().indexOf(ord));
					if (addOrder(ord, timeLapse.getStartTime()))
						orderIdList.add(ord.getId());
				}
			}
			for (String id: orderIdList) {
				Iterator<Order> k = GlobalParameters.PROBLEM.getOrders().iterator();
				while(k.hasNext()) {
					Order ord = k.next();
					if (id.equals(ord.getId())) {
						GlobalParameters.PROBLEM.getOrders().remove(ord);
						break;
					}
				}
			}
			lastTimeOrdersChecked = currTime;
		}
	}
	
	/**
	 * Should be called after registering orderManagerInitial.If the method is not called at all, no orders will be added, except if they are handled in tick() method
	 */
	public void addOrders() {
		if (loadBasicOrders)
			addBasicOrders();
		else {
			int orderToBeAdded = GlobalParameters.INPUT_INSTANCE_TYPE.getPerOfX(GlobalParameters.PROBLEM.getOrders().size());
			logger.debug("Orders to be added at Start is=  " + orderToBeAdded);
			for (int k = 0; k < orderToBeAdded; k++) {
				int index = this.rng.nextInt(GlobalParameters.PROBLEM.getOrders().size());
				this.addOrder(GlobalParameters.PROBLEM.getOrders().get(index) , 0);
				GlobalParameters.PROBLEM.getOrders().remove(index);
			}
		}
	}

	/**
	 * @return Set of orders that are registered in the simulator
	 */
	public Set<OrderAgentInitial> getOrders() {
		return unmodifiableSet(this.orderSet);
	}
	/**
	 * @param order from PROBLEM.getOrders(). The corresponding orderAgentInitial is created, and registered in simulator. 
	 * The order is also removed from PROBLEM.getOrders() list.
	 * @return true if agent successfully create and registered, else false
	 */
	private boolean addOrder(Order ord, long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		System.out.println(ord.toString());
		OrderAgentInitial or = new OrderAgentInitial(sim, prm.getRandomPosition(rng), ord);//<GlobalParameters.PROBLEM.getOrders().size()?i:i-1));
		if (orderSet.contains(or))
			return false;
		if (orderSet.add(or)) {
			boolean b = sim.register(or);
			if (b) System.out.println("order added and CurrentTime = " + currTime);
			return b;
		}
		return false;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {

	}
	/**
	 * the method is meant to generate customize orders w.r.t the basic input instance.
	 */
	private void addBasicOrders() {
		Iterator<Order> i = GlobalParameters.PROBLEM.getOrders().iterator();
		while(i.hasNext()) {
			Order ord = i.next();
			if (ord.getId().equals("43") || ord.getId().equals("4") ||
					ord.getId().equals("55")
					//ord.getId().equals("19")
					) {
				addOrder(ord, 0);
				i.remove();
			}
		}
	
	}
}
