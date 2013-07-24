/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

/**
 * @author Shaza
 * @date 23/07/2013
 * To represent plans of the orderAgent. One plan refers to the concept that a an ST_DELAY is fixed (to specific time) and then 
 * rest of the deliveries are tried to be scheduled accordingly. If all deliveries found, then all become 'ACCEPT' and then order
 * state = BOOKED. if any of the delivery is unable to be booked, after specific time, new OrderAgentPlan will be made with 
 * increased ST_DELAY.
 *  
 **/
public class OrderAgentPlan {
	

}
