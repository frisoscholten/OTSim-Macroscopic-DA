package nl.tudelft.otsim.GeoObjects;

import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import nl.tudelft.otsim.FileIO.ParsedNode;
import nl.tudelft.otsim.FileIO.StaXWriter;
import nl.tudelft.otsim.FileIO.XML_IO;
import nl.tudelft.otsim.GUI.GraphicsPanel;
import nl.tudelft.otsim.GUI.InputValidator;
import nl.tudelft.otsim.SpatialTools.Planar;
import nl.tudelft.otsim.Utilities.Reversed;

/**
 * The Link class implements links between two instances of a {@link Node}.
 * A Link has a design line that starts at the origin {@link Node}, then
 * follows zero or more intermediate vertices and ends at the destination
 * {@link Node}. The way the Link looks is described by one or more
 * {@link CrossSection CrossSections} that are <i>swept</i> along the design line.
 * @author Guus F Tamminga, Peter Knoppers
 *
 */
public class Link implements XML_IO {
	/** Name for a Link element when stored in XML format */
	public static final String XMLTAG = "link";
	
	/** Name for a Link name element when stored in XML format */
	private static final String XML_NAME = "name";
	/** Name for a Link ID element when stored in XML format */
	private static final String XML_ID = "ID";
	/** Name for a Link fromNode element when stored in XML format */
	private static final String XML_FROMNODE = "fromNode";
	/** Name for a Link toNode element when stored in XML format */
	private static final String XML_TONODE = "toNode";
	/** Name for a Link distance element when stored in XML format */
	private static final String XML_DISTANCE= "distance";
	/** Name for a Link priority element when stored in XML format */
	private static final String XML_PRIORITY= "priority";
	/** Name for a Link intermediatePoint element when stored in XML format */
	private static final String XML_VERTICES = "intermediatePoint";
	/** Name for a Link crossSection element when stored in XML format */
	private static final String XML_CROSSSECTION = "crossSection";
	
	// TODO: ensure that crossSections are sorted by their distance along the link
	// (currently they are stored in the order of the XML file which may be VERY wrong)
	
    private String name;
    private Node fromNode;
    private Node fromNodeExpand;
    private Node toNode;
    private Node toNodeExpand;
    private double length = Double.NaN;
    //TODO: explain what this field is for (GUUS)
    private boolean priority;
    private ArrayList<CrossSection> crossSections;
	private double maxSpeed;
	private ArrayList<Vertex> intermediateVertices;
	//private boolean zoneConnector = false;

	/**
	 * The {@link Network} that this Link is part of.
	 */
	public final Network network;

	/**
	 * Create a Link between two {@link Node Nodes}.
	 * @param network {@link Network} that is the owner of the nodes (and the new Link)
	 * @param linkName Name of the new link
	 * @param fromNode {link Node} that the link departs from
	 * @param toNode {@link Node} that the link ends at
	 * @param length Length of the link in meters
	 * @param priority XXXXX
	 * @param crossSections List of {@link CrossSection CrossSections} along the link
	 * @param intermediateVertices Refinement of the shape of the link with a list of {@link Vertex} objects
	 * @throws Error when fromNode or toNode is null.
	 */
	public Link (Network network, String linkName, Node fromNode, Node toNode, double length, boolean priority, ArrayList<CrossSection> crossSections,  ArrayList<Vertex> intermediateVertices) {
    	if (null == network)
    		throw new Error("network is null");
    	if (null != network.lookupLink(linkName))
    		throw new Error("link name already exists in the Network");
		this.network = network;
    	this.name = linkName;
        this.fromNode = fromNode;
		if (null == fromNode)
			throw new Error(String.format("fromNode is null"));
        this.toNode = toNode;
		if (null == toNode)
			throw new Error(String.format("toNode is null"));
        this.length = length;
        this.priority = priority;
        this.crossSections = crossSections;
        this.maxSpeed = 70;
        this.intermediateVertices = intermediateVertices;
        for (CrossSection cs : crossSections)
        	cs.setLink(this);
    }

