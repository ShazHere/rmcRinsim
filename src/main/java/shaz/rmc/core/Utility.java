/**
 * 
 */
package shaz.rmc.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.rits.cloning.Cloner;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;
import shaz.rmc.pdpExtended.delMasInitial.ProductionSiteInitial;


/**
 * @author Shaza
 *
 */
public class Utility {
	
	/**
	 * @param pSchedule the schedule units between which slots need to be found
	 * @param availableSlots the reference of found arrayList of slots
	 * @param truckTotalTimeRange the range (boundries) between which slots should be found
	 * @return An array of available slots of more than GLOBALPARAMETERS.AVAILABLE_SLOT_SIZE found between the units of pSchedule. 
	 * Also param availableSlot contains the same returning arrayList 
	 */
	 public static ArrayList<AvailableSlot> getAvailableSlots(ArrayList<TruckScheduleUnit> pSchedule , ArrayList<AvailableSlot> availableSlots, final TimeSlot truckTotalTimeRange, long slotSizeInMinutes) {
		if (pSchedule.isEmpty()) {
			checkArgument(availableSlots.size() == 1 , true);
			availableSlots.get(0).setStartTime(new DateTime(truckTotalTimeRange.getStartTime())); //start time fixed according to currrent time
			return availableSlots;											//start location is already fixed according to startLocation of truck in method initRoadPDP()
		}
		else
		{
			sortSchedule(pSchedule);
			//checkArgument(availableSlots.size()>0, "T Unexpectedly Available slots empty and schedule size is " + pSchedule.size()); //there shud b one slot any way..
			if (availableSlots.isEmpty()) //means in some previous call, they were already empty
				return availableSlots; 
			AvailableSlot av = new AvailableSlot();
			//av.setLocationAtStartTime(availableSlots.get(0).getLocationAtStartTime(), availableSlots.get(0).getProductionSiteAtStartTime());
			availableSlots.clear();
			DateTime initialTime = truckTotalTimeRange.getStartTime();
			for (int i = 0; i< pSchedule.size(); i++) {
				if (initialTime.compareTo(pSchedule.get(i).getTimeSlot().getStartTime()) < 0) {
					Duration d = new Duration (initialTime, pSchedule.get(i).getTimeSlot().getStartTime());
					//if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {  //more than X hour slot
					if (d.compareTo(new Duration(slotSizeInMinutes*60l*1000l)) >= 0) {  //more than X hour slot
						av.setStartTime(initialTime);
						av.setEndtime(pSchedule.get(i).getTimeSlot().getStartTime()); //chk start time or end time are inclusive or not
//						if (i!=0) //if previous slot exists, then truck should start from the return station of previous slot
//							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
						if (i!=0 && pSchedule.get(i-1) instanceof TruckDeliveryUnit) {
							TruckDeliveryUnit tdu = (TruckDeliveryUnit)pSchedule.get(i-1); 
							av.setLastOrderVisited(tdu.getDelivery().getOrder());
						}
						if (i< pSchedule.size()  && pSchedule.get(i) instanceof TruckDeliveryUnit) {
							TruckDeliveryUnit tdu = (TruckDeliveryUnit)pSchedule.get(i);
							av.setpS4NextOrderVisited((ProductionSiteInitial)tdu.getDelivery().getLoadingStation());
						}
						availableSlots.add(av);
						av = new AvailableSlot();
////						if (i > 0) 
////							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
//						//System.out.println("added slot = " + av.toString());
//						//initialTime = u.getTimeSlot().getEndTime();
					}
				}
				initialTime = pSchedule.get(i).getTimeSlot().getEndTime().plusMinutes(1); // so there should be difference of one minute between slots
			}
			//after last unit, to check if after the last unit of schedule, there could be available time?
			//initialTime = pSchedule.get(pSchedule.size()-1).getTimeSlot().getEndTime();
			if (initialTime.compareTo(truckTotalTimeRange.getEndTime()) < 0) {
				Duration d = new Duration (initialTime, truckTotalTimeRange.getEndTime());
				if (d.compareTo(new Duration(slotSizeInMinutes*60l*1000l)) >= 0) {
//					if (!pSchedule.isEmpty()) { //if previous slot exists, then truck should start from the return station of previous slot
//						av.setLocationAtStartTime(pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation());
//					}
					if (!pSchedule.isEmpty()) {
						TruckDeliveryUnit tdu = (TruckDeliveryUnit)pSchedule.get(pSchedule.size()-1); 
						av.setLastOrderVisited(tdu.getDelivery().getOrder());			
					}
					av.setStartTime(initialTime);
					av.setEndtime(truckTotalTimeRange.getEndTime()); //chk start time or end time are inclusive or not
					availableSlots.add(av);
				}
			}
			if (availableSlots.isEmpty()) {
				return availableSlots;
			}
			return availableSlots;
		}
	}
	/**
	 * @param pSchedule
	 */
	public static void sortSchedule(ArrayList<TruckScheduleUnit> pSchedule) {
		if (pSchedule.size() <= 1)
			return;
		Collections.sort(pSchedule, new Comparator<TruckScheduleUnit>(){
		    public int compare( TruckScheduleUnit a, TruckScheduleUnit b ){
		        return (int)((a.getTimeSlot().getStartTime().getMillis()/1000)- (b.getTimeSlot().getStartTime().getMillis()/1000));
		    }
		});
	} 
	public static TimeSlot getTravelUnitTimeSlot(Duration travelDuration, DateTime referenceTime, boolean isbeforeReferenceTime) {
		if (isbeforeReferenceTime)
			return new TimeSlot(referenceTime.minus(travelDuration).minusMinutes(1), referenceTime.minusMinutes(1));
		else
			return new TimeSlot(referenceTime.plusMinutes(1), referenceTime.plus(travelDuration).plusMinutes(1));
	}
	
