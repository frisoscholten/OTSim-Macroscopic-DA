package nl.tudelft.otsim.GeoObjects;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Locale;

import nl.tudelft.otsim.FileIO.StaXWriter;

import org.junit.Test;

/**
 * Test the methods in the Node class
 * <br /> This test is (currently) very incomplete
 * 
 * @author Peter Knoppers
 *
 */
public class NodeTest {

	private static Link createLink (Network network, String name, Node from, Node to, int laneCount) {
		if (0 == laneCount)
			return null;

		final double laneWidth = 3.0;
		final double grassWidth = 1.0;
		final double stripeRoom = 0.2;
		final double stripeWidth = 0.1;

		ArrayList<CrossSection> csl = new ArrayList<CrossSection>();
		ArrayList<CrossSectionElement> csel = new ArrayList<CrossSectionElement>();
		CrossSection cs = new CrossSection(0, 0, csel);
		csel.add(new CrossSectionElement(cs, "grass", grassWidth, new ArrayList<RoadMarkerAlong>(), null));
		ArrayList<RoadMarkerAlong> rmal = new ArrayList<RoadMarkerAlong>();
		rmal.add(new RoadMarkerAlong("|", stripeRoom / 2 + stripeWidth));
		for (int i = 1; i < laneCount; i++)
			rmal.add(new RoadMarkerAlong(":", i * (laneWidth + stripeRoom) + stripeRoom / 2 + stripeWidth));
		rmal.add(new RoadMarkerAlong("|", laneCount * (laneWidth + stripeWidth) + stripeRoom / 2 + stripeWidth));
		csel.add(new CrossSectionElement(cs, "road", laneCount * (laneWidth + stripeRoom) + stripeRoom, rmal, null));
		csel.add(new CrossSectionElement(cs, "grass", grassWidth, new ArrayList<RoadMarkerAlong>(), null));
		cs.setCrossSectionElementList_w(csel);
		csl.add(cs);
		return network.addLink(name, from.getNodeID(), to.getNodeID(), from.distance(to), false, csl, new ArrayList<Vertex>());
	}
	
	private static double round (double in, int fractionalDigits) {
		double multiplier = Math.pow(10, fractionalDigits);
		double result = Math.round(in * multiplier) / multiplier;
		//System.out.println(String.format(Locale.US, "rounding %s to %d digits yields %s", in, fractionalDigits, result));
		return result;
	}
	
