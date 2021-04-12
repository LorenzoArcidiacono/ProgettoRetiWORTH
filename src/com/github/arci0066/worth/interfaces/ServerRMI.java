package com.github.arci0066.worth.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ServerRMI extends Remote {
    void registerForCallback(NotifyEventInterface stub) throws RemoteException;

    void unregisterForCallback(NotifyEventInterface stub) throws RemoteException;
}
