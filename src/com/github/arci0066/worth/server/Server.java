package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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

        //Inizializza gli oggetti
        projectsList = ProjectsList.getSingletonInstance();
        registeredUsersList = UsersList.getSingletonInstance();
        socketList = SocketList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
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
                synchronized (socketList) {
                    socketList.add(client);
                }
                System.out.println(socketList);

                /*int numMessages = 0; //Per stampare quanti messagi ha mandato un utente
                while (!client.isClosed()) {
                    Message msg = null;
                    String message, read = "";

                    while ((message = reader.readLine()) != null) { // TODO: 25/01/21 Capire se manda più messaggi che fare
                        read += message;
                        numMessages++;
                        msg = gson.fromJson(read, Message.class);
                        System.out.println("\n@ Client send " + numMessages + ": " + msg + "\n");
                        break;
                    }

                    if (msg != null) {
                        msg.setAnswer(ANSWER_CODE.OP_OK, null);
                        writer.write(gson.toJson(msg));
                        writer.write("\n");
                        writer.flush();
                    } else { //Caso in cui l'utente abbia chiuso la comunicazione
                        System.out.println("@ Utente: " + client.getRemoteSocketAddress() + " chiuso.");
                        client.close();
                        reader.close();
                        writer.close();
                    }*/
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}