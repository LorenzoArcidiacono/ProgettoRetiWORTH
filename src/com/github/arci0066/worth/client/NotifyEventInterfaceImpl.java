/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
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
    Gson gson;

    public NotifyEventInterfaceImpl(List<String> userStatus) {
        super();
       gson = new Gson();
        this.userStatus = userStatus;
    }

    @Override
    public void notifyEvent(String value) throws RemoteException {
        List<String> userList = gson.fromJson(value, new TypeToken<List<String>>() {}.getType());
        synchronized (userStatus) {
            userStatus.clear();
            userStatus.addAll(userList);
        }
    }

}
