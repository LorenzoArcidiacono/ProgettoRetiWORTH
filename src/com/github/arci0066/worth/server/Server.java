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
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.github.arci0066.worth.server.ServerSettings.serverBackupDirPath;

public class Server {

    //---------- MAIN ---------
    public static void main(String[] args) {
        ProjectsList projectsList;
        UsersList registeredUsersList;
        SocketList socketList;
        ThreadPoolExecutor pool;
        Thread leader;

        boolean exit;

        //Oggetti per la connessione
        final ServerSocket serverSocket;
        RemoteRegistration rmi;

        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        //Inizializza gli oggetti
        //projectsList = ProjectsList.getSingletonInstance();
        //registeredUsersList = UsersList.getSingletonInstance();
        socketList = SocketList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());

        //Creo la directory in cui salvo i progetti e, se esiste, leggo la lista degli utenti registrati
        Path path = Paths.get(serverBackupDirPath);
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(path);

        readServerBackup(path);

        //Lancio il thread Leader
        leader = new Leader(pool);
        leader.start();

        exit = false;
        //Apre la connessione
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                    ServerSettings.SERVER_PORT));
            System.out.println("Server: Aperta connessione: " + InetAddress.getLocalHost() + "," + ServerSettings.SERVER_PORT);
        } catch (UnknownHostException e) { // TODO: 24/01/21 sistemare return
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
//        Setto la RMI per la registrazione
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
                // TODO: 22/01/21 salvare tutto in memoria (forse dovrei farlo più spesso in Leader) chiudere connessioni
                pool.shutdown();
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Shutdown-thread"));


        //Ciclo principale
        while (!exit) {
            System.out.println("\nWaiting for clients");
            //Aspetta una connessione
            System.out.println("2 " + socketList);
            Socket client;
            try {
                client = serverSocket.accept();
                System.out.println("\nServer: new client:" + client.getRemoteSocketAddress());
                //server.update(gson.toJson(registeredUsersList)); // TODO: 27/01/21 andrebbe messo al momento della registrazione
                synchronized (socketList) {
                    socketList.add(client);
                }
                System.out.println(socketList);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void readServerBackup(Path path) {
        UsersList registeredUsersList;
        ProjectsList projectsList = null;

        //Leggo il backup della lista utenti registrati
        String usersData = readFile(path + "/Users.txt"); // TODO: 04/03/21 scrivere il nome del file in setting
        if (usersData != null) {
            String[] users = usersData.split(ServerSettings.usersDivider); //Divido i nomi e password dei singoli utenti e passo l'array al costruttore
            registeredUsersList = UsersList.getSingletonInstance(users);
        }

        //Leggo il backup dei progetti
        List<Path> result = null;
        try (Stream<Path> paths = Files.walk(Paths.get(serverBackupDirPath),1)) {
            result = paths.filter(Files::isDirectory)
                    .collect(Collectors.toList());
            result.remove(Paths.get(serverBackupDirPath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(result);
       /* for (Path proj: result) {
            String pName = proj.toString().replaceAll(serverBackupDirPath+"/","");
            System.out.println(pName);
        }*/
        projectsList = ProjectsList.getSingletonInstance(result);
        System.out.println(projectsList.getProjectsTitle());
    }

    // TODO: 04/03/21 string ha una dimensione massima, cambiare con String[]? per file lunghi 
    private static String readFile(String filePath) {
        Path path = Paths.get(filePath);
        String str = "";
        String line = "";
        try (BufferedReader reader = Files.newBufferedReader(path)){
            while ((line = reader.readLine()) != null)
                str += line;
        } catch (IOException e) {
            System.err.println("File " + filePath + " non esiste.");
            return null;
        }
        return str;
    }
}