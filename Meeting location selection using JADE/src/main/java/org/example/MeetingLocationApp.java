package org.example;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class MeetingLocationApp {
    public static void main(String[] args) {
        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");


        Runtime runtime = Runtime.instance();

        try {
            AgentContainer container = runtime.createMainContainer(profile);

            String[] locations = agentsReader();
            for (int i = 1; i < locations.length; i++) {
                AgentController agentController = container.createNewAgent(locations[i], LocationAgent.class.getName(), null);
                agentController.start();
            }

            AgentController coordinatorController = container.createNewAgent("Coordinator", CoordinatorAgent.class.getName(), null);
            coordinatorController.start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static String[] agentsReader(){
        String[] matrix;
        try {
            Scanner sc = new Scanner(new File("C:\\Users\\s3eed\\Desktop\\Final Project\\projectAI\\src\\main\\resources\\Book1.csv"));
            sc.useDelimiter("\r\n");
            String line = sc.next();
            matrix = line.split(",");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        return matrix;
    }

}
