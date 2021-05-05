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
    private List<String> userStatus; //struttura dati condivisa con il client

    public NotifyEventInterfaceImpl(List<String> userStatus) {
        super();
        this.userStatus = userStatus;
    }

    @Override
    public void notifyEvent(String value) throws RemoteException {
        // TODO: 09/04/21 stampare meglio la lista degli utenti
        Gson gson = new Gson();
        List<String> userList = gson.fromJson(value, new TypeToken<List<String>>() {}.getType());
        // TODO: 05/05/21 funziona ma costoso
        userStatus.clear();
        userStatus.addAll(userList);
        System.err.println("user status:"+ userStatus);
        System.err.println("Ricevuto dal server: "+ userList);
    }

}
