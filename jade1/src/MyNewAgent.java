package jadelab1;


import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.domain.*;
import jade.domain.FIPAAgentManagement.*;

import javax.swing.*;
import java.net.*;
import java.io.*;

public class MyNewAgent extends Agent {
    protected void setup() {
        DFAgentDescription dfad = new DFAgentDescription();
        dfad.setName(getAID());

        ServiceDescription sd1 = new ServiceDescription();
        sd1.setType("answers");
        sd1.setName("myDict");
        dfad.addServices(sd1);

        try {
            DFService.register(this,dfad);
        } catch (FIPAException ex) {
            ex.printStackTrace();
        }

        addBehaviour(new MyNewAgentBehaviour(this));
    }

    protected void takeDown() {
        displayResponse("See you");
    }
    public void displayResponse(String message) {
        JOptionPane.showMessageDialog(null,message,"Message",JOptionPane.PLAIN_MESSAGE);
    }

    public void displayHtmlResponse(String html) {
        JTextPane tp = new JTextPane();
        JScrollPane js = new JScrollPane();
        js.getViewport().add(tp);
        JFrame jf = new JFrame();
        jf.getContentPane().add(js);
        jf.pack();
        jf.setSize(400,500);
        jf.setVisible(true);
        tp.setContentType("text/html");
        tp.setEditable(false);
        tp.setText(html);
    }
    public String makeRequest(String serviceName, String word) {
        StringBuffer response = new StringBuffer();
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            DataInputStream input;
            url = new URL("http://dict.org/bin/Dict");
            urlConn = url.openConnection();
            urlConn.setDoInput(true);
            urlConn.setDoOutput(true);
            urlConn.setUseCaches(false);
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            String content = "Form=Dict1&Strategy=*&Database=" + URLEncoder.encode(serviceName) + "&Query="
                    + URLEncoder.encode(word) + "&submit=Submit+query";
            // forth
            printout = new DataOutputStream(urlConn.getOutputStream());
            printout.writeBytes(content);
            printout.flush();
            printout.close();
            // back
            input = new DataInputStream(urlConn.getInputStream());
            String str;
            while (null != ((str = input.readLine()))) {
                response.append(str);
            }
            input.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return response.substring(response.indexOf("<hr>") + 4, response.lastIndexOf("<hr>"));
    }
}

class MyNewAgentBehaviour extends CyclicBehaviour {
    MyNewAgent agent;

    public MyNewAgentBehaviour(MyNewAgent agent) {
        this.agent = agent;
    }

    public void action() {
        MessageTemplate template = MessageTemplate.MatchOntology("myDict");
        ACLMessage message = agent.receive(template);
        if (message == null) {
            block();
        } else {
            String content = message.getContent();
            String response = "";
            try {
                response = agent.makeRequest("wn", content);
            } catch (NumberFormatException ex) {
                response = ex.getMessage();
            }
            agent.displayHtmlResponse(response);
        }
    }
}