	/**
	 * Just to avoid that AvailableSlot is 1 if truck schedule is empty.
	 * @param currTime 
	 */
	public static void adjustAvailableSlotInBeginning(DateTime currTime, ArrayList<AvailableSlot> availableSlots) {
		availableSlots.clear();
		availableSlots.add(new AvailableSlot(new TimeSlot(currTime, GlobalParameters.END_DATETIME), null, null));
	}
	public static boolean wrtieInFile( boolean isAppend, ResultElements resultElement) {
		// PrintWriter out; //not using it any more because it swallows any exceptions and  
		
		//fileName =  fileName + GlobalParameters.INPUT_INSTANCE_TYPE.toString();
		String filePostfix = getFilePostFix();
			
		String fileName = GlobalParameters.RESULT_FOLDER + GlobalParameters.getFileNamePrefix() + filePostfix; //+ "-" +GlobalParameters.ENABLE_TRUCK_BREAKDOWN + "-" +GlobalParameters.ENABLE_JI;
		String fileNameHourConcrete = GlobalParameters.RESULT_FOLDER + GlobalParameters.getFileNamePrefix() + "-HoursConcrete" + filePostfix;
		String fileNameWastedConcrete = GlobalParameters.RESULT_FOLDER + GlobalParameters.getFileNamePrefix() + "-WastedConcrete" + filePostfix;
		String columnSeperator = "\t"; 
		String commentText = "# Data file for input file: " + GlobalParameters.INPUT_FILE + "\n" +
							"# Experiment Date: " + new DateTime(System.currentTimeMillis()) + "\n";
		createDirectory();
			boolean addComment = checkAlreadyExist(fileName);
			
			String columnName = resultElement.getColumnNames(columnSeperator)+ columnSeperator + GlobalParameters.INPUT_INSTANCE_TYPE.toString() +"\n";
			String writeText = resultElement.getResultDetailsInColumnForm(columnSeperator)+ columnSeperator + GlobalParameters.INPUT_INSTANCE_TYPE.toString() +"\r";			
			writeTextToFile (isAppend, addComment, commentText, fileName, columnName, writeText );
			
			//for hours concrete file
			columnName = resultElement.getColumnNamesHoursConcrete(columnSeperator) + "\n";
			writeText = resultElement.getResultDetailsHourConcrteInColumnForm(columnSeperator);
			writeTextToFile (isAppend, addComment, commentText, fileNameHourConcrete, columnName, writeText );
			
			//for wastedConcrete file
			columnName = resultElement.getColumnNamesWastedConcrete(columnSeperator) + "\n";
			writeText = resultElement.getResultDetailsWastedConcrteInColumnForm(columnSeperator);
			writeTextToFile (isAppend, addComment, commentText, fileNameWastedConcrete, columnName, writeText );

	
		return false;
	}
	/**
	 * @return
	 */
	private static String getFilePostFix() {
		String filePostfix = "";
		if (GlobalParameters.ENABLE_TRUCK_BREAKDOWN)
			filePostfix = filePostfix + "_B";
		if (GlobalParameters.ENABLE_JI)
			filePostfix = filePostfix + "_JI";
		filePostfix = filePostfix + ".txt";
		return filePostfix;
	} 
	
	private static boolean checkAlreadyExist(String fileName) {
		boolean addComment = false;
		try {
			
			//first check if file already contains anything
			FileReader fileReader = 
                new FileReader(fileName);
			
//			FileReader fileReaderHourConcrete = 
//	                new FileReader(fileNameHourConcrete);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
//            BufferedReader bufferedReaderhourConcrete = 
//                    new BufferedReader(fileReaderHourConcrete);
			String line;

            if (fileReader == null || bufferedReader == null) { //probably  it should be enough for boht files..
            	addComment = true;
	              System.out.println("File or buffere reader Null!..Comment should be added!!");
            }
            else {
	            while((line = bufferedReader.readLine()) == null) {
	                addComment = true;
	                System.out.println("Comment should be added!!");
	                break;
	            }
	            bufferedReader.close();
            }
			}
			catch (IOException exx) {
				   addComment = true;
	                System.out.println("Caught exception..Comment should be added!!");
			}
		return addComment;
	} 