	/**
	 * Create a Link from a parsed XML file.
	 * @param network {@link Network}; the Network that will own the new Link
	 * @param pn {@link ParsedNode}; the root of the Link in the parsed XML file
	 * @throws Exception
	 */
	public Link(Network network, ParsedNode pn) throws Exception {
		if (null == network)
			throw new Exception("network is null");
		this.network = network;
		name = null;
		fromNode = toNode = null;
		priority = false;
		crossSections = new ArrayList<CrossSection>();
		maxSpeed = 70;
		intermediateVertices = new ArrayList<Vertex>();
		
		for (String fieldName : pn.getKeys()) {
			String value = pn.getSubNode(fieldName, 0).getValue();
			if (fieldName.equals(XML_NAME)) {
				if (null != network.lookupLink(value))
					throw new Exception("Duplicate link name " + value);
				name = value;
			} else if (fieldName.equals(XML_ID))
				;	// ignore
			else if (fieldName.equals(XML_FROMNODE)) {
				fromNode = network.lookupNode(Integer.parseInt(value), false);
				if (null == fromNode)
					throw new Exception("Could not find fromNode " + value);
			} else if (fieldName.equals(XML_TONODE)) {
				toNode = network.lookupNode(Integer.parseInt(value), false);
				if (null == fromNode)
					throw new Exception("Could not find toNode " + value);
			} else if (fieldName.equals(XML_DISTANCE))
				length = Double.parseDouble(value);
			else if (fieldName.equals(XML_PRIORITY))
				priority = Boolean.parseBoolean(value);
			else if (fieldName.equals(XML_VERTICES))
				for (int index = 0; index < pn.size(fieldName); index++)
					intermediateVertices.add(new Vertex(pn.getSubNode(fieldName, index)));
			else if (fieldName.equals(XML_CROSSSECTION))
				for (int index = 0; index < pn.size(fieldName); index++)
					crossSections.add(new CrossSection(this, pn.getSubNode(fieldName, index)));
		}
		if (null == toNode)
			throw new Exception("toNode not defined " + pn.description());
		if (null == fromNode)
			throw new Exception("fromNode not defined " + pn.description());
	}

	/**
	 * Retrieve the name of the link
	 * @return Name of the link
	 */
    public String getName_r() {
		return name;
	}

    /**
     * Set or change the name of the Link
     * @param name New name of the Link
     */
	public void setName_w(String name) {
		this.name = name;
	}

	/**
	 * Check that a proposed name is acceptable for {@link #setName_w}.
	 * @return InputValidator that checks that a String is acceptable as name for the {@link Link}
	 */
	@SuppressWarnings("static-method")
	public InputValidator validateName_v() {
		return new InputValidator("[a-zA-Z_][a-zA-Z_0-9]*");
	}

	/**
	 * Retrieve the intermediate vertices of the Link.
	 * @return ArrayList<{@link Vertex}>
	 */
	public ArrayList<Vertex> getIntermediateVertices_r() {
		return intermediateVertices;
	}
	
	/**
	 * Replace the list of intermediate vertices of the Link. The first and last
	 * vertex of the link are not part of this list.
	 * @param intermediateVertices ArrayList<{@link Vertex}> that will replace the current list
	 */
	public void setIntermediateVertices_w(ArrayList<Vertex> intermediateVertices) {
		this.intermediateVertices = intermediateVertices;
	}

