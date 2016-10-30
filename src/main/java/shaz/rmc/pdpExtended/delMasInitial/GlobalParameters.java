/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

//import java.util.GregorianCalendar;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;

import org.joda.time.DateTime;
import org.joda.time.chrono.GregorianChronology;

//import rmc1.environment.OrderManager;
import shaz.rmc.core.domain.Problem;


/** 
 * @author Shaza
 * @description For adjusting different kinds of variables/knobs at a single point!
 * @date 14/09/2011
 * 
 */
public final class GlobalParameters {

	
	/**
	 * @param pScale
	 * @param pStress
	 * @param pDataFolder
	 * @param pResultFolder
	 * @param pFileName
	 * At the beining of program, these should be given as parameters. So that correct files are loaded. 
	 */
	public GlobalParameters(int pScale, String pStress, String pDataFolder, String pResultFolder, String pFileName){
				 this.SCALE = pScale;
				 this.STRESS = pStress;
				 this.DATA_FOLDER = pDataFolder;
				 this.RESULT_FOLDER = pResultFolder;
				 this.INPUT_FILE  = pFileName; //+ STRESS +"_" + SCALE + "_95" + ".xml";
			}	
	public static Problem PROBLEM = null;	//the problem loader
	//public static boolean ENABLE_GUI = true;
	
	//public static final int SIMULATION_SPEED = 200; //specifies what should be execution speed of simulator.
	//public static final int TICK_INTERVAL = 200000; 

	public static final int DEPHASE_INTERVAL_SEC = 6*60; //means ants shud be de-phased randomly between 0 to X Sec
	public static final int FEASIBILITY_INTERVAL_SEC = 90; //earlier 8 means send feeasibilty ants to near by after every X minutes.
	public static final int FEASIBILITY_EVAPORATION_INTERVAL_SEC = FEASIBILITY_INTERVAL_SEC + (60); //earlier + 5
	public static final int EXPLORATION_INTERVAL_SEC = 60; //means no. of mintues //earlier 2
	public static final int INTENTION_INTERVAL_SEC = 90; //means no. of mintues earlier = 5
	public static final int INTENTION_EVAPORATION_SEC = INTENTION_INTERVAL_SEC + (60); //earlier 3
	
	public static final int EXPLORATION_SCHEDULE_SIZE = 1; //earlier 3 (5/02/2013)
	//public static final int TOTAL_TRUCKS = PROBLEM.getVehicles().size() ; //almost 29 trucks are there in the instances
	public static final boolean IS_FIXED_VEHICLE_CAPACITY = true; //will all the vehicles have same capacity
	public static final int FIXED_CAPACITY = 10000; //in m3, but not used except for estimation of maximum possible start time 30/07/2013
	public static final double TRUCK_SPEED = 10; //points per hour, in a 10*10 square area. at the moment i consider it 100km2 area. 
	//public static final int TRUCKS_PLAN_IN_ADVANCE_HOURS = 6; //specifies how much time in advance the truck can plan. but if deliveries are so many, the truck
	//accepts the deliveyr time of even after TRUCKS_PLAN_IN_ADVANCE_HOURS. 
	public static final int MINUTES_TO_PERISH_CONCRETE = 80; //in minutes..
	public static final int MAX_LAG_TIME_MINUTES = 30; //used when LAG_TIME_ENABLE = true
	public static final boolean LAG_TIME_ENABLE = true;
	public static final int MINUTES_TO_CHANGE_ST4ORDER = 60; // When shold start time of the order be delayed..
	public static final int MINUTES_TO_DELAY_ST = 30; //means ST should be delayed with MINUTES_TO_DELAY_ST if MINUTES_TO_CHANGE_ST4ORDER time has been elapsed
							//after each change of ST. I should make it in such a way that if this is 0, then actually there is no ST_DELAY
	public static final int MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED = 70;
	public static final int MINUTES_BEFOR_DELIVERY_CREATED = 5; //mintues before pickup time, when delivery(physically) should be created. 
	public static final int DISCHARGE_RATE_PERHOUR = 10000; //Constant for all orders
	public static final int LOADING_MINUTES = 5;
	public static final int MAX_NO_OF_EXPLORATION_FOR_ORDER = 5; //no. of exploration ants, that can visit order while exploring
	public static final long AVAILABLE_SLOT_SIZE_HOURS = 1; //used to find slots that is available, after which exp ants checks
	public static final boolean ENABLE_TRUCK_BREAKDOWN = true;
	public static final boolean ENABLE_JI = false; //would JI features be used
	
