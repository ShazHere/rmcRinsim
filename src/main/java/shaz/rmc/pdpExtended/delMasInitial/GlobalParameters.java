/**
 * 
 */
package shaz.rmc.pdpExtended.delMasInitial;

//import java.util.GregorianCalendar;

import org.apache.commons.math3.random.RandomData;
import org.apache.commons.math3.random.RandomDataImpl;

import org.joda.time.Chronology;
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
public class GlobalParameters {

	//public static boolean ENABLE_GUI = true;
	
	//public static final int SIMULATION_SPEED = 200; //specifies what should be execution speed of simulator.
	//public static final int TICK_INTERVAL = 200000; 
	//public static final int INTENTION_EVAPORATION_RATE = 10; //means 20 ticks
	//public static final int EXPLORATION_EVAPORATION_RATE = 20; //means  no. of tics
	public static final int FEASIBILITY_INTERVAL_MIN = 100; //means send feeasibilty ants to near by after every X minutes.
	public static final int FEASIBILITY_EVAPORATION_INTERVAL_MIN = 30;
	public static final int EXPLORATION_INTERVAL_MIN = 100; //means no. of mintues
	public static final int TOTAL_TRUCKS = 1; //almost 29 trucks are there in the instances
	public static final boolean IS_FIXED_VEHICLE_CAPACITY = true; //will all the vehicles have same capacity
	//public static final int FIXED_VEHICLE_CAPACITY =10000;
	public static final int TRUCKS_PLAN_IN_ADVANCE_HOURS = 6; //specifies how much time in advance the truck can plan. but if deliveries are so many, the truck
	//accepts the deliveyr time of even after TRUCKS_PLAN_IN_ADVANCE_HOURS. 
	public static final int MINUTES_TO_PERISH_CONCRETE = 90; //in minutes..
	public static final int MAX_LAG_TIME_MINUTES = 10; //used in OR section, proposed by JOris
	public static final int MINUTES_TO_CHANGE_ST4ORDER = 5; // When shold start time of the order be delayed..
	public static final int MINUTES_TO_DELAY_ST = 10; //means ST should be delayed with MINUTES_TO_DELAY_ST if MINUTES_TO_CHANGE_ST4ORDER time has been elapsed
							//after each change of ST. 
	public static final int DISCHARGE_RATE_PERHOUR = 10; //Constant for all orders
	public static final int LOADING_MINUTES = 5;
	
	//CHECK NOTES ABOUT INPUT FILES!!
	public static final String INPUT_FILE = "planning2011-01-10/planning2011-01-10-update-ucy-oneDayOrdersSameTruck-basic.xml";  //for the data file in Rmc
	//public static final String INPUT_FILE = "planning2011-01-20-update-ucy-sameTruck1.xml";  //total trucks = 27
	//public static final String INPUT_FILE = "planning2011-01-11/planning2011-01-11-update-ucy-shaz-requiredRateFixed.xml";
	//public static final String INPUT_FILE = "planning2011-01-20-update-ucy-SameTruck.xml";  //total trucks = 28
	public static final String DATA_FOLDER= "/Users/Shaza/Documents/try/ReadyMixConcrete/data/2011/";
	public static final String LOG_LOCATION = "hh";  //not used 
	
	public static Problem PROBLEM = null;	//the problem loader
	
	public static final DateTime START_DATETIME = new DateTime(2011, 1, 10, 11, 0, 0 ,0, GregorianChronology.getInstance()); //07AM on 10Jan, 2011
	public static final DateTime END_DATETIME = new DateTime(2011, 1, 10, 23, 55, 0, 0, GregorianChronology.getInstance());//10PM on 12Jan, 2011
	
//	public static OrderManager om; //temprary solution for accessing all the trucks and orders at the end, value is saved by RmcAgentInitialization
	
	public static RandomData RANDOM_DATA_GEN = new RandomDataImpl();  // for generating random sequence having good values..
	
	//RINSIM PARAMETERS
	public static final int TOTAL_PRODUCTION_SITES = 2;
	public class Weights {
		public static final int LAGTIME =  20;
		public static final int TRAVEL_TIME = 20;
		public static final int CONCRETE_WASTAGE = 10;
		public static final int STARTTIME_DELAY = 10;
		public static final int PREFFERED_STATION =1;
	}
}




