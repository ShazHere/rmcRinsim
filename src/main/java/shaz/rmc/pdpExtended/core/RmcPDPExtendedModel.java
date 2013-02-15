package shaz.rmc.pdpExtended.core;

import rinde.sim.core.model.pdp.PDPModel;
//import rinde.sim.core.model.pdp.Parcel;
//import rinde.sim.core.model.pdp.Vehicle;
//import shaz.rmc.pdpExtended.trial.RmcDelivery;
//import shaz.rmc.pdpExtended.trial.RmcDeliveryTruck;
//import shaz.rmc.pdpExtended.trial.RmcProductionSite;
//import shaz.rmc.pdpExtended.trial.RmcStrictTimeWindowPolicy;



/**
 * For maintaining the physical aspect of RMC problem. Used most of the features from PDP model.
 * 
 *  * Currently supports three kinds of objects:
 * <ul>
 * <li> {@link RmcDelivery}</li>
 * <li> {@link RmcDeliveryTruck}</li>
 * <li> {@link RmcProductionSite}</li>
 * </ul>
 * 
 * 
 * 
 * @author Shaza
 * @date 17/10/2012
 *
 */
public class RmcPDPExtendedModel extends PDPModel {
	/**
	 * Problem specific meanings for enum PDPModel.ParcelState
	 * 
	 *   * State that indicates that the {@link Parcel} is not yet available for
         * pickup but that it will be in the (near) future.
         * Modified: remains same 
        * ANNOUNCED,
        /**
         * State that indicates that the {@link Parcel} is available for pickup.
         * Modified: When delivery is available for pickup, i.e its timewindow should have started  
        AVAILABLE,
        /**
         * State that indicates that the {@link Parcel} is in the process of
         * being picked up.
         * Modified: When the delivery is loaded
        PICKING_UP,
        /**
         * State that indicates that the {@link Parcel} is currently in the
         * cargo of a {@link Vehicle}.
         * Modified: Delivery is already loaded in the {@link RmcDeliveryTruck}
        IN_CARGO,
        /**
         * State that indicates that the {@link Parcel} is in the process of
         * being delivered.
         * Modified: Unloading at the Order Site
        DELIVERING,
        /**
         * State that indicates that the {@link Parcel} has been delivered.
         * Modified: Delivery Delivered and {@linkRmcDeliveyTruck} should be free now
        DELIVERED
	 */
	
	
	/**
	 * @param twp The {@link RmcStrictTimeWindowPolicy} used for this model
	 */
	RmcPDPExtendedModel() {
		super();		
	}
	
	
}
