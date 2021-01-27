package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;

public class RemoteRegistration extends RemoteServer implements RemoteRegistrationInterface {
    private UsersList usersList;

    // ------ Constructors ------
    public RemoteRegistration() throws RemoteException {
        usersList = UsersList.getSingletonInstance();
    }

    @Override
    public String register(String userNIckname, String password) throws RemoteException{
        System.out.println("REGISTER "+usersList.toString());
        if(userNIckname != null && password != null){
            if(usersList.findUser(userNIckname)==null) {
                User user = new User(userNIckname, password);
                user.login();
                usersList.add(user);

                return ANSWER_CODE.OP_OK.toString();
            }
        }
        System.err.println("Err: "+userNIckname+","+password);
        return ANSWER_CODE.OP_FAIL.toString();
    }



// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
