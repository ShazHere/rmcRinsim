/**
 * 
 */
package trial.rinsim.rmc.basic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;

/**
 * @author Shaza
 * TickListener, MovingRoadUser,CommunicationUser, SimulatorUser 
 */
public class RmcDeliveryTruck implements CommunicationUser, TickListener, MovingRoadUser {//, SimulatorUser{

	protected RoadModel roadModel;
	protected final RandomGenerator rnd;
	protected Point startLocation;
	protected Point destination; 
//	private SimulatorAPI simulator;
	protected int radius;
	private final Mailbox mailbox;
	private CommunicationAPI cApi;
	int id;
	static int totalNum;
	
	RmcDeliveryTruck(Point pStartLocation, int radius) {
		rnd = new MersenneTwister();
		startLocation = pStartLocation;
		totalNum = totalNum+1;
		id = totalNum;
		destination = null;
		this.radius = radius;
		mailbox = new Mailbox();
	}
	
	@Override
	public void tick(TimeLapse timeLapse) {
		checkMsgs(timeLapse.getTime());
		//refreshList(timeLapse.getTime());
		//roadModel.moveTo(this, roadModel.getRandomPosition(rnd), timeLapse);
		if (destination == null && roadModel.getPosition(this).equals(startLocation)) {
			Set <RmcOrder> ro = roadModel.getObjectsOfType(RmcOrder.class);  
			int i = this.id % ro.size();
			int j = 0;
			RmcOrder order = null;
		    for (RmcOrder ord: ro) {
		    	if (j == i){
		    		order = ord;
		    		break;
		    	}
		    	j++;
		    }  
		   if (this.id == 1){
		    	sendMsgs(timeLapse.getStartTime());
		    }
		    destination = roadModel.getPosition(order);
		}
		else if (roadModel.getPosition(this).equals(destination)) {
				System.out.println("Destination Reached");
				if (destination.equals(startLocation))
					destination = null;
				else
					destination = startLocation;
		}
		else if (destination != null)
			roadModel.moveTo(this, destination, timeLapse);
	}

	public void initRoadUser(RoadModel model) {
		//we receive model and add ourselves at a random position
		roadModel = model;
		roadModel.addObjectAt(this, startLocation);
		
	}

	@Override
	public void afterTick(TimeLapse timeLapse) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getSpeed() {
		return .03;
		//return 400;
	}
	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cApi = api;
	}
	@Override
	public Point getPosition() {
		return roadModel.getPosition(this);
	}
	@Override
	public double getRadius() {
		return this.radius;
	}
	@Override
	public double getReliability() {
		// TODO Auto-generated method stub
		return 1;
	}
	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}
	private void sendMsgs(long currentTime) {
		//if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
			//lastCommunication = currentTime;
			if (cApi != null) {
				cApi.broadcast(new Message(this) {});
				System.out.println(this.id + "T message broad caseted");
			}
//		}
	}

	private void checkMsgs(long currentTime) {
		Queue<Message> messages = mailbox.getMessages();
		if (messages.size() > 0)
			System.out.println(this.id+"T received messages quantity = " + messages.size());
//		for (Message m : messages) {
//			lastCommunicationTime.put((RandomWalkAgent) m.getSender(), currentTime);
//			communications++;
//		}
}

//	@Override
//	public void setSimulator(SimulatorAPI api) {
//		simulator = api;
//		//rnd = api.getRandomGenerator();		
//	}

	

}
