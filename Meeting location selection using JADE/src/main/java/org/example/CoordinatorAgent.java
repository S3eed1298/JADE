package org.example;
import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;
import org.jgrapht.graph.*;

import java.io.*;
import java.util.Base64;
import java.util.Hashtable;
import java.util.Scanner;

public class CoordinatorAgent extends Agent{
    private SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> cityMap = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
    private Hashtable<String, Double> finalHashTable = new Hashtable<>();
    private AID[] locationAgents;

    protected void setup() {
        cityMap = createCityMap();
        DFAgentDescription agentDescription = new DFAgentDescription();
        agentDescription.setName(getAID());

        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("CoordinatorAgent");
        serviceDescription.setName("MeetingCoordinatorService");
        agentDescription.addServices(serviceDescription);

        try {
            DFService.register(this, agentDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }



        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("LocationAgent");
        template.addServices(sd);
        try
        {
            DFAgentDescription[] result = DFService.search(this, template);
            locationAgents = new AID[result.length];
            for (int i = 0; i < result.length; ++i)
            {
                locationAgents[i] = result[i].getName();
            }
        }
        catch (FIPAException fe)
        {
            fe.printStackTrace();
        }

        for(AID locationAgent : locationAgents){
            finalHashTable.put(locationAgent.getLocalName(),0.0);
        }

        addBehaviour(new CoordinatorAgentBehaviour(this));
    }

    private class CoordinatorAgentBehaviour extends CyclicBehaviour {
        CoordinatorAgent coordinatorAgent;
        private int step = 0;
        private int repliesCnt = 0;
        private MessageTemplate mt;
        private Hashtable<String, Double> weights;
        private String optimalMeetingPlace;


        public CoordinatorAgentBehaviour(CoordinatorAgent coordinatorAgent){this.coordinatorAgent = coordinatorAgent;}
        public void action() {
            switch (step) {
                case 0:
                    ACLMessage cfp = new ACLMessage(ACLMessage.PROPOSE);
                    for (int i = 0; i < locationAgents.length; ++i) {
                        cfp.addReceiver(locationAgents[i]);
                    }
                    try {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(baos);
                        oos.writeObject(cityMap);
                        oos.close();
                        cfp.setContent(Base64.getEncoder().encodeToString(baos.toByteArray()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    cfp.setConversationId("optimalPlace");
                    cfp.setReplyWith("optimalPlace" + System.currentTimeMillis()); //unique value
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("optimalPlace"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = coordinatorAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                            try {
                                weights = (Hashtable<String, Double>) reply.getContentObject();
                            } catch (UnreadableException e) {
                                throw new RuntimeException(e);
                            }
                            weightsCollector(weights);
                        }
                        repliesCnt++;
                        if (repliesCnt >= locationAgents.length) {

                            step = 2;
                        }
                    } else {
                        block(10000);
                    }

                    break;
                case 2:
                    ACLMessage optimalPlace = new ACLMessage(ACLMessage.INFORM);
                    optimalMeetingPlace = selectOptimalPlace();
                    for (int i = 0; i < locationAgents.length; ++i) {
                        optimalPlace.addReceiver(locationAgents[i]);
                    }
                    optimalPlace.setContent(optimalMeetingPlace);
                    optimalPlace.setConversationId("optimalPlace");
                    optimalPlace.setReplyWith("optimalPlace" + System.currentTimeMillis()); //unique value
                    myAgent.send(optimalPlace);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("optimalPlace"),
                            MessageTemplate.MatchInReplyTo(optimalPlace.getReplyWith()));

                    step = 3;
                    break;
                case 3 :{
                    ACLMessage optimalPlaceReply = coordinatorAgent.receive(mt);
                    if (optimalPlaceReply != null) {
                        if (optimalPlaceReply.getPerformative() == ACLMessage.CONFIRM) repliesCnt++;

                        if (repliesCnt >= locationAgents.length) {
                            System.out.println("All agent agreed to meet in the : " + optimalMeetingPlace + " place");
                        }
                        step = 4;
                    } else {
                        block(10000);
                    }
                }
            }
        }
    }

    private String selectOptimalPlace() {
        Double smallestValue = null;
        String smallestKey = null;

        for (String key : finalHashTable.keySet()) {
            Double value = finalHashTable.get(key);
            if (smallestValue == null || value < smallestValue) {
                smallestValue = value;
                smallestKey = key;
            }
        }
        return smallestKey;
    }

    private void weightsCollector( Hashtable<String, Double> weights){
        Hashtable<String, Double> combinedHashtable = new Hashtable<>();
        combinedHashtable.putAll(finalHashTable);

        for (String key : weights.keySet()) {
            double value = weights.get(key);
            if (combinedHashtable.containsKey(key)) {
                double combinedValue = combinedHashtable.get(key) + value;
                combinedHashtable.put(key, combinedValue);
            } else {
                combinedHashtable.put(key, value);
            }
        }
        finalHashTable = combinedHashtable;
    }

    private static String[][] data(){
        String[][] matrix;
        int size,i = 0,j = 0;
        try {
            Scanner sc = new Scanner(new File("C:\\Users\\s3eed\\Desktop\\Final Project\\projectAI\\src\\main\\resources\\Book1.csv"));
            sc.useDelimiter("\r\n");
            String line = sc.next();
            size = Character.getNumericValue(line.charAt(0));
            matrix = new String[size +1][size +1];
            while (sc.hasNext()){
                matrix[i] = line.split(",");
                i++;
                line = sc.next();
            }
            matrix[i] = line.split(",");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return matrix;
    }

    private static SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> createCityMap() {
        SimpleDirectedWeightedGraph<String, DefaultWeightedEdge> cityMap = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        String[][] matrix = data();
        String[] vertices = matrix[0];
        for (int i = 1; i < matrix[0].length; i++)
            cityMap.addVertex(matrix[0][i]);

        // Add weighted edges (distances) between locations
        for (int i = 1; i < matrix[1].length; i++) {
            for (int j = 1; j < matrix[i].length; j++) {
                if (!matrix[i][j].equals("0") && i != j) {
                   //System.out.println("the value is " + matrix[i][j] + " with the type of "+ matrix[i][j].getClass().getSimpleName());
                    DefaultWeightedEdge edge = cityMap.addEdge(vertices[i], vertices[j]);
                    cityMap.setEdgeWeight(edge, Double.parseDouble(matrix[i][j]));
                }
            }
        }
        return cityMap;
    }
}