	/**
	 * Retrieve the list of vertices that describe the design line of the Link.
	 * The result starts with the {@link #fromNode} of the Link, followed by all
	 * {@link #intermediateVertices} of the link and ends with the {@link #toNode}
	 * of the Link. If the size of the fromNode and/or toNode has been computed,
	 * extra vertices are inserted at the boundaries of these nodes.
	 * @return ArrayList<{@link Vertex}> that describes the entire design line
	 * of the Link.
	 */
	public ArrayList<Vertex> getVertices() {
		ArrayList<Vertex> result = new ArrayList<Vertex>(intermediateVertices);
		//System.out.println("intermediatevertices is " + Planar.verticesToString(result));
		//result.add(0, getFromNodeExpand());	// insert from node Vertex at head
		result.add(0, getFromNode_r());
		//result.add(getToNodeExpand());		// append to node Vertex at tail
		result.add(getToNode_r());
		if (null == toNode)
			throw new Error("Cannot happen");
		final double veryClose = 0.0001;
		if (null != toNode.getCircle()) {
			// Insert an extra vertex where this link (if it were sufficiently wide) enters the circle of toNode
			Line2D.Double line = new Line2D.Double(result.get(result.size() - 2).getPoint(), result.get(result.size() - 1).getPoint());
			Point2D.Double projection = Planar.nearestPointOnLine(line, toNode.getCircle().center());
			double distanceFromP1 = line.getP1().distance(projection) - toNode.getCircle().radius();
			double distanceP1P2 = line.getP1().distance(line.getP2());
			if (distanceFromP1 < distanceP1P2) {
				Vertex v = Vertex.weightedVertex(distanceFromP1 / distanceP1P2, result.get(result.size() - 2), result.get(result.size() - 1));
				deleteVerticesBetween(result, v, result.get(result.size() - 1));
				if (result.get(result.size() - 2).distance(v) < veryClose)
					result.set(result.size() - 2, v);
				else
					result.add(result.size() - 1, v);
				//System.out.println("inserting vertex " + v.toString() + " before end");
			} // else link ends before entering the circle
		}
		if (null != fromNode.getCircle()) {
			// Insert an extra vertex where this link (if it were infinitely wide) leaves the circle of fromNode
			Line2D.Double line = new Line2D.Double(result.get(1).getPoint(), result.get(0).getPoint());
			Point2D.Double projection = Planar.nearestPointOnLine(line, fromNode.getCircle().center());
			double distanceFromP1 = line.getP1().distance(projection) - fromNode.getCircle().radius();
			double distanceP1P2 = line.getP1().distance(line.getP2());
			if (distanceFromP1 < distanceP1P2) {
				Vertex v = Vertex.weightedVertex(distanceFromP1 / distanceP1P2, result.get(1), result.get(0));
				deleteVerticesBetween (result, result.get(0), v);
				if (result.get(1).distance(v) < veryClose)
					result.set(1, v);
				else
					result.add(1, v);
				//System.out.println("inserting vertex " + v.toString() + " after start");
			} // else link ends before entering the circle
		}
		return result;
	}
	
	/**
	 * Delete {@link Vertex vertices} between the given two from a list. The first or last
	 * entry in the list will never be removed.
	 * @param vertices ArrayList&lt;{@link Vertex}&gt;; the list of Vertices
	 * @param from {@link Vertex}; the lower bound of the range to remove
	 * @param to {@link Vertex}; the upper bound of the range to remove
	 */
	private static void deleteVerticesBetween(ArrayList<Vertex> vertices, Vertex from, Vertex to) {
		double distance = from.distance(to);
		for (int i = vertices.size() - 1; --i > 0; ) {
			Vertex v = vertices.get(i);
			if ((v.distance(from) < distance) && (v.distance(to) < distance))
				vertices.remove(i);
		}
	}
	
	/**
	 * Retrieve the maximum allowed speed on the link in m/s
	 * @return double precision floating point value (speed limit in km/h)
	 */
    public double getMaxSpeed_r() {
        return maxSpeed;
    }
    
