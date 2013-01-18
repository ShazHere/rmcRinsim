/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
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
	Map<Integer, Boolean> addAbleOrderIndexes;
	
	public OrderManagerInitial(Simulator pSim, int randomSeed, PlaneRoadModel pPrm) {
		rng = new MersenneTwister(randomSeed); 
		sim = pSim;
		prm = pPrm;
		lastTimeOrdersChecked = new DateTime(0);
		logger = Logger.getLogger(OrderManagerInitial.class);
		addAbleOrderIndexes = new LinkedHashMap<Integer, Boolean>();
	}
	@Override
	public void tick(TimeLapse timeLapse) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)timeLapse.getStartTime());
		if (currTime.minusMinutes(lastTimeOrdersChecked.getMinuteOfDay()).getMinuteOfDay() >= GlobalParameters.FEASIBILITY_INTERVAL_MIN ){
			if (addAbleOrderIndexes.containsValue(false)) {
				try {
					throw new Exception("ORDER should have been added!");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Iterator<Order> i = GlobalParameters.PROBLEM.getOrders().iterator();
			addAbleOrderIndexes = new LinkedHashMap<Integer, Boolean>();
			while(i.hasNext()) {
				Order ord = i.next();
				if (currTime.plusHours(2).compareTo(ord.getEarliestStartTime()) >= 0)  { //Xhours earlier the from their start times, orders will be added
					addAbleOrderIndexes.put(GlobalParameters.PROBLEM.getOrders().indexOf(ord), false);
					
				}
			}
			int offset = 0; //since the index will change after removal of an order from PROBLEM.getOrder()
			for (Integer it : addAbleOrderIndexes.keySet()) {
				if (addOrder(it+offset, timeLapse.getStartTime()))
					offset -= 1;
			}
			//remove orders
//			for (Integer it : addAbleOrderIndexes.keySet()) {
//				
//			}
			lastTimeOrdersChecked = currTime;
		}
	}

	
	/**
	 * Should be called after registering orderManagerInitial.If the method is not called at all, no orders will be added, except if they are handled in tick() method
	 */
	public void addOrders() {
		//addBasicOrders();
		int orderToBeAdded = GlobalParameters.INPUT_INSTANCE_TYPE.getPerOfX(GlobalParameters.PROBLEM.getOrders().size());
		logger.debug("Orders to be added at Start is=  " + orderToBeAdded);
//		if (orderToBeAdded == 0) { //so all is dynamic, add the orders whose time comes befor
//			Iterator<Order> i = GlobalParameters.PROBLEM.getOrders().iterator();
//			while(i.hasNext()) { //all orders should show up X+1 hours before their start time, +1 since 
//				//sim start time is almost 1 hour before earliest start tiem of earliest order
//				Order ord = i.next();
//				if (ord.getEarliestStartTime().compareTo(GlobalParameters.START_DATETIME.plusHours(3)) <= 0) {
//					this.addOrder(GlobalParameters.PROBLEM.getOrders().indexOf(ord));
//				}
//			}
//		}
		for (int k = 0; k < orderToBeAdded; k++) {
			int index = this.rng.nextInt(GlobalParameters.PROBLEM.getOrders().size());
			this.addOrder(index , 0);
		}
	}

	/**
	 * @return Set of orders that are registered in the simulator
	 */
	public Set<OrderAgentInitial> getOrders() {
		return unmodifiableSet(this.orderSet);
	}
	/**
	 * @param i index of Order w.r.t PROBLEM.getOrders(). The corresponding orderAgentInitial is created, and registered in simulator. 
	 * The order is also removed from PROBLEM.getOrders() list.
	 * @return
	 */
	private boolean addOrder(int i, long startTime) {
		DateTime currTime = GlobalParameters.START_DATETIME.plusMillis((int)startTime);
		System.out.println(GlobalParameters.PROBLEM.getOrders().get(i).toString());
		OrderAgentInitial or = new OrderAgentInitial(sim, prm.getRandomPosition(rng), GlobalParameters.PROBLEM.getOrders().get(i));//<GlobalParameters.PROBLEM.getOrders().size()?i:i-1));
		System.out.println("index = " + i + ", order is = " + or.getOrder().getId());
		if (orderSet.add(or)) {
			sim.register(or);
			System.out.println("order added and CurrentTime = " + currTime);
			GlobalParameters.PROBLEM.getOrders().remove(i);
			addAbleOrderIndexes.put(i, true); //means agent created and order removed
			return true;
		}
		return false;
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub  

	}
	/**
	 * the method is meant to generate customize orders fromt he basic input instance.
	 */
	private void addBasicOrders() {
		int  k =0, l =0, m =0, n =0 ; //4, 43, 19, 55
		for (int i = 0; i< GlobalParameters.PROBLEM.getOrders().size(); i++){
			if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("43")) {
				k = i;
			}
			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("4")) {
				l = i;
			}
			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("19"))
				m = i;
			else if (GlobalParameters.PROBLEM.getOrders().get(i).getId().equals("55")) {				
				n = i;		
			}
		}
		System.out.println("k= " +k+", l = " +l+ ", m = "+m+ ", n =" + n);
		
		this.addOrder(k);
		this.addOrder(l);
		this.addOrder(m);
		this.addOrder(n);
	
	}
}
