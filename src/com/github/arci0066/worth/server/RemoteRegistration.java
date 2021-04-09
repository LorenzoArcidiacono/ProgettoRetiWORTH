package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.RemoteServer;

public class RemoteRegistration extends RemoteServer implements RemoteRegistrationInterface {
    private UsersList usersList;
    private ServerRMIImpl server;
    Registry registry;
    private Gson gson;

    // ------ Constructors ------
    public RemoteRegistration(ServerRMIImpl server) throws RemoteException {
        usersList = UsersList.getSingletonInstance();
        this.server = server;
        //LocateRegistry.createRegistry(ServerSettings.REGISTRY_PORT);
        //registry = LocateRegistry.getRegistry(ServerSettings.REGISTRY_PORT);
        //ServerRMI stub2 = (ServerRMI) UnicastRemoteObject.exportObject (this.server,0);
        /*String name = "SERVER";
        registry.rebind (name, stub2);*/
        gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
    }

    @Override
    public ANSWER_CODE register(String userNIckname, String password) throws RemoteException {
        System.out.println("REGISTER " + usersList.toString());
        if (userNIckname != null && password != null) {
            if (usersList.findUser(userNIckname) == null) {
                User user = new User(userNIckname, password,false);
                user.login();
                usersList.add(user);
                String gsonUserList;
                synchronized (usersList) { //TODO capire se serve sincronizzare
                    gsonUserList = gson.toJson(usersList);
                    System.err.println(gsonUserList);
                }
                server.update(gsonUserList);
                return ANSWER_CODE.OP_OK;
            }
            System.err.println("Err: " + userNIckname + "," + password);
            return ANSWER_CODE.EXISTING_USER;
        }
        return ANSWER_CODE.OP_FAIL;
    }


// ------ Getters -------

// ------ Setters -------

// ------ Methods ------
}
