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
//        Serializza la lista utenti
        usersList.serialize();

//        Salva i progetti singolarmente in cartelle
        projectsList.saveAll();
    }
}