	private static void writeTextToFile(boolean isAppend, boolean addComment, String commentText, String fileName, String columnNames, String writeText) {
		try {
			
		FileWriter fileWriter =
                new FileWriter(fileName,isAppend);

            // Always wrap FileWriter in BufferedWriter.
            BufferedWriter bufferedWriter =
                new BufferedWriter(fileWriter);

            // Note that write() does not automatically
            // append a newline character.
            if (addComment) {
            	
            	bufferedWriter.write(commentText);
            	bufferedWriter.write( columnNames);
            	
            }
            bufferedWriter.write(writeText);
        
            bufferedWriter.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static Cloner getCloner() {
		final Cloner cl = new Cloner();
		cl.dontCloneInstanceOf(Agent.class);
		cl.dontCloneInstanceOf(ProductionSite.class);
		cl.dontCloneInstanceOf(DeliveryTruckInitial.class);
		cl.dontCloneInstanceOf(OrderAgentInitial.class);
		cl.registerImmutable(DateTime.class);
		cl.registerImmutable(Delivery.class);
		//cl.setDumpClonedClasses(true);
		return cl;
	}
	private static void createDirectory() {
		String dirName = GlobalParameters.RESULT_FOLDER + GlobalParameters.SCALE;
		 File theDir = new File(dirName);

		  // if the directory does not exist, create it
		  if (!theDir.exists()) {
		    System.out.println("creating directory: " + dirName);
		    boolean result = theDir.mkdir();  

		     if(result) {    
		       System.out.println("DIR created");  
		     }
		  }		
	}
	
//	/**
//	 * @param pSchedule the schedule units between which slots need to be found
//	 * @param availableSlots the reference of found arrayList of slots
//	 * @param truckTotalTimeRange the range (boundries) between which slots should be found
//	 * @return An array of available slots of more than GLOBALPARAMETERS.AVAILABLE_SLOT_SIZE found between the units of pSchedule. 
//	 * Also param availableSlot contains the same returning arrayList 
//	 */
//	public static ArrayList<TimeSlot> getAvailableSlots(ArrayList<TruckScheduleUnit> pSchedule , ArrayList<TimeSlot> availableSlots, final TimeSlot truckTotalTimeRange) {
//		if (pSchedule.isEmpty()) {
//			checkArgument(availableSlots.size() == 1 , true);
//			availableSlots.get(0).setStartTime(new DateTime(truckTotalTimeRange.getStartTime())); //start time fixed according to currrent time
//			return availableSlots;											//start location is already fixed according to startLocation of truck in method initRoadPDP()
//		}
//		else
//		{
//			sortSchedule(pSchedule);
//			//checkArgument(availableSlots.size()>0, "T Unexpectedly Available slots empty and schedule size is " + pSchedule.size()); //there shud b one slot any way..
//			if (availableSlots.isEmpty()) //means in some previous call, they were already empty
//				return availableSlots; 
//			TimeSlot av = new TimeSlot();
//			//av.setLocationAtStartTime(availableSlots.get(0).getLocationAtStartTime(), availableSlots.get(0).getProductionSiteAtStartTime());
//			availableSlots.clear();
//			DateTime initialTime = truckTotalTimeRange.getStartTime();
//			for (int i = 0; i< pSchedule.size(); i++) {
//				if (initialTime.compareTo(pSchedule.get(i).getTimeSlot().getStartTime()) < 0) {
//					Duration d = new Duration (initialTime, pSchedule.get(i).getTimeSlot().getStartTime());
//					if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {  //more than 2 hour slot
//						av.setStartTime(initialTime);
//						av.setEndtime(pSchedule.get(i).getTimeSlot().getStartTime()); //chk start time or end time are inclusive or not
////						if (i!=0) //if previous slot exists, then truck should start from the return station of previous slot
////							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
//						availableSlots.add(av);
//						av = new TimeSlot();
//////						if (i > 0) 
//////							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
////						//System.out.println("added slot = " + av.toString());
////						//initialTime = u.getTimeSlot().getEndTime();
//					}
//				}
//				initialTime = pSchedule.get(i).getTimeSlot().getEndTime().plusMinutes(1); // so there should be differenc of one minute between slots
//			}
//			//after last unit, to check if after the last unit of schedule, there could be available time?
//			//initialTime = pSchedule.get(pSchedule.size()-1).getTimeSlot().getEndTime();
//			if (initialTime.compareTo(truckTotalTimeRange.getEndTime()) < 0) {
//				Duration d = new Duration (initialTime, truckTotalTimeRange.getEndTime());
//				if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {
////					if (!pSchedule.isEmpty()) { //if previous slot exists, then truck should start from the return station of previous slot
////						av.setLocationAtStartTime(pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation());
////					}
//					av.setStartTime(initialTime);
//					av.setEndtime(truckTotalTimeRange.getEndTime()); //chk start time or end time are inclusive or not
//					availableSlots.add(av);
//				}
//			}
//			if (availableSlots.isEmpty()) {
//				return availableSlots;
//			}
//			return availableSlots;
//		}
//	}
}
