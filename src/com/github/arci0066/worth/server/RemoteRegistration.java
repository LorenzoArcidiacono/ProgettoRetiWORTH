package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;

// TODO: 22/04/21 tutta da rivedere
public class RemoteRegistration extends RemoteServer implements RemoteRegistrationInterface {
    private UsersList usersList;
    private ServerRMIImpl server;
    Registry registry;

    // ------ Constructors ------
    public RemoteRegistration(ServerRMIImpl server) throws RemoteException {
        usersList = UsersList.getSingletonInstance();
        this.server = server;
    }

    @Override
    public ANSWER_CODE register(String userNickname, String password) throws RemoteException {
        if (userNickname != null && password != null) {
            //Controllo che non ci sia un utente con lo stesso nickname
            if (usersList.findUser(userNickname) == null) {
                User user = new User(userNickname, password,false);
                user.login();
                usersList.add(user);
                return ANSWER_CODE.OP_OK;
            }
            System.err.println("Err: " + userNickname + "," + password);
            return ANSWER_CODE.EXISTING_USER;
        }
        return ANSWER_CODE.OP_FAIL;
    }


// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
