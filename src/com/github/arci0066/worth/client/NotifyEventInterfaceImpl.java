package com.github.arci0066.worth.client;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;

import java.rmi.RemoteException;

public class NotifyEventInterfaceImpl implements NotifyEventInterface {

    public NotifyEventInterfaceImpl() {
        super();
    }

    @Override
    public void notifyEvent(String value) throws RemoteException {
        System.err.println("Ricevuto dal server: "+ value);
    }

// ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
