package com.github.arci0066.worth.client;

import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.server.User;
import com.github.arci0066.worth.server.UsersList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class NotifyEventInterfaceImpl implements NotifyEventInterface {
    private List<String> userList;

    public NotifyEventInterfaceImpl() {
        super();
    }

    @Override
    public void notifyEvent(String value) throws RemoteException {
        // TODO: 09/04/21 stampare meglio la lista degli utenti
        Gson gson = new Gson();
        userList = gson.fromJson(value,new TypeToken<List<String>>(){}.getType());
        System.err.println("Ricevuto dal server: "+ userList);
    }

    // ------ Constructors ------

// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
