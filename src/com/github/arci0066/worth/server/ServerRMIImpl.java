package com.github.arci0066.worth.server;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;
import com.google.gson.Gson;

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

    public synchronized void registerForCallback (NotifyEventInterface clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)) {
            clients.add(clientInterface);
            System.out.println("Nuovo client registrato per la callback.");

            // TODO: 12/04/21 capire se va bene così, funziona
            String registeredUserList;
            synchronized (usersList) { //TODO capire se serve sincronizzare
                registeredUserList = usersList.jsonString(); // TODO: 09/04/21 Meglio così o json?
            }
            update(registeredUserList);
        }
    }


        @Override
        public void unregisterForCallback (NotifyEventInterface client) throws RemoteException {
            if (clients.remove(client))
                System.out.println("Client deregistrato dalla callback");
            else
                System.err.println("Unable to unregister client");
            //notifico in caso di chiusura di un client
            update(usersList.jsonString());
        }


    /* notifica di una variazione di valore dell'azione
/* quando viene richiamato, fa il callback a tutti i client
registrati */
    // TODO: 05/05/21 dovrebbe essere chiamata anche in caso di logout per notificare
    public void update(String value) throws RemoteException {
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