	public static final boolean EXP_RANKING_WITH_SCORE_ENABLE = true; //should exploration ants be ranked accoridng their score by Truck agent
	public static final boolean EXP_BEST_PRIORATIZE_NOT_SERVING_ORDERS = true;
	public static final boolean EXP_BEST_DE_PRIORITIZE_DEL0 = true;
	public static final int EXPLORATION_ANT_SEARCH_FOR_SEC_AFTER =0;// INTENTION_EVAPORATION_SEC * 4; //EXP ants donot start searching for truckSchedule for currTime,
	public static final boolean PS_ALWAYS_AVAILABLE = true;
	//breakdown probability between 0 to 25 percent.  
	public static final int TRUCK_BREAKDOWN_PROBABILTY = 80;   
		
	//CHECK NOTES ABOUT INPUT FILES!!
	//public static final String INPUT_FILE = "canonical-30May2013.xml";  //with expanded order start times
	//public static final String INPUT_FILE = "planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic-bigger2.xml";  //with expanded order start times
//	public static final String INPUT_FILE = "planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic.xml";  //compaq deliveries, the normal basic set
//	public static final String INPUT_FILE = "planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic-4Delivery.xml";
	//public static final String INPUT_FILE =  "planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic-expanded.xml";
	//public static final String INPUT_FILE =  "planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic-1Delivery.xml";
	//public static final String INPUT_FILE = "planning2011-01-20-update-ucy-sameTruck1.xml";  //total trucks = 27
//	public static final String INPUT_FILE = "planning2011-01-14-update-ucy.xml";
	//public static final String INPUT_FILE = "planning2011-02-15-update-ucy.xml";
	//public static final String INPUT_FILE = "planning2011-01-11/planning2011-01-11-update-ucy-shaz-requiredRateFixed.xml";
	//public static final String INPUT_FILE = "planning2011-01-20-update-ucy-sameTruck1.xml";  //total trucks = 28
//	public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/planning2011-01-10/"; //planning2011-01-20/";canonicalFrom
	//public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/canonicalFrom/";
	//public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/planning2011-01-10-bigger2/"; 
	//public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/planning2011-01-10-1Delivery/"; 
	//public static final String RESULT_FOLDER = "/Users/Shaza/Documents/workspace/shaza-rmc.core/JIExpMay2013/";
	//public static final String RESULT_FOLDER = "/Users/Shaza/Documents/workspace/shaza-rmc.core/30August/";
	//public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/planning2011-01-20/";
	//public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/planning2011-02-15/";
	public static final String LOG_LOCATION = "hh";  //not used 
	
	public static int SCALE = 4;
		public static  String STRESS  = "1.0";
		public static  String DATA_FOLDER= "/home/shaza/RMCData_2016/input/";
		//public static final String RESULT_FOLDER = "/Users/Shaza/Documents/workspace/shaza-rmc.core/GeneratroFilesRestult14Sept2013/";
		public static  String RESULT_FOLDER = "/home/shaza/RMCData_2016/output/";
		public static  String INPUT_FILE = DATA_FOLDER + SCALE + "/" + STRESS +"_" + SCALE + "_839.xml";
		public static PERCENT INPUT_INSTANCE_TYPE = PERCENT.per100;
		
