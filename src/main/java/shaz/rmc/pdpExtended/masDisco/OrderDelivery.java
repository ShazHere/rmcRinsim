/**
 * 
 */
package shaz.rmc.pdpExtended.masDisco;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.util.TimeWindow;
import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.trial.RmcOrder;

/**
 * @author Shaza
 *
 */
public class OrderDelivery extends Parcel {
	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	//protected Point startPosition;
	private final int deliveryNo;
	private final Delivery delivery;

	private final RmcOrder order;
	
	public OrderDelivery(RmcOrder pOrder, int pDeliveryNo, Delivery pDelivery) {
		super(null, 0, TimeWindow.ALWAYS, 0, TimeWindow.ALWAYS, 0);
		order = pOrder;	
		deliveryNo = pDeliveryNo;
		delivery = pDelivery;
	}
	public OrderDelivery(RmcOrder pOrder,  int pDeliveryNo, Point pStartPosition, Point pDestination, int pLoadingDuration, int pUnloadingDuration,
			Delivery pDelivery) {
		super(pDestination, pLoadingDuration, TimeWindow.ALWAYS, pUnloadingDuration, TimeWindow.ALWAYS, 0);
		setStartPosition(pStartPosition);
		System.out.println("Delivery " + pDeliveryNo + " startLocation is " + pStartPosition.toString());
		order = pOrder;
		 deliveryNo = pDeliveryNo;
		 delivery = pDelivery;
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
	public RmcOrder getOrder (){
		return this.order;
	}
	public int getDeliveryNo() {
		return deliveryNo;
	}
	public Delivery getDelivery() {
		return delivery;
	}

}
