/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.arci0066.worth.server.ServerSettings.*;

public class Server {

    //---------- MAIN ---------
    public static void main(String[] args) {
        SocketList socketList; //Lista dei descrittori delle connessioni dei client
        ThreadPoolExecutor pool; //pool di thread per gestire le richieste dei client
        Thread leader; //Thread per la gestione delle connessioni pronte a scrivere

        boolean exit = false; //Stabilisce se devo uscire dal ciclo principale

        //Oggetti per la connessione
        final ServerSocket serverSocket;
        RemoteRegistration rmi;


        //Inizializza gli oggetti
        socketList = SocketList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        //Crea la directory in cui salvo i progetti e, se esistono, leggo i file di backup
        Path path = Paths.get(serverBackupDirPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(path);

        //Se presenti legge i file di backup
        readServerBackup(path);

        //Lancia il thread Leader
        leader = new Leader(pool);
        leader.start();

        //Apre la connessione
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),ServerSettings.SERVER_PORT));
            System.out.println("Server: Aperta connessione: " + InetAddress.getLocalHost() + "," + ServerSettings.SERVER_PORT);
        } catch (UnknownHostException e) { // TODO: 24/01/21 sistemare return
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

//        Setta la RMI per la registrazione dei client
        ServerRMIImpl server = null;
        try {
            server = new ServerRMIImpl();
            LocateRegistry.createRegistry(ServerSettings.REGISTRY_PORT);
            Registry registry = LocateRegistry.getRegistry(ServerSettings.REGISTRY_PORT);
// TODO: 27/01/21 pulire il tutto e capire se posso effettivamente mandarlo nel RemoteRegistration 
            ServerRMI stub2 = (ServerRMI) UnicastRemoteObject.exportObject(server, 0);
            String name = "SERVER";
            registry.rebind(name, stub2);

            rmi = new RemoteRegistration(server);
            RemoteRegistrationInterface stub = (RemoteRegistrationInterface) UnicastRemoteObject.exportObject(rmi, 0);
            registry.rebind(ServerSettings.REGISRTY_OP_NAME, stub);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Prepara un Thread di pulizia da lanciare prima della chiusura
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("In Shutdown Hook");
                // TODO: 22/01/21 salvare tutto in memoria
                pool.shutdown();
                try {
                    // TODO: 22/04/21 vedere se c'è altro da chiudere 
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Shutdown-thread"));


        //Ciclo principale
        while (!exit) { // TODO: 09/04/21 implementare qualcosa che riconosca il messaggio di stop
            //Aspetta una connessione
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("\nServer: new client @ " + client.getRemoteSocketAddress());
                synchronized (socketList) { //aggiunge la connessione all' elenco
                    socketList.add(client);
                }
                System.out.println(socketList);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     * EFFECTS: Legge i file di backup sul disco e inizializza la lista degli utenti e dei progetti
     */
    private static void readServerBackup(Path path) { // TODO: 09/04/21 posso eliminare path
        List<User> registeredUsersList = null;
        UsersList usersList = null;

        List<Project> oldProjectsList = null;
        ProjectsList projectsList = null;

        //Leggo il backup della lista utenti registrati
        try (FileInputStream fis = new FileInputStream(usersBackupFile);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            registeredUsersList = (List<User>) in.readObject();
            usersList = UsersList.getSingletonInstance(registeredUsersList); //setto la lista utenti a partire dal backup
            System.out.println("Backup utenti trovati:" + usersList.getUsersNickname());
        } catch (FileNotFoundException e) {
            System.err.println("Nessun file di backup utenti trovato.");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }


        //Leggo il backup dei progetti
        try (FileInputStream fis = new FileInputStream(projectsBackupFile);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            oldProjectsList = (List<Project>) in.readObject();
            projectsList = ProjectsList.getSingletonInstance(oldProjectsList); //setto la lista progetti a partire dal backup
            System.out.println("Backup progetti trovati:" + projectsList.getProjectsTitle());
        } catch (FileNotFoundException e) {
            System.err.println("Nessun file di backup Progetti trovato.");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}