	/**
	 * Simulation Start time, w.r.t real time clock
	 */
	public static final DateTime START_DATETIME = new DateTime(2011, 1, 10, 5, 0, 0 ,0, GregorianChronology.getInstance()); //07AM on 10Jan, 2011
	/**
	 * Simulation End time, w.r.t real time clock
	 */
	public static final DateTime END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());//10PM on 12Jan, 2011
	
	

	//RINSIM PARAMETERS
	//public static final int TOTAL_PRODUCTION_SITES = 2;

	
	public class Weights {
		//public static final int LAGTIME =  20;
		public static final int TRAVEL_TIME = 20;
		public static final int CONCRETE_WASTAGE = 10;
		public static final int STARTTIME_DELAY = 10;
		public static final int PREFFERED_STATION =1;
	}
	
	@Override
	public String toString() {
		StringBuffer strbuf = new StringBuffer();
		strbuf.append("\nDEPHASE_INTERVAL_SEC = ").append(DEPHASE_INTERVAL_SEC);
		strbuf.append("\nFEASIBILITY_INTERVAL_SEC = ").append(FEASIBILITY_INTERVAL_SEC);
		strbuf.append("\nFEASIBILITY_EVAPORATION_INTERVAL_SEC = ").append(FEASIBILITY_EVAPORATION_INTERVAL_SEC);
		strbuf.append("\nEXPLORATION_INTERVAL_SEC = ").append(EXPLORATION_INTERVAL_SEC);
		strbuf.append("\nINTENTION_INTERVAL_SEC = ").append(INTENTION_INTERVAL_SEC);
		strbuf.append("\nINTENTION_EVAPORATION_SEC = ").append(INTENTION_EVAPORATION_SEC);
		strbuf.append("\nEXPLORATION_ANT_SEARCH_FOR_SEC_AFTER = ").append(EXPLORATION_ANT_SEARCH_FOR_SEC_AFTER);
		strbuf.append("\nEXPLORATION_SCHEDULE_SIZE = ").append(EXPLORATION_SCHEDULE_SIZE);
		strbuf.append("\nIS_FIXED_VEHICLE_CAPACITY = ").append(IS_FIXED_VEHICLE_CAPACITY);
		strbuf.append("\nFIXED_CAPACITY = ").append(FIXED_CAPACITY);
		strbuf.append("\nTRUCK_SPEED = ").append(TRUCK_SPEED);
		strbuf.append("\nMINUTES_TO_PERISH_CONCRETE = ").append(MINUTES_TO_PERISH_CONCRETE);
		strbuf.append("\nMAX_LAG_TIME_MINUTES = ").append(MAX_LAG_TIME_MINUTES);
		strbuf.append("\nLAG_TIME_ENABLE").append(LAG_TIME_ENABLE);
		strbuf.append("\nMINUTES_TO_CHANGE_ST4ORDER = ").append(MINUTES_TO_CHANGE_ST4ORDER);
		strbuf.append("\nMINUTES_TO_DELAY_ST = ").append(MINUTES_TO_DELAY_ST);
				
		strbuf.append("\nMINUTES_BEFORE_ORDER_SHOULDBE_BOOKED = ").append(MINUTES_BEFORE_ORDER_SHOULDBE_BOOKED);
		strbuf.append("\nMINUTES_BEFOR_DELIVERY_CREATED = ").append(MINUTES_BEFOR_DELIVERY_CREATED);
		strbuf.append("\nDISCHARGE_RATE_PERHOUR = ").append(DISCHARGE_RATE_PERHOUR);
		strbuf.append("\nLOADING_MINUTES = ").append(LOADING_MINUTES);
		strbuf.append("\nMAX_NO_OF_EXPLORATION_FOR_ORDER = ").append(MAX_NO_OF_EXPLORATION_FOR_ORDER);
		strbuf.append("\nAVAILABLE_SLOT_SIZE_HOURS = ").append(AVAILABLE_SLOT_SIZE_HOURS);
		strbuf.append("\nENABLE_TRUCK_BREAKDOWN = ").append(ENABLE_TRUCK_BREAKDOWN);
		strbuf.append("\nENABLE_JI = ").append(ENABLE_JI);
		strbuf.append("\nEXP_RANKING_WITH_SCORE_ENABLE = ").append(EXP_RANKING_WITH_SCORE_ENABLE);
		strbuf.append("\nEXP_BEST_DE_PRIORITIZE_DEL0 = ").append(EXP_BEST_DE_PRIORITIZE_DEL0);
		strbuf.append("\nEXP_BEST_PRIORATIZE_NOT_SERVING_ORDERS = ").append(EXP_BEST_PRIORATIZE_NOT_SERVING_ORDERS);
		strbuf.append("\nPS_ALWAYS_AVAILABLE = ").append(PS_ALWAYS_AVAILABLE);
		strbuf.append("\nTRUCK_BREAKDOWN_PROBABILTY = ").append(TRUCK_BREAKDOWN_PROBABILTY);
		strbuf.append("\nSTART_DATETIME = ").append(START_DATETIME);
		strbuf.append("\nEND_DATETIME = ").append(END_DATETIME);
		
		strbuf.append("\nSCALE = ").append(SCALE);
		strbuf.append("\nSTRESS = ").append(STRESS);
		strbuf.append("\nDATA_FOLDER = ").append(DATA_FOLDER);
		strbuf.append("\nRESULT_FOLDER = ").append(RESULT_FOLDER);
		strbuf.append("\nINPUT_FILE = ").append(INPUT_FILE);
		strbuf.append("\nINPUT_INSTANCE_TYPE = ").append(INPUT_INSTANCE_TYPE);
		
		return strbuf.toString();

	}
	
	public enum PERCENT {
		per100 (0), //100 percent dynamic, so zero orders should be know before start of simulation, 
					//	rather, after simulaton started, orderManagerInitial should add orders according to their startTime 
		per50 (50),  ///50 percent orders are dynamic, so in the begining randomly 50percent orders added, irrespective of their start time.
					//later, in first tick of orderManagerInitial, the 
		per0(100); //nothing will be dynamic, all orders are known before hand, irrespective of their start Time
		
		
		private final int value;
		
		PERCENT (int pValue) {
			this.value = pValue;
		}
		
		public int getPerOfX(int X) {
			int perOfX = this.value * X/100;
			
			return perOfX;
		}
	}
	public static String getFileNamePrefix() {
			return SCALE + "/" + STRESS +"_" + SCALE;
		}
}




