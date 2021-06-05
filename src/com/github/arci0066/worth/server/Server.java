/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;


import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.arci0066.worth.server.ServerSettings.*;

public class Server {

    //---------- MAIN ---------
    public static void main(String[] args) {
        final SocketList socketList; //Lista dei descrittori delle connessioni dei client
        ThreadPoolExecutor pool; //pool di thread per gestire le richieste dei client
        Thread leader; //Thread per la gestione delle connessioni pronte a scrivere

        boolean exit = false; //Stabilisce se devo uscire dal ciclo principale

        //Oggetti per la connessione
        final ServerSocket serverSocket;
        RemoteRegistration rmi;


        //Inizializza gli oggetti
        socketList = SocketList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        //Crea la directory in cui salvo i backup se non esiste
        // TODO: 12/05/21 spostare questo in readServerBackup()? 
        Path path = Paths.get(serverBackupDirPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Se presenti legge i file di backup,
        //in caso questi non esistano ancora UsersList e ProjectsList saranno inizializzati dalla prima chiamata a getSingleton()
        readServerBackup();

        //Lancia il thread Leader
        leader = new Leader(pool);
        leader.start();

        //Apre la connessione
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(), ServerSettings.SERVER_PORT));
            System.out.println("Aperta la connessione @ " + InetAddress.getLocalHost() + ":" + ServerSettings.SERVER_PORT);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

//        Setta la RMI per la registrazione dei client
        ServerRMIImpl server;
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
            return;
        }

        // Prepara un Thread di pulizia da lanciare prima della chiusura alla ricezione di un SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("Server: ricevuto segnale di interruzione.");

                //Avvio un backup del sistema
                BackupTask bt = new BackupTask();
                pool.execute(bt);
                pool.shutdown();
                try {
                    //aspetto che il backup ed eventuali altri task siano finiti
                    pool.awaitTermination(10,TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                //invio in un segnale di interruzione al leader
                leader.interrupt();
                try {
                    leader.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //chiudo il socket
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Shutdown-thread"));


        //Ciclo principale
        while (true) {
            //Aspetta una connessione
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("\nNuovo client @ " + client.getRemoteSocketAddress());
                synchronized (socketList) { //aggiunge la connessione all' elenco
                    socketList.add(client);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /*
     * EFFECTS: Legge i file di backup sul disco e inizializza la lista degli utenti e dei progetti
     */
    private static void readServerBackup() {
        List<User> registeredUsersList;
        UsersList usersList;

        List<Project> oldProjectsList;
        ProjectsList projectsList;

        //Leggo il backup della lista utenti registrati
        try (FileInputStream fis = new FileInputStream(usersBackupFile);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            registeredUsersList = (List<User>) in.readObject();
            usersList = UsersList.getSingletonInstance(registeredUsersList); //setto la lista utenti a partire dal backup
            System.out.println("Backup utenti trovato:" + usersList.getUsersNickname());
        } catch (FileNotFoundException e) {
            System.err.println("Nessun file di backup utenti trovato.");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }


        //Leggo il backup dei progetti
        /*try (FileInputStream fis = new FileInputStream(projectsBackupFile);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            oldProjectsList = (List<Project>) in.readObject();
            projectsList = ProjectsList.getSingletonInstance(oldProjectsList); //setto la lista progetti a partire dal backup
            System.out.println("Backup progetti trovato:" + projectsList.getProjectsTitle());
        } catch (FileNotFoundException e) {
            System.err.println("Nessun file di backup Progetti trovato.");
        } catch (IOException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }*/

        //Leggo il backup dei progetti
        List<Path> result = null;
        //Leggo i nomi delle cartelle
        try (Stream<Path> paths = Files.walk(Paths.get(serverBackupDirPath),1)) {
            result = paths.filter(Files::isDirectory)
                    .collect(Collectors.toList());
            result.remove(Paths.get(serverBackupDirPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Server -> progetti: "+result);
       /* for (Path proj: result) {
            String pName = proj.toString().replaceAll(serverBackupDirPath+"/","");
            System.out.println(pName);
        }*/
        projectsList = ProjectsList.getSingletonInstance(result);
        System.out.println("Server->PrjList: "+projectsList.getProjectsTitle());
    }
}