/**
 * 
 */
package shaz.rmc.core;

/**
 * @author Shaza
 * @description For reply by Order to the proposal of Truck
 * @date 10/11/2011
 * 
 */


//probably if Truck proposes for first deliver, then Week_accept. If any truck proposes for d2++, then order should accept. 
//if already another truck is commited to the delivery then reject.
public enum Reply {
	NO_REPLY, //havn't got any reply yet
	ACCEPT,		//means full ok, for sure you can make the delivery 
	WEEK_ACCEPT,  //means partial ok, this should be reply if all the deliveries of Order are not manged yet
	REJECT, //No you cant pick it up because other agent has already committed.
	UNDER_PROCESS
}