	/**
	 * Test junction expansion
	 */
	@Test
	public void testFixLinkConnections() {
		String[] testJunctions = {
				// Fully constrained cases (sum of exiting lanes == sum of feeding lanes)
				"2,0,-90/0,1,0/0,1,180:2.0,1.0//:T junction single left single right",
				"3,0,-90/0,1,0/0,2,180:2.1,2.0,1.0//:T with double left, single right",
				"3,0,-90/0,2,0/0,1,180:2.0,1.1,1.0//:T with single left, double right",
				"4,0,-90/0,2,0/0,2,180:2.1,2.0,1.1,1.0//:T with double left, double right",
				"3,0,-90/0,1,0/0,1,90/0,1,180:3.0,2.0,1.0///:X with single left, single straight, single right",
				"4,0,-90/0,1,0/0,1,90/0,2,180:3.1,3.0,2.0,1.0///:X with double left, single straight, single right",
				"4,0,-90/0,1,0/0,2,90/0,1,180:3.0,2.1,2.0,1.0///:X with single left, double straight, single right",
				"4,0,-90/0,2,0/0,1,90/0,1,180:3.0,2.0,1.1,1.0///:X with single left, single straight, double right",
				"5,0,-90/0,1,0/0,2,90/0,2,180:3.1,3.0,2.1,2.0,1.0///:X with double left, double straight, single right",
				"5,0,-90/0,2,0/0,1,90/0,2,180:3.1,3.0,2.0,1.1,1.0///:X with double left, single straight, double right",
				"5,0,-90/0,2,0/0,2,90/0,1,180:3.0,2.1,2.0,1.1,1.0///:X with single left, double straight, double right",
				"6,0,-90/0,2,0/0,2,90/0,2,180:3.1,3.0,2.1,2.0,1.1,1.0///:X with double left, double straight, double right",
				// Fully constrained cases (sum of feeding lanes == 1)
				"1,0,-90/0,1,0/0,1,180:2.0+1.0//:T with single left, single right",
				"1,0,-90/0,1,0/0,1,90/0,1,180:3.0+2.0+1.0///:X with single left, single straight, single right",
		};
		// TODO check that no unexpected connections were built
		double cx = 0;
		double cy = 0; 
		double cz = 0;
		for (String testJunction : testJunctions) {
			System.out.println("Running test " + testJunction.split(":")[2]);
			Junction junction = new Junction(testJunction.split(":")[0]);
			Network network = new Network();
			Node junctionNode = network.addNode ("junction", network.nextNodeID(), cx, cy, cz);
			int legCount = junction.legCount();
			Node[] otherNodes = new Node[legCount];
			Link[] incomingLinks = new Link[legCount];
			Link[] outgoingLinks = new Link[legCount];
			
			for (int legNo = 0; legNo < legCount; legNo++) {
				Junction.Leg leg = junction.getLeg(legNo);
				final double distance = 100;
				otherNodes[legNo] = network.addNode ("neighborNode" + legNo, network.nextNodeID(), round(cx + distance * Math.cos(Math.toRadians(leg.angle)), 3), round(cy + distance * Math.sin(Math.toRadians(leg.angle)), 3), cz);
				incomingLinks[legNo] = createLink(network, "feedLink" + legNo, otherNodes[legNo], junctionNode, leg.inLaneCount);
				outgoingLinks[legNo] = createLink(network, "exitLink" + legNo, junctionNode, otherNodes[legNo], leg.outLaneCount);
			}
			assertEquals("Network rebuild should succeed", Network.RebuildResult.SUCCESS, network.rebuild());
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			StaXWriter writer = null;
			try {
				writer = new StaXWriter(outputStream);
			} catch (Exception e) {
				fail("Caught unexpected exception in creation of the StaXWriter");
			}
			network.writeXML(writer);
			writer.close();
			String xmlText = outputStream.toString();
			System.out.println(xmlText);
			System.out.println("Test: " + testJunction);
			String expected = testJunction.split(":")[1];
			assertEquals("Test description error; number of exits mismatches", legCount, expected.split("/", -1).length);
			for (int legNo = 0; legNo < legCount; legNo++) {
				String connections = expected.split("/", -1)[legNo];
				String[] laneDestinations = connections.split(",", 0);
				if (connections.length() == 0)
					laneDestinations = new String[0];
				//System.out.println("laneDestinations.length is " + laneDestinations.length + ", inlaneCount is " + junction.getLeg(legNo).inLaneCount);
				assertEquals ("Test description error; number of lanes feeding junction does not match lane count", laneDestinations.length, junction.getLeg(legNo).inLaneCount);
				if (0 == laneDestinations.length)
					continue;
				System.out.println("Checking non-trivial incoming leg " + legNo);
				ArrayList<Lane> incomingLanes = incomingLinks[legNo].getCrossSections_r().get(0).collectLanes();
				ArrayList<Integer> connectionChecked = new ArrayList<Integer> ();
				//System.out.println("expected inLaneCount " + junction.getLeg(legNo).inLaneCount + " actual " + incomingLanes.size());
				assertEquals("incoming link has unexpected number of lanes", junction.getLeg(legNo).inLaneCount, incomingLanes.size());
				String[] subConnections = connections.split(",");
				for (int laneIndex = 0; laneIndex < subConnections.length; laneIndex++) {
					String subConnection = subConnections[laneIndex];
					System.out.println("checking subConnection \"" + subConnection + "\"");
					/*
					Lane incomingLane = incomingLanes.get(laneIndex);
					System.out.println("Incoming lane is " + incomingLane.toString());
					ArrayList<Lane> connectingLanes = incomingLane.getDownLanes_r();
					for (Lane l : connectingLanes) {
						System.out.print("Connecting lane " + l.toString() + " -> [");
						ArrayList<Lane> leavingLanes = l.getDownLanes_r();
						for (Lane l2 : leavingLanes)
							System.out.print(l2.toString() + " ");
						System.out.println("]");
						
					}
					*/
					for (String subSubConnection : subConnection.split("\\+")) {
						int outLinkNo = Integer.parseInt(subSubConnection.split("\\.")[0]);
						int outLaneNo = Integer.parseInt(subSubConnection.split("\\.")[1]);
						//System.out.println("outLinkNo " + outLinkNo + ", outLaneNo " + outLaneNo);
						for (Lane l : incomingLanes) {
							for (Lane connectingLane : l.getDownLanes_r()) {
								//System.out.println("Checking connectingLane " + connectingLane.toString());
								ArrayList<Lane> leavingLanes = connectingLane.getDownLanes_r();
								assertEquals("There should be exactly one down lane on a connectingLane (lane " + l.toString() + " has " + leavingLanes.toString() + ")" , 1, leavingLanes.size());
								for (Lane leavingLane : leavingLanes) {
									int destinationID = leavingLane.getCse().getCrossSection().getLink().getToNode_r().getNodeID();
									//System.out.println("actual destinationID " + destinationID + " expecting " + otherNodes[outLinkNo].getNodeID());
									if (otherNodes[outLinkNo].getNodeID() == destinationID) {
										int actualLaneIndex = connectingLane.getCse().getCrossSectionObjects(Lane.class).indexOf(connectingLane);
										//System.out.println("actual lane index is " + actualLaneIndex + " expecting " + outLaneNo);
										if (actualLaneIndex == outLaneNo) {
											connectionChecked.add(laneIndex);
											System.out.println(subSubConnection + " found in " + subConnection);
										}
									}
								}
							}
						}
					}
				}
				//System.out.println("connectionChecked contains " + connectionChecked.toString());
				for (int i = 0; i < subConnections.length; i++) {
					int expectedConnections = subConnections[i].split("\\+").length;
					int count = 0;
					for (int k = connectionChecked.size(); --k >= 0; )
						if (connectionChecked.get(k) == i)
							count++;
					assertEquals("Expected number of connecting lanes must match actual number for " + subConnections[i], expectedConnections, count);
				}
					
			}
			System.out.println("Test succeeded (generated connections match expected connections)\n");
		}
	}

	class Junction {
		public class Leg {
			final int inLaneCount;
			final int outLaneCount;
			final double angle;
			
			public Leg(int inLaneCount, int outLaneCount, double angle) {
				this.inLaneCount = inLaneCount;
				this.outLaneCount = outLaneCount;
				this.angle = angle;
			}
			
		}

		private ArrayList<Leg> legs = new ArrayList<Leg>();
		
		public Junction (String description) {
			for (String legString : description.split("/", -1)) {
				String fields[] = legString.split(",");
				legs.add(new Leg(Integer.parseInt(fields[0]), Integer.parseInt(fields[1]), Double.parseDouble(fields[2])));
			}
		}
		
		public Leg getLeg(int index) {
			return legs.get(index);
		}
		
		public int legCount() {
			return legs.size();
		}
	}
}