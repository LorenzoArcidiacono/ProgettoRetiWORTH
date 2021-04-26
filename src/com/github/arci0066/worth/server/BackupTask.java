package com.github.arci0066.worth.server;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

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
        projectsList.serialize();

//        Salva i progetti singolarmente in cartelle
        projectsList.saveAll();
    }
}
