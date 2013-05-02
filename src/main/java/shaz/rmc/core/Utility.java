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
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;

import shaz.rmc.core.domain.Delivery;
import shaz.rmc.pdpExtended.delMasInitial.DeliveryTruckInitial;
import shaz.rmc.pdpExtended.delMasInitial.GlobalParameters;
import shaz.rmc.pdpExtended.delMasInitial.OrderAgentInitial;


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
	public static ArrayList<TimeSlot> getAvailableSlots(ArrayList<TruckScheduleUnit> pSchedule , ArrayList<TimeSlot> availableSlots, final TimeSlot truckTotalTimeRange) {
		if (pSchedule.isEmpty()) {
			checkArgument(availableSlots.size() == 1 , true);
			availableSlots.get(0).setStartTime(new DateTime(truckTotalTimeRange.getStartTime())); //start time fixed according to currrent time
			return availableSlots;											//start location is already fixed according to startLocation of truck in method initRoadPDP()
		}
		else
		{
			Collections.sort(pSchedule, new Comparator<TruckScheduleUnit>(){
		        public int compare( TruckScheduleUnit a, TruckScheduleUnit b ){
		            return (int)((a.getTimeSlot().getStartTime().getMillis()/1000)- (b.getTimeSlot().getStartTime().getMillis()/1000));
		        }
			});
			//checkArgument(availableSlots.size()>0, "T Unexpectedly Available slots empty and schedule size is " + pSchedule.size()); //there shud b one slot any way..
			if (availableSlots.isEmpty()) //means in some previous call, they were already empty
				return availableSlots; 
			TimeSlot av = new TimeSlot();
			av.setLocationAtStartTime(availableSlots.get(0).getLocationAtStartTime(), availableSlots.get(0).getProductionSiteAtStartTime());
			availableSlots.clear();
			DateTime initialTime = truckTotalTimeRange.getStartTime();
			for (int i = 0; i< pSchedule.size(); i++) {
				if (initialTime.compareTo(pSchedule.get(i).getTimeSlot().getStartTime()) < 0) {
					Duration d = new Duration (initialTime, pSchedule.get(i).getTimeSlot().getStartTime());
					if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {  //more than 2 hour slot
						av.setStartTime(initialTime);
						av.setEndtime(pSchedule.get(i).getTimeSlot().getStartTime()); //chk start time or end time are inclusive or not
						if (i!=0) //if previous slot exists, then truck should start from the return station of previous slot
							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
						availableSlots.add(av);
						av = new TimeSlot();
//						if (i > 0) 
//							av.setLocationAtStartTime(pSchedule.get(i-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(i-1).getDelivery().getReturnStation());
						//System.out.println("added slot = " + av.toString());
						//initialTime = u.getTimeSlot().getEndTime();
					}
				}
				initialTime = pSchedule.get(i).getTimeSlot().getEndTime().plusMinutes(1); // so there should be differenc of one minute between slots
			}
			//after last unit, to check if after the last unit of schedule, there could be available time?
			//initialTime = pSchedule.get(pSchedule.size()-1).getTimeSlot().getEndTime();
			if (initialTime.compareTo(truckTotalTimeRange.getEndTime()) < 0) {
				Duration d = new Duration (initialTime, truckTotalTimeRange.getEndTime());
				if (d.compareTo(new Duration(GlobalParameters.AVAILABLE_SLOT_SIZE_HOURS*60l*60l*1000l)) >= 0) {
					if (!pSchedule.isEmpty()) { //if previous slot exists, then truck should start from the return station of previous slot
						av.setLocationAtStartTime(pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation().getLocation(), pSchedule.get(pSchedule.size()-1).getDelivery().getReturnStation());
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
	public static boolean wrtieInFile(String fileName, boolean isAppend, String text, ResultElements resultElement) {
		// PrintWriter out; //not using it any more because it swallows any exceptions and  
		boolean addComment = false;
		//fileName =  fileName + GlobalParameters.INPUT_INSTANCE_TYPE.toString();
		fileName = GlobalParameters.RESULT_FOLDER + fileName + "-" +GlobalParameters.ENABLE_TRUCK_BREAKDOWN + "-" +GlobalParameters.ENABLE_JI;
		String columnSeperator = "\t"; 
		String commentText = "# Data file for input file: " + GlobalParameters.DATA_FOLDER + GlobalParameters.INPUT_FILE + "\n" +
							"# Experiment Date: " + new DateTime(System.currentTimeMillis()) + "\n";
		try {
			
			try {
			//first check if file already contains anything
			String line;
			FileReader fileReader = 
                new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = 
                new BufferedReader(fileReader);
            
            if (fileReader == null || bufferedReader == null) {
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
			 
			//prepare for WRITING in file
			FileWriter fileWriter =
	                new FileWriter(fileName,isAppend);

	            // Always wrap FileWriter in BufferedWriter.
	            BufferedWriter bufferedWriter =
	                new BufferedWriter(fileWriter);

	            // Note that write() does not automatically
	            // append a newline character.
	            if (addComment) {
	            	
	            	bufferedWriter.write(commentText);
	            	bufferedWriter.write( resultElement.getColumnNames(columnSeperator)+ columnSeperator + GlobalParameters.INPUT_INSTANCE_TYPE.toString() +"\n");
	            }
	            bufferedWriter.write(resultElement.getResultDetailsInColumnForm(columnSeperator)+ columnSeperator + GlobalParameters.INPUT_INSTANCE_TYPE.toString() +"\r");
	            //bufferedWriter.write("\n");
	            
	            // Always close files.
	            bufferedWriter.close();
			//System.out.printf("Testing String");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
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
}
