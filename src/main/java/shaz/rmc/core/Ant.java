/**
 * 
 */
package shaz.rmc.core;

import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Message;
/**
 * @author Shaza
 * @description Abstract Class to be implemented by delegate MAs ants in RMC
 *
 */
public abstract class Ant extends Message {
	
	public Ant(CommunicationUser sender) {
		super(sender);
		// TODO Auto-generated constructor stub
	}
	
	public abstract Ant clone(CommunicationUser currentSender);
	//public boolean sendTo (Object o);
	
}