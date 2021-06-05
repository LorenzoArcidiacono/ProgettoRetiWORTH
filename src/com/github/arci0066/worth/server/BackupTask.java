package com.github.arci0066.worth.server;


public class BackupTask extends Thread {
    private ProjectsList projectsList;
    private UsersList usersList;

// ------ Constructors ------

    public BackupTask() {
        projectsList = ProjectsList.getSingletonInstance();
        usersList = UsersList.getSingletonInstance();
    }

    @Override
    public void run() {
        System.out.println("Backup in memoria.");
//        Serializzazioni
        usersList.serialize();
        // TODO: 03/06/21 posso levarlo
        //projectsList.serialize();

//        Salva i progetti singolarmente in cartelle
        projectsList.saveAll();
    }
}
