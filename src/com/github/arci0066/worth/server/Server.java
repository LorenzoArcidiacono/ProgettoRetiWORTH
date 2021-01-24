package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.interfaces.ServerInterface;
import com.google.gson.Gson;

import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server {


    // ------ Constructors ------
    public static void main(String[] args) {
        ProjectsList projectsList;
        UsersList registeredUsersList;
        ThreadPoolExecutor pool;
        boolean exit;

        Gson gson;
        final ServerSocket serverSocket;


        projectsList = ProjectsList.getSingletonInstance();
        registeredUsersList = UsersList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        gson = new Gson();
        exit = false;

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

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("In Shutdown Hook");
                // TODO: 22/01/21 salvare tutto in memoria (forse dovrei farlo più spesso) chiudere connessioni
                pool.shutdown();
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Shutdown-thread"));

        while (!exit) {
            System.out.println("\nWaiting for clients");
            //Aspetto una connessione
            try (Socket client = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new
                         InputStreamReader(client.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new
                         OutputStreamWriter(client.getOutputStream()));) {
                System.out.println("\nServer: new client:" + client.getInetAddress());
                System.out.flush();
                System.out.println("Server Pronto a leggere.");

                int numMessages = 0;
                while (!client.isClosed()){
                    Message msg = null;
                    String message = "", read = "";
                    while ((message = reader.readLine()) != null) {
                        //message = reader.readLine();
                        System.out.println("\nClient sent: " + message + "\n");
                        System.out.println("SONO QUI");
                        read += message;
                        numMessages++;
                        //read = read.replace("\n", "");
                        System.out.println("\nServer read "+numMessages+": " + read + "\n");
                        msg = gson.fromJson(read, Message.class);
                        System.out.println(msg.toString());
                        break;
                    }
                    System.out.println("Invio risposta");
                    if(msg != null){
                        msg.setAnswer(ANSWER_CODE.OP_OK,null);
                        writer.write(gson.toJson(msg));
                        writer.write("\n");
                        writer.flush();
                    }
                    else { //Caso in cui l'utente abbia chiuso la comunicazione
                        client.close();
                        reader.close();
                        writer.close();
                    }
                    if(client.isClosed()){
                        System.out.println("Utente chiuso!");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();

            }
        }


    }

    /*public Server() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                System.out.println("In Shutdown Hook");
                // TODO: 22/01/21 salvare tutto in memoria (forse dovrei farlo più spesso) chiudere connessioni
                pool.shutdown();
                try {
                    fileWriter.flush();
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, "Shutdown-thread"));
        projectsList = ProjectsList.getSingletonInstance();
        registeredUsersList = UsersList.getSingletonInstance();
        pool = new ThreadPoolExecutor(ServerSettings.MIN_THREAD_NUMBER, ServerSettings.MAX_THREAD_NUMBER, ServerSettings.THREAD_KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        gson = new Gson();
        exit = false;

        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(InetAddress.getLocalHost(),
                    ServerSettings.SERVER_PORT));
            f = new FileWriter("Server.txt");
            fileWriter = new BufferedWriter(f);
            fileWriter.write("Server: Aperta connessione: " + InetAddress.getLocalHost() + "," + ServerSettings.SERVER_PORT);
            fileWriter.flush();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void run() {
        while (!exit) {
            try {
                fileWriter.write("\nWaiting for clients");
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String message = "", read = "";
            try (Socket client = serverSocket.accept();
                 BufferedReader reader = new BufferedReader(new
                         InputStreamReader(client.getInputStream()));
                 BufferedWriter writer = new BufferedWriter(new
                         OutputStreamWriter(client.getOutputStream()));) {
                fileWriter.write("\nServer: new client:" + client.getInetAddress());
                System.out.flush();
                boolean done = false;
                while (!message.contains("EOS")) {
                    message = reader.readLine();

                    fileWriter.write("\nClient sent: " + message + "\n");
                    read += message;
                    read = read.replace("EOS", "");
                    fileWriter.write("\nServer read: " + read + "\n");
                    Message msg = gson.fromJson(read, Message.class);
                    fileWriter.write(msg.toString());
                    fileWriter.flush();
                }
            } catch (IOException e) {
                System.out.println("Client closed connection or some error appeared");
            }
        }

    }*/


    /*public void reciveMessage(Message msg) {
        if (msg == null) {
            System.out.println("ERRORE: Messaggio == null");
        }
        pool.execute(new Task(msg));
        System.err.println(pool.getActiveCount() + " di: " + pool.getPoolSize());

    }*/

}
