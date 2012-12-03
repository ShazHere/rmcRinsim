package shaz.rmc.core.domain;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.apache.log4j.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shaz.rmc.core.domain.ConstructionYard;
import shaz.rmc.core.domain.Order;
import shaz.rmc.core.domain.Problem;
import shaz.rmc.core.domain.Station;
import shaz.rmc.core.domain.Vehicle;
import shaz.rmc.core.domain.VehicleType;

public class XMLProblemDAO implements Serializable {

	private static final Logger log = Logger.getLogger(XMLProblemDAO.class);
	
	private final File xml;
	
	public XMLProblemDAO(final File xml) {
		this.xml = xml;
	}
	
	public Problem loadProblem() throws IOException {
		
		Problem result = new Problem();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(xml);
			
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			
			//log.info("before parse");
			
			parseStations(doc,xPath,result);
			log.info("stations read");
			parseVehicles(doc,xPath,result);
			log.info("vehicles read");
			parseOrders(doc,xPath,result);
			log.info("orders read");
			return result;
		} catch(Exception e) {
			log.error("Unable to parse problem from xml.",e);
			System.out.println("exception in parsing file");
			throw new IOException(e);
		} finally {
					
		}
	}
	public Problem loadOrdersAndTrucks() throws IOException {
		Problem result = new Problem();
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(xml);
			
			XPathFactory xPathFactory = XPathFactory.newInstance();
			XPath xPath = xPathFactory.newXPath();
			
			//log.info("before parse");
			
			parseOrders(doc,xPath,result);
			log.info("orders read");
			parseVehicles(doc,xPath,result);
			log.info("vehicles read");
			
			return result;
		} catch(Exception e) {
			log.error("Unable to parse problem from xml.",e);
			System.out.println("exception in parsing file");
			throw new IOException(e);
		} 
	}

	private void parseStations(Document doc, XPath xPath, Problem problem) 
		throws XPathExpressionException {
		
		log.debug("Parsing stations.");
		Set<Station> stations = new HashSet<Station>();
		
		XPathExpression xPathExpression = xPath.compile("//Stations/Station");
		Object result = xPathExpression.evaluate(doc,XPathConstants.NODESET);
		
		NodeList nodes = (NodeList)result;
		for(int i = 0; i<nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				try {
					stations.add(parseStation(element,problem));
				} catch (NullPointerException npe) {
					log.debug("Could not parse station due to nullpointer exception", npe);
				}
			}
		}
		
		for(Station station : stations) {
			problem.addStation(station);
		}
	}
	
	private Station parseStation(Element element, Problem problem) {
		
		log.trace("  Parsing station:");
		String id = getTagValue("StationCode", element);
		log.trace("    id = "+id);
		Duration loadingDuration = new Duration(1000*60*new Integer(getTagValue("LoadingMinutes", element)));
		log.trace("    loadingDuration = "+loadingDuration);
		
		Station station = new Station(id, loadingDuration);
		return station;
	}
	
	private void parseVehicles(Document doc, XPath xPath, Problem problem) 
		throws XPathExpressionException {
		
		log.debug("Parsing vehicles.");
		Set<Vehicle> vehicles = new HashSet<Vehicle>();
		
		XPathExpression xPathExpression = xPath.compile("//Vehicles/Vehicle");
		Object result = xPathExpression.evaluate(doc,XPathConstants.NODESET);
		
		NodeList nodes = (NodeList)result;
		for(int i = 0; i<nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				try {
					Vehicle veh = parseVehicle(element,problem);
					if (veh!= null)
						vehicles.add(veh);
				} catch (Exception npe) {
					log.debug("Could not parse vehicle due to exception", npe);
				}
			}
		}
		
		for(Vehicle vehicle : vehicles) {
			problem.addVehicle(vehicle);
		}
	}

	private Vehicle parseVehicle(Element element, Problem problem)
		throws Exception {
		
		log.trace("  Parsing vehicle:");
		
		String id = getTagValue("VehicleCode", element);
		log.trace(String.format("id = {}",id));
		
		BigDecimal nominalVolume = new BigDecimal(getTagValue("NormalVolume", element));
		nominalVolume = nominalVolume.multiply(new BigDecimal(1000));
		log.trace(String.format("nominalVolume = {}",nominalVolume));
		
		if(nominalVolume.equals(BigDecimal.ZERO))
			return null;//throw new Exception("Nominal volume = 0");
		
		BigDecimal maximalVolume = new BigDecimal(getTagValue("MaximumVolume", element));
		maximalVolume = maximalVolume.multiply(new BigDecimal(1000));
		log.trace(String.format("maximumVolume = {}",maximalVolume));
		
		if(maximalVolume.equals(BigDecimal.ZERO)) {
			maximalVolume = nominalVolume;
			log.debug("Adapting a maximal volume to a nominal volume");
		}
		
		
		BigDecimal dischargePerHour = new BigDecimal(getTagValue("DischargeM3PerHour",element));
		dischargePerHour = dischargePerHour.multiply(new BigDecimal(1000));
		log.trace(String.format("dischargePerHour = {}",dischargePerHour));
		
		
		String vehicleType = getTagValue("VehicleType", element);
		log.trace(String.format("vehicleType = {}",vehicleType));
		
		VehicleType vt = problem.getVehicleType(vehicleType);
		if(vt == null) {
			problem.addVehicleType(new VehicleType(vehicleType));
		}
		
		Integer pumpLineLength = null;
		try {
			String lineLength = getTagValue("PumpLineLength", element);
			lineLength = lineLength.replace("m", "");
			pumpLineLength = Integer.parseInt(lineLength);
			log.trace(String.format("pumpLineLength = {}",pumpLineLength));
		} catch (Exception e) {
			log.debug("Could not parse pumpline length due to exception.",e);
		}
		
		
		return new Vehicle(id, vt, nominalVolume.intValue(),
				maximalVolume.intValue(), dischargePerHour.intValue(),pumpLineLength);
	}

	private void parseOrders(Document doc, XPath xPath, Problem problem) 
		throws XPathExpressionException, DatatypeConfigurationException {
		
		log.trace("Parsing orders.");
		Set<Order> orders = new HashSet<Order>();
		
		XPathExpression xPathExpression = xPath.compile("//Orders/Order");
		Object result = xPathExpression.evaluate(doc,XPathConstants.NODESET);
		
		NodeList nodes = (NodeList)result;
		for(int i = 0; i<nodes.getLength(); i++) {
			Node node = nodes.item(i);
			
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				orders.add(parseOrder(element,problem));
				
			}
		}
		
		for(Order order : orders) {
			problem.addOrder(order);
		}
	}
	
	private Order parseOrder(Element orderElement,Problem problem) throws DatatypeConfigurationException {
		
		log.trace("  Parsing order:");
		DatatypeFactory factory = DatatypeFactory.newInstance();
		
		String id = getTagValue("OrderCode", orderElement);
		log.trace("    id = "+id);
		
		//log.debug("Parsing Order " + id);
		Integer pumpLineLengthRequired = null;
		try {
			pumpLineLengthRequired = Integer.parseInt(getTagValue("PumpLineLengthRequired", orderElement));
			log.trace("    pumpline length = "+pumpLineLengthRequired);
		} catch (Exception e) {
			log.debug("No pumpline length given for order");
		}
		
		DateTime earliestStartTime = 
			new DateTime(factory.newXMLGregorianCalendar(getTagValue("From", orderElement))
					.toGregorianCalendar());
		log.trace("    earliestStartTime = "+earliestStartTime);
		
		BigDecimal totalVolume = new BigDecimal(getTagValue("TotalVolumeM3", orderElement));
		totalVolume = totalVolume.multiply(new BigDecimal(1000));
		log.trace("    total volume = "+totalVolume);
		
		BigDecimal requiredDischargePerHour = 
			new BigDecimal(getTagValue("RequiredDischargeM3PerHour", orderElement));
		requiredDischargePerHour = requiredDischargePerHour.multiply(new BigDecimal(1000));
		log.trace("    required discharge per hour = "+requiredDischargePerHour);
		
		boolean maximumVolumeAllowed = 
			Boolean.parseBoolean(getTagValue("MaximumVolumeAllowed",orderElement));
		log.trace("    maximum volume allowed = "+maximumVolumeAllowed);
		
		boolean isPickup = Boolean.parseBoolean(getTagValue("IsPickUp", orderElement));
		log.trace("    is pickup = "+isPickup);
		
		String preferredStationCode = getTagValue("PreferredStationCode", orderElement);
		log.trace("    preferred station code = "+preferredStationCode);	
		
		long waitingMinutes = Long.parseLong(getTagValue("WaitingMinutes",orderElement));
		log.trace("    waiting minutes = "+waitingMinutes);
		Duration waitingMinutesDuration = new Duration(waitingMinutes*1000*60);
		
		
		Node node = orderElement.getElementsByTagName("ConstructionYard").item(0);
		
		ConstructionYard constructionYard = parseConstructionYard((Element)node,problem);
		
		node = orderElement.getElementsByTagName("ProhibitedVehicleTypes").item(0);
		
		Set<VehicleType> prohibitedVehicleTypes = new HashSet<VehicleType>();
		if(node != null) {
			prohibitedVehicleTypes.addAll(parseProhibitedVehicleTypes((Element)node,problem));

		}
		
		Order order = new Order(
				id,
				constructionYard,
				pumpLineLengthRequired,
				totalVolume.intValue(),
				requiredDischargePerHour.intValue(),
				earliestStartTime,
				maximumVolumeAllowed,
				isPickup,
				preferredStationCode,
				waitingMinutesDuration);
		
		order.getProhibitedVehicleTypes().addAll(prohibitedVehicleTypes);
		
		return order;
	}
	
	private Set<VehicleType> parseProhibitedVehicleTypes(Element vehicleTypesNode,
			Problem problem) {
		log.trace("    Parsing prohibited vehicle types:");
		Set<VehicleType> vehicleTypes = new HashSet<VehicleType>();
		
		NodeList childNodes = vehicleTypesNode.getChildNodes();
		
		for(int i = 0; i<childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				String vehicleType = element.getTextContent();
				
				log.trace("      vehicleType = "+vehicleType);
				vehicleTypes.add(problem.getVehicleType(vehicleType));
			}
		}
		
		return vehicleTypes;
	}

	private ConstructionYard parseConstructionYard(Element node, Problem problem) {
		
		log.trace("    Parsing construction yard:");
		
		String id = getTagValue("ConstructionYardCode", node);
		log.trace("      id = "+id);
		
		Duration waitingDuration = new Duration(1000*60*new Integer(getTagValue("WaitingMinutes", node)));
		log.trace("      waitingDuration = "+waitingDuration);
		
		ConstructionYard yard = new ConstructionYard(id, waitingDuration);

		Element stationDurationNode = (Element)node.getElementsByTagName("StationDurations").item(0);
		NodeList  stationDurationsList = stationDurationNode.getElementsByTagName("StationDuration");
		
		log.trace("      Parsing station durations:");
		for(int i = 0; i<stationDurationsList.getLength(); i++) {
			
			Station station = 
				problem.getStation(getTagValue("StationCode", (Element)stationDurationsList.item(i)));
			
			if(station == null) {
				log.debug("Did not add station to distancematrix due to npe earlier.");
				continue;
			}
			//else
				//log.debug("Station Added with code " + station.getId());
			Duration drivingDuration = 
				new Duration(1000*60*new Integer(getTagValue("DrivingMinutes", (Element)stationDurationsList.item(i))));
			
			String direction = getTagValue("Direction", (Element)stationDurationsList.item(i)); 

			if("From".equals(direction)) {
				log.trace(String.format("        From {} to {} : {}",new Object[] {station,yard,drivingDuration}));
				problem.addDuration(station, yard, drivingDuration);
			} else if("To".equals(direction)) {
				log.trace(String.format("        From {} to {} : {}",new Object[] {yard,station,drivingDuration}));
				problem.addDuration(yard, station, drivingDuration);
			} else {
				throw new RuntimeException("Unsupported direction: "+direction);
			}
		}
		
		
		return yard;
	}

	private static String getTagValue(String tag, Element element) {

		NodeList nodeList = element.getElementsByTagName(tag).item(0)
				.getChildNodes();
		Node node = (Node) nodeList.item(0);

		return node.getNodeValue();
	}

}
