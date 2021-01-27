package com.github.arci0066.worth.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistrationInterface extends Remote {
    String register(String userNIckname, String password) throws RemoteException;
}
