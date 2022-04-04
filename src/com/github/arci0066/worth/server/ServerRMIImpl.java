/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.List;

public class ServerRMIImpl extends RemoteServer implements ServerRMI {

    private List<NotifyEventInterface> clients;
    private UsersList usersList;

    /* crea un nuovo servente */
    public ServerRMIImpl() throws RemoteException {
        super();
        usersList = UsersList.getSingletonInstance();
        clients = new ArrayList<>();
    }

    public synchronized void registerForCallback(NotifyEventInterface clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)) {
            clients.add(clientInterface);
            System.out.println("Nuovo client registrato per la callback.");

            String registeredUserList = usersList.jsonString();
            update(registeredUserList);
        }
    }


    @Override
    public synchronized void unregisterForCallback(NotifyEventInterface client) throws RemoteException {
        if (!clients.contains(client))
            return;
        if (clients.remove(client))
            System.out.println("Client deregistrato dalla callback");
        else
            System.err.println("Impossibile deregistrare.");
        //notifico in caso di chiusura di un client
        update(usersList.jsonString());
    }


    /* notifica di una variazione di valore dell'azione
/* quando viene richiamato, fa il callback a tutti i client
registrati */
    public synchronized void update(String value) throws RemoteException {
        doCallbacks(value);
    }

    private synchronized void doCallbacks(String value) throws
            RemoteException {
        //int numeroClienti = clients.size( );
        for (NotifyEventInterface client : clients) {
            client.notifyEvent(value);
        }
    }
// ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
