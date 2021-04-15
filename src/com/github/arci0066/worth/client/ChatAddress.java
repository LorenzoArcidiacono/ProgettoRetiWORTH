package com.github.arci0066.worth.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class ChatAddress {

    private String projectTitle;
    private MulticastSocket ms;
    private InetAddress ia;
    private int port;

// ------ Constructors ------

    public ChatAddress(String projectTitle, String socketAddress) throws IOException {
        this.projectTitle = projectTitle;
        String[] str = socketAddress.split(":");
        System.out.println("chatAddress:"+socketAddress);
        System.err.println("ia:"+str[0]+" port:"+str[1]);
        ia = InetAddress.getByName(str[0]);
        port = Integer.parseInt(str[1]);
        ms = new MulticastSocket(port);
        ms.joinGroup(ia);
    }

// ------ Getters -------

    public String getProjectTitle() {
        return projectTitle;
    }

    public InetAddress getAddress() {
        return ia;
    }

    public int getPort() {
        return port;
    }

    public MulticastSocket getMulticastSocket() {
        return ms;
    }

    @Override
    public String toString() {
        return  "Project:" + projectTitle + " @ "+ia.toString()+":"+port;
    }
}
