package com.github.arci0066.worth.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NotifyEventInterface extends Remote {

    void notifyEvent(String value) throws RemoteException;
}
