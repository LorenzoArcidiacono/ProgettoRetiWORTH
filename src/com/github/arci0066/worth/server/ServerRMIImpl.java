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
            System.out.println("New client registered for User List Callback.");

            // TODO: 12/04/21 capire se va bene così, funziona
            String registeredUserList;
            synchronized (usersList) { //TODO capire se serve sincronizzare
                registeredUserList = usersList.jsonString(); // TODO: 09/04/21 Meglio così o json?
                System.err.println("ServerRMIImpl "+registeredUserList+"in teoria "+usersList.jsonString());
            }
            update(registeredUserList);
        }
    }


        @Override
        public void unregisterForCallback (NotifyEventInterface client) throws RemoteException {
            if (clients.remove(client))
                System.out.println("Client unregistered for callback");
            else
                System.out.println("Unable to unregister client");
        }


    /* notifica di una variazione di valore dell'azione
/* quando viene richiamato, fa il callback a tutti i client
registrati */
    public void update(String value) throws RemoteException {
        doCallbacks(value);
    }

    private synchronized void doCallbacks(String value) throws
            RemoteException {
        System.out.println("Starting callbacks.");
        //int numeroClienti = clients.size( );
        for (NotifyEventInterface client : clients) {
            client.notifyEvent(value);
        }
        System.out.println("Callbacks complete.");
    }
// ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
    }
