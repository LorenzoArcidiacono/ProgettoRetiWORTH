/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.interfaces;

import com.github.arci0066.worth.server.User;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NotifyEventInterface extends Remote {

    void notifyEvent(String value) throws RemoteException;



}
