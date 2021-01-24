package com.github.arci0066.worth.client;

import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.ServerSettings;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Thread {
    private Scanner scanner;
    private Message message;

    // ---- Connessione con il Server ----
    protected Socket clientSocket;
    BufferedReader readerIn;
    BufferedWriter writerOut;
    Gson gson;

    public Client() {
        scanner = new Scanner(System.in);
        clientSocket = new Socket();
        gson = new Gson();
    }

    public void run() {
        int operazione = -1;
        boolean exit = false;
        printWelcomeMenu();
        operazione = scegliOperazione();
        System.out.println("Scelto " + operazione);

        if (!exit) {
           if(!openConnection()){
               exit = true;
               System.out.println("Errore di connessione");
           }
        }
        if(!exit){
            System.out.println("Client: connesso al server.");

            switch (operazione) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> exit = true;
                default -> {
                    System.out.println("Scelta non valida.");
                    exit = true;
                }
            }

            while (!exit) {
                Message msg = null;
                printOperationMenu();
                operazione = scegliOperazione();
                switch (operazione) {
                    case 1 -> msg = login(); // TODO: 23/01/21 posso eliminarlo e se logout lo mando al menù prima;
                    case 2 -> logout();
                    case 3 -> listUsers();
                    case 4 -> listOnlineUsers();
                    case 5 -> listProjects();
                    case 6 -> createProject();
                    case 7 -> addMember();
                    case 8 -> showMember();
                    case 9 -> showProjectCards();
                    case 10 -> showCard();
                    case 11 -> addCard();
                    case 12 -> moveCard();
                    case 13 -> getCardHistory();
                    case 14, 15 -> {
                        System.err.println("Non supportata.");
                        break;
                    }
                    case 16 -> cancelProject();
                    case 17 -> exit = true;
                    default -> {
                        System.out.println("Scelta non valida.");
                        break;
                    }
                }
                if (msg != null) {
                    sendMessage(msg);
                    aspettaRispostaServer();
                }

            }
            try {
                System.out.println("Chiudo Socket");
                clientSocket.close();
                readerIn.close();
                writerOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean openConnection() {
        try { //Prova a connettersi al server.
            clientSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), ServerSettings.SERVER_PORT));
            readerIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writerOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (SocketException e) {
            System.out.println("Il Server ha chiuso la connessione o avvenuto un errore: " + e);
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("Il Server ha chiuso la connessione o  avvenuto un errore:" + e);
            return false;
        }
        return true;
    }

    private void sendMessage(Message msg) {
        try {
            writerOut.write(gson.toJson(msg));
            writerOut.write("\n");
            writerOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void aspettaRispostaServer() {
        String message="",read = "";
        try {
            while((message = readerIn.readLine()) != null) {
                read += message;
                break;
            }
            System.out.println("RED:"+read);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // -------- METODI PRIVATI ---------
    private void printWelcomeMenu() {
        System.out.println("Scegli operazione:\n 1. Registra Utente.\n 2. Login Utente.\n 3. Annulla e Esci.");
    }

    private int scegliOperazione() {
        System.out.print("Inserisci numero operazione e premi invio: ");
        return scanner.nextInt();
    }

    private void printOperationMenu() {
        System.out.println("Scegli operazione:" +
                "\n 1.  Login Utente," +
                "\n 2.  Logout Utente," +
                "\n 3.  Vedi Lista Utenti Registrati," +
                "\n 4.  Vedi Lista Utenti Online," +
                "\n 5.  Vedi Lista dei Progetti," +
                "\n 6.  Crea un Progetto," +
                "\n 7.  Aggiungi un Utente a un Progetto," +
                "\n 8.  Vedi Membri del Progetto," +
                "\n 9.  Vedi Cards del Progetto," +
                "\n 10. Vedi Informazioni di una Card," +
                "\n 11. Aggiungi una Card al progetto," +
                "\n 12. Sposta una Card in un'altra Lista," +
                "\n 13. Vedi la History della Card," +
                "\n 14. Leggi la Chat del Progetto," +
                "\n 15. Invia un Messaggio in Chat," +
                "\n 16. Cancella un Progetto," +
                "\n 17. Esci.");
    }

    //------ POSSIBILI OPERAZIONI ------

    private void register() {
    }

    private Message login() {
        return new Message("Pluto", "Pluto1", null, null, null, null);
    }

    private void logout() {
    }

    private void listUsers() {
    }

    private void listOnlineUsers() {
    }

    private void listProjects() {
    }

    private void createProject() {
    }

    private void addMember() {
    }

    private void showMember() {
    }

    private void showProjectCards() {
    }

    private void showCard() {
    }

    private void addCard() {
    }

    private void moveCard() {
    }

    private void getCardHistory() {
    }

    private void cancelProject() {
    }
}