    /**
     * Set or update the speed limit on the Link
     * @param maxSpeed new speed limit (in km/h)
     */
    public void setMaxSpeed_w(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
   
    /**
     * Check that a proposed new speed limit value is acceptable to {@link #setMaxSpeed_w}
     * @return {@link InputValidator} to check speed limit value
     */
	@SuppressWarnings("static-method")
	public InputValidator validateMaxSpeed_v() {
    	return new InputValidator("[.,0-9].*", 5, 200); 
    }
    
	/**
	 * Retrieve the length of the link in m.
	 * @return Length of the link (in m)
	 */
    public double getLength() {
        return length;
    }
    
    /**
     * Retrieve the value of the priority field of this Link.
     * @return Boolean
     */
    public boolean isPriority() {
		return priority;
	}

    /**
     * Update the value of the priority field of this Link.
     * @param priority Boolean; new value for the priority field
     */
	public void setPriority(boolean priority) {
		this.priority = priority;
	}

	/**
	 * Replace the list of {@link CrossSection CrossSections} of the Link.
	 * @param crossSections ArrayList<{@link CrossSection}> that will replace the current one
	 */
	public void setCrossSections(ArrayList<CrossSection> crossSections) {
		this.crossSections = crossSections;
	}

	/**
	 * Retrieve the current list of {@link CrossSection CrossSections} of the Link.
	 * @return The current ArrayList<{@link CrossSection}> of the Link
	 */
	public ArrayList<CrossSection> getCrossSections_r() {
		//System.out.println("getCrossSections_r called for link " + name);
		if (null == crossSections)
			throw new Error("Oops: crossSections is null");
		return crossSections;
	}
	
	/**
	 * Retrieve the starting {@link Node} of the Link.
	 * @return fromNode field of the Link 
	 */
	public Node getFromNode_r() {
		return fromNode;
	}
	
	public Node getExpandedFromNode_r() {
		return fromNodeExpand;
	}
	
	/**
	 * Change the starting {@link Node} of the Link.
	 * @param fromNode New starting {@link Node} of the Link
	 */
	public void setFromNode_w(Node fromNode) {
		if (null == toNode)
			throw new Error("WTF");
		this.fromNode = fromNode;
	}
	
	/**
	 * Retrieve the ending {@link Node} of the Link.
	 * @return toNode field of the Link
	 */
	public Node getToNode_r() {
		return toNode;
	}
	
	public Node getExpandedToNode_r() {
		return toNodeExpand;
	}

	/**
	 * Change the ending {@link Node} of the Link.
	 * @param toNode New ending {@link Node} of the Link
	 */
	public void setToNode_w(Node toNode) {
		if (null == toNode)
			throw new Error("WTF");
		this.toNode = toNode;
	}
	
	/**
	 * At junctions it may be necessary to add additional {@link Node Nodes} and
	 * {@link Link Links} to make route choices for lane base simulators explicit.
	 * This method retrieves this additional Node. If no additional Node was
	 * needed, this method retrieves the fromNode of the Link.
	 * @return {@link Node} the inserted junction node near the beginning of 
	 * the link, or the fromNode field of the Link.
	 */
    public Node getFromNodeExpand() {
		return fromNodeExpand;
	}

    /**
	 * At junctions it may be necessary to add additional {@link Node Nodes} and
	 * {@link Link Links} to make route choices for lane base simulators explicit.
     * This method sets or replaces the additional Node.
     * @param fromNodeExpand The new inserted {@link Node} near the start of this link 
     */
	public void setFromNodeExpand(Node fromNodeExpand) {
		if (null == fromNode)
			throw new Error("WTF");
		this.fromNodeExpand = fromNodeExpand;
	}

	/**
	 * At junctions it may be necessary to add additional {@link Node Nodes} and
	 * {@link Link Links} to make route choices for lane base simulators explicit.
	 * This method retrieves this additional Node. If no additional Node was
	 * needed, this method retrieves the toNode of the Link.
	 * @return {@link Node} the inserted junction node near the end of the 
	 * link, or the toNode field of the Link.
	 */
	public Node getToNodeExpand() {
		return toNodeExpand;
	}

    /**
	 * At junctions it may be necessary to add additional {@link Node Nodes} and
	 * {@link Link Links} to make route choices for lane base simulators explicit.
     * This method sets or replaces the additional Node.
     * @param toNodeExpand The new inserted {@link Node} near the end of this link 
     */
	public void setToNodeExpand(Node toNodeExpand) {
		if (null == toNode)
			throw new Error("WTF");
		this.toNodeExpand = toNodeExpand;
	}

	/**
	 * Retrieve the {@link CrossSection} that describes this Link at the
	 * beginning or the end
	 * @param atEnd If true: return the {@link CrossSection} at the end of the 
	 * Link otherwise return the {@link CrossSection} at the beginning of the
	 * Link.
	 * @return The requested {@link CrossSection}
	 */
	public CrossSection getCrossSectionAtNode(boolean atEnd) {
		return crossSections.get(atEnd ? crossSections.size() - 1 : 0);
	}

	/**
	 * Retrieve the name of the Link.
	 * @return The name of the Link
	 */
	@Override
	public String toString() {
		return name;
	}
	
	/**
	 * Re-generate additional Nodes and Links that are needed by lane based
	 * simulator to make all route choice points at a junction explicit.
	 */
	public void fixPhase1() {
		setFromNodeExpand(fromNode);			
		setToNodeExpand(toNode);		
		for (CrossSection cs : getCrossSections_r()) {
    		cs.setLink(this);
        	List<CrossSectionElement> elementList = cs.getCrossSectionElementList_r();
        	for (CrossSectionElement cse : elementList) {
        		String typologyName = cse.getName_r();
        		if (null == typologyName)
        			throw new Error ("CrossSectionElement has null typologyName");
        		for (CrossSectionElementTypology cset : network.getCrossSectionElementTypologyList())
            		if (typologyName.equalsIgnoreCase(cset.getName_r().toString()))
            			cse.setCrossSectionElementTypology(cset);
        		if (null == cse.getCrossSectionElementTypology())
        			throw new Error("Undefined crossSectionElementTypology " + typologyName);
        		for (CrossSectionObject cso : cse.getCrossSectionObjects(RoadMarkerAlong.class)) {
        			RoadMarkerAlong rma = (RoadMarkerAlong) cso;
            		for (RoadMarkerAlongTemplate rmat : network.getRoadMarkerTemplateList()) {
                		if (rmat.getType().equals(rma.getType()))
                			rma.setMarkerWidth(rmat.getMarkerWidth());
		            	if (null == rmat.getType())
		            		throw new Error("Undefined RoadMarkerAlongTemplate " + rma.getType());
        		    }
            		if (Double.isNaN(rma.getMarkerWidth()))
            			throw new Error("No road marker template for roadMarkerAlong type \"" + rma.getType() + "\"");	
        		}
/*        		for (CrossSectionObject cso : cse.getCrossSectionObjects(TurnArrow.class)) {
        			TurnArrow turnArrow = (TurnArrow) cso;
        		}*/
        	}	
        }
	}
	
	/**
	 * Re-calculate the length of this Link from the specified list of vertices.
	 */
    public void calculateLength() {
    	length = Planar.length(getVertices());
   }

    /**
     * Re-build the list of {@link Lane Lanes} of the Link.
     */
	public void rebuildLanes() {
		for (CrossSection cs : crossSections)
			for (CrossSectionElement cse : Reversed.reversed(cs.getCrossSectionElementList_r()))  {
				for (CrossSectionObject cso : Reversed.reversed(cse.getCrossSectionObjects(RoadMarkerAlong.class)))
            		((RoadMarkerAlong) cso).createVertices();
				cse.createLanes();
			}
	}
	
	/**
	 * Clean up the way that the {@link CrossSection CrossSections} of this 
	 * Link are joined.
	 * @param csList 
	 */
	public static void connectSuccessiveLanesAtLink(ArrayList<CrossSection> csList) {
   		CrossSection csPrev = null;
   		for (CrossSection cs : csList) {
        	if (csPrev != null) 
        		connectSectionElements(csPrev, cs);
        	csPrev = cs;
   		}
	}
	
	public static void connectSuccessiveLanesAtNode(Node node) {
		int inCount = node.incomingCount();
		int outCount = node.leavingCount();
		// the simple case: two links 
		if ((inCount == 1) && (outCount == 1) )	{
			// set the neighbor index of the end cross section of the entering
			// and the starting cross section of the leaving link
	    	Link fromLink = node.getLinksFromJunction(true).get(0).link;
	    	Link toLink = node.getLinksFromJunction(false).get(0).link;
	    	if (! fromLink.getFromNode_r().equals(toLink.getToNode_r())) {
		    	ArrayList<CrossSection> csList = new ArrayList<CrossSection>();
				CrossSection inCS = fromLink.getCrossSections_r().get(fromLink.getCrossSections_r().size() - 1);
				CrossSection outCS = toLink.getCrossSections_r().get(0);
				csList.add(inCS);
				csList.add(outCS);
				if (! fromLink.getFromNode_r().equals(toLink.getToNode_r()))
					inCS.linkToCrossSection(outCS); 				
				connectSectionElements(inCS, outCS);	
	    	}
		}
	}

	public static void connectSectionElements(CrossSection csPrev, CrossSection cs)   {
	    for (CrossSectionElement csePrev : csPrev.getCrossSectionElementList_r()) {
	        if (csePrev.getCrossSectionElementTypology().getDrivable()) {
	        	int neighborIndex = csePrev.getNeighborIndex();
	        	// There is no guarantee that every drive-able CrossSectionElement gets linked
	        	if (neighborIndex < 0)
	        		continue;
	        	CrossSectionElement cse = cs.getCrossSectionElementList_r().get(neighborIndex);
		    	cse.fixLaneJump(csePrev);
	        }
	 	}
	}
    
	private boolean writeXMLVertices(StaXWriter staXWriter) {
		for (Vertex v : getIntermediateVertices_r())
			if (! (staXWriter.writeNodeStart(XML_VERTICES)
					&& v.writeVertexXML(staXWriter)
					&& staXWriter.writeNodeEnd(XML_VERTICES)))
				return false;
		return true;
	}
	
	private boolean writeXMLCrossSections(StaXWriter staXWriter) {
		for (CrossSection cs : getCrossSections_r())
			if (! cs.writeXML(staXWriter))
				return false;
		return true;
	}
	
	/**
	 * Write this Link to an XML file.
	 * @param staXWriter {@link StaXWriter} for the XML file
	 * @return Boolean; true on success; false on failure
	 */
	@Override
	public boolean writeXML(StaXWriter staXWriter) {
		return staXWriter.writeNodeStart(XMLTAG)
				&& staXWriter.writeNode(XML_NAME, getName_r())
				&& staXWriter.writeNode(XML_FROMNODE, Integer.toString(getFromNode_r().getNodeID()))
				&& staXWriter.writeNode(XML_TONODE, Integer.toString(getToNode_r().getNodeID()))
				&& staXWriter.writeNode(XML_DISTANCE, Double.toString(getLength()))
				&& writeXMLVertices(staXWriter)
				&& writeXMLCrossSections(staXWriter)
				&& staXWriter.writeNodeEnd(XMLTAG);
	}

	public void regenerateVertices() {
		for (CrossSection cs : getCrossSections_r())
			for (CrossSectionElement cse : cs.getCrossSectionElementList_r())
				cse.regenerateVertices();
	}

	/**
	 * Draw the design line of this Link on a GraphicsPanel.
	 * @param graphicsPanel {@link GraphicsPanel}; the GraphicsPanel to draw on.
	 */
	public void paint(GraphicsPanel graphicsPanel) {
		graphicsPanel.setColor(Color.BLUE);
		graphicsPanel.setStroke(0); // hair line
		graphicsPanel.drawPolyLine(getVertices());
	}
	
}