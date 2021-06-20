/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.interfaces;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteRegistrationInterface extends Remote {
    ANSWER_CODE register(String userNIckname, String password) throws RemoteException;
}
