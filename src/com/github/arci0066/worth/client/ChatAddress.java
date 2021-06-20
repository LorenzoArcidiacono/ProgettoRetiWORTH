/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
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
        ia = InetAddress.getByName(str[0]);
        port = Integer.parseInt(str[1]);
        ms = new MulticastSocket(port);
        ms.joinGroup(ia);
    }

// ------ Getters -------

    /*
    * RETURN: il titolo del progetto.
    */
    public String getProjectTitle() {
        return projectTitle;
    }

    /*
    * RETURN: l'indirizzo del progetto.
    */
    public InetAddress getAddress() {
        return ia;
    }

    /*
    * RETURN: la porta del progetto.
    */
    public int getPort() {
        return port;
    }

    /*
    * RETURN: il socket multicast del progetto.
    */
    public MulticastSocket getMulticastSocket() {
        return ms;
    }

    @Override
    public String toString() {
        return  "Project:" + projectTitle + " @ "+ia.toString()+":"+port;
    }
}
