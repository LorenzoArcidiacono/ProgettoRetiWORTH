package com.github.arci0066.worth.client;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.server.UsersList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.rmi.RemoteException;

public class NotifyEventInterfaceImpl implements NotifyEventInterface {

    public NotifyEventInterfaceImpl() {
        super();
    }

    @Override
    public void notifyEvent(String value) throws RemoteException {
        // TODO: 09/04/21 stampare meglio la lista degli utenti
        Gson gson = new Gson();
        System.err.println("Ricevuto dal server: "+ value);
    }

// ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
