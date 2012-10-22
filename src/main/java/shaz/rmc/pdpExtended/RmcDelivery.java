package shaz.rmc.pdpExtended;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPModel;
import rinde.sim.core.model.pdp.Parcel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.util.TimeWindow;

/**
 * 
 * The RmcDelivery are the split deliveries according to total quantity required by RmcOrder.
 * They should be created when they are meant to be physically on the pdp model.
 * 
 * @author Shaza
 * @date 17/10/2012
 */
public class RmcDelivery extends Parcel {

	protected RoadModel roadModel;
	protected PDPModel pdpModel;
	//protected Point startPosition;
	final protected int deliveryNo;

	private final RmcOrder order;
	
	public RmcDelivery(RmcOrder pOrder, int pDeliveryNo, double pVolume) {
		super(null, 0, TimeWindow.ALWAYS, 0, TimeWindow.ALWAYS, pVolume);
		order = pOrder;	
		deliveryNo = pDeliveryNo;
	}
	public RmcDelivery(RmcOrder pOrder,  int pDeliveryNo, Point pStartPosition, Point pDestination, int pLoadingDuration, int pUnloadingDuration,
			double pVolume) {
		super(pDestination, pLoadingDuration, TimeWindow.ALWAYS, pUnloadingDuration, TimeWindow.ALWAYS, pVolume);
		setStartPosition(pStartPosition);
		System.out.println("Delivery " + pDeliveryNo + " startLocation is " + pStartPosition.toString());
		order = pOrder;
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

}