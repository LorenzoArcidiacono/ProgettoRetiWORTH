package com.github.arci0066.worth.server;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ServerRMIImpl extends RemoteServer implements ServerRMI {

    private List<NotifyEventInterface> clients;

    /* crea un nuovo servente */
    public ServerRMIImpl() throws RemoteException {
        super();
        clients = new ArrayList<>();
    }

    public synchronized void registerForCallback (NotifyEventInterface clientInterface) throws RemoteException {
        if (!clients.contains(clientInterface)) {
            clients.add(clientInterface);
            System.out.println("New client registered for User List Callback.");
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
        Iterator i = clients.iterator( );
//int numeroClienti = clients.size( );
        while (i.hasNext()) {
            NotifyEventInterface client = (NotifyEventInterface) i.next();
            client.notifyEvent(value);
        }
        System.out.println("Callbacks complete.");
    }
// ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
    }
