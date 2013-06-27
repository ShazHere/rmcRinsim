/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.util.TimeWindow;
import shaz.rmc.core.domain.Delivery;

/**
 * 
 * The RmcDelivery are the split deliveries according to total quantity required by RmcOrder.
 * They should be created when they are meant to be physically on the pdp model.
 * 
 * @author Shaza
 * @date 20/11/2012
 * 
 */
public class DeliveryInitial extends Parcel {  //parcel has to be given start location at the time of creation.

	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	//protected Point startPosition;
	private final int deliveryNo;
	final private Delivery delivery;
	private final OrderAgentInitial order;
	
	public DeliveryInitial(OrderAgentInitial pOrder, Delivery pDelivery, int pDeliveryNo, Point pStation, 
			Point pOrderDestination, long pLoadingDuration, long pUnloadingDuration,
			double pVolume) {
		super(pStation, (long)pLoadingDuration, TimeWindow.ALWAYS, (long)pUnloadingDuration, TimeWindow.ALWAYS, pVolume);
		setStartPosition(pStation);
		setDestination(pOrderDestination); //do i require to set it??
		System.out.println("Delivery " + pDeliveryNo + " startLocation is " + pStation.toString());
		order = pOrder;
		delivery = pDelivery;
		deliveryNo = pDeliveryNo;
	}

	
	/**
	 * @param pStartPosition
	 * @return true if sgtart positon is Ste
	 */
//	public boolean setStartPositionNow(Point pStartPosition) {
//		setStartPosition(pStartPosition);
//		//this.startPosition = pStartPosition;
//		return true;
//	}
	
	@Override
	public void initRoadPDP(RoadModel pRoadModel, PDPModel pPdpModel) {
		roadModel = pRoadModel;
		pdpModel = pPdpModel;
		//roadModel.addObjectAt(this,roadModel.getRandomPosition(rng));	
		//System.out.println("Delivery added at randomPosition");
	}
	public OrderAgentInitial getOrder (){
		return this.order;
	}


	public Delivery getDelivery() {
		return delivery;
	}

}