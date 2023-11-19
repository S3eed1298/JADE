package org.example;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import java.io.*;
import java.util.Base64;
import java.util.Hashtable;


public class LocationAgent extends Agent {
    public SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> cityMap;
    public String meetingPlace;

    protected void setup() {

        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());

        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("LocationAgent");
        serviceDescription.setName("MeetingLocationService");
        agentDescription.addServices(serviceDescription);

        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new LocationAgentBehaviour(this));
    }

    public Hashtable<String, Double> calculateShortestPathCost(SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> cityMap) {
        Hashtable<String, Double> hashtable = new Hashtable<>();
        String sourceVertex = this.getLocalName();
        ShortestPathAlgorithm<String, DefaultWeightedEdge> shortestPathAlgorithm = new DijkstraShortestPath<>(cityMap);

        ShortestPathAlgorithm.SingleSourcePaths<String, DefaultWeightedEdge> paths = shortestPathAlgorithm.getPaths(sourceVertex);

        for (String vertex : cityMap.vertexSet()) {
            if (vertex.equals(sourceVertex)){
                hashtable.put(sourceVertex,0.0);
                continue;
            }

            GraphPath<String, DefaultWeightedEdge> shortestPath = paths.getPath(vertex);

            if (shortestPath != null) {
                double pathCost = shortestPath.getWeight();
                hashtable.put(vertex, pathCost);
            } else {
                System.out.println("No path exists from " + sourceVertex + " to " + vertex);
            }
        }
        return hashtable;
    }
    private class LocationAgentBehaviour extends CyclicBehaviour {

        LocationAgent locationAgent;

        boolean graphReceived;

        Hashtable<String,Double> weights;

        public LocationAgentBehaviour(LocationAgent locationAgent) {
            this.locationAgent = locationAgent;
        }

        @Override
        public void action() {
            ACLMessage msg = locationAgent.receive();
            if (msg != null) {
                if (msg.getPerformative() == ACLMessage.PROPOSE) {
                    String content = msg.getContent();
                    byte[] decodedContent = Base64.getDecoder().decode(content);
                    try {
                        ByteArrayInputStream bais = new ByteArrayInputStream(decodedContent);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        cityMap = (SimpleDirectedWeightedGraph<String, DefaultWeightedEdge>) ois.readObject();
                        ois.close();
                        graphReceived = true;
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    if(graphReceived){
                        weights = calculateShortestPathCost(cityMap);
                    }
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    try {
                        reply.setContentObject(weights);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    send(reply);
                }else{
                    meetingPlace = msg.getContent();
                    if (meetingPlace.equals(locationAgent.getLocalName()))
                        System.out.println("I'm " + locationAgent.getLocalName() + " and all agent will meet at my place");
                    else
                        System.out.println("I'm " + locationAgent.getLocalName() + " and we will meet at " + meetingPlace);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.CONFIRM);
                    send(reply);
                }
            } else {
                block();
            }
        }
    }
}