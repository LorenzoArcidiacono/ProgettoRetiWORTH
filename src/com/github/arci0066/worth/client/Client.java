package com.github.arci0066.worth.client;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.ServerSettings;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client extends Thread {
    private String password = "Pluto1";
    private String nickname = "Pluto";
    private Scanner scanner;    //Per leggere le richieste dell'utente

    // ---- Connessione con il Server ----
    protected Socket clientSocket;
    private BufferedReader readerIn;
    private BufferedWriter writerOut;
    private Gson gson;

    public Client() {
        scanner = new Scanner(System.in);
        scanner.useDelimiter(System.lineSeparator()); //Evita di lasciare un '\n' in sospeso
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
            if (!openConnection()) {
                exit = true;
                System.out.println("Errore di connessione");
            }
        }
        if (!exit) {
            System.out.println("Client: connesso al server.");

            switch (operazione) {
                case 1 -> register();
                case 2 -> login();
                case 3 -> exit = true;
                default -> {
                    System.out.println("Scelta non valida."); // TODO: 26/01/21 farlo riprovare 
                    exit = true;
                }
            }
            //aspettaRispostaServer();

            while (!exit) {
                if (clientSocket.isClosed()) {
                    System.err.println("Connessione chiusa");
                    break;
                }
                Message msg = null;
                printOperationMenu();
                operazione = scegliOperazione();
                switch (operazione) {
                    case 1 -> msg = login(); // TODO: 23/01/21 posso eliminarlo e se logout lo mando al menù prima;
                    case 2 -> msg = logout();
                    case 3 -> msg = listUsers();
                    case 4 -> msg = listOnlineUsers();
                    case 5 -> msg = listProjects();
                    case 6 -> msg = createProject();
                    case 7 -> msg = addMember();
                    case 8 -> msg = showMember();
                    case 9 -> msg = showProjectCards();
                    case 10 -> msg = showCard();
                    case 11 -> msg = addCard();
                    case 12 -> msg = moveCard();
                    case 13 -> msg = getCardHistory();
                    case 14, 15 -> {
                        System.err.println("Non supportata.");
                        break;
                    }
                    case 16 -> msg = cancelProject();
                    case 17 -> {
                        exit = true;
                        msg = closeConnection();
                    }
                    default -> {
                        System.out.println("Scelta non valida.");
                        break;
                    }
                }
                if (msg != null) {
                    sendMessage(msg);
                    if(!msg.getOperationCode().equals(OP_CODE.CLOSE_CONNECTION))
                    rispostaServer();
                }

            }
            try {
                System.out.println("Chiudo Socket");
                scanner.close();
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
            writerOut.write(gson.toJson(msg)+"\n");
            writerOut.write(ServerSettings.MESSAGE_TERMINATION_CODE+"\n");
            writerOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rispostaServer() {
        String message = "", read = "";
        boolean end = false;
                 // TODO: 26/01/21 Capire come gestire questo while
        try {
            while (!end && (message = readerIn.readLine()) != null) {
                if(!message.contains(ServerSettings.MESSAGE_TERMINATION_CODE)){
                    read += message;
                    System.out.println("Task leggo " + read);
                    System.out.println("Provo a uscire");
                    //read = read.replace("END","");
                }
                else
                    end = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message answer = gson.fromJson(read, Message.class);
        System.out.println("Server answer:" + answer);
        switch (answer.getOperationCode()) {
            case LOGIN, LOGOUT,CREATE_PROJECT, ADD_CARD, ADD_MEMBER, MOVE_CARD, CANCEL_PROJECT: {
                System.out.println("\n@> "+ answer.getAnswerCode()+"\n");
                break;
            }
            case LIST_USER, LIST_ONLINE_USER, LIST_PROJECTS, SHOW_CARD, SHOW_MEMBERS, SHOW_PROJECT_CARDS, GET_CARD_HISTORY: {
                System.out.println("\n@> "+answer.getAnswerCode()+"\n");
                if (answer.getAnswerCode().equals(ANSWER_CODE.OP_OK)){
                    System.out.println("@> "+ answer.getExtra()+"\n");
                }
                break;
            }
        }
    }


        // -------- METODI PRIVATI ---------
        private void printWelcomeMenu () {
            System.out.println("Scegli operazione:\n 1. Registra Utente.\n 2. Login Utente.\n 3. Annulla e Esci.");
        }

        private int scegliOperazione () {
            System.out.print("Inserisci numero operazione e premi invio: ");
            return scanner.nextInt();
        }

        private void printOperationMenu () {
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

        private void register () {
        }

        private Message login () {
            return new Message(nickname, password, OP_CODE.LOGIN, null, null, null);
        }

        private Message logout () {
            return new Message(nickname, null, OP_CODE.LOGOUT, null, null, null);
        }

        private Message listUsers () {
            return new Message(nickname, null, OP_CODE.LIST_USER, null, null, null);
        }

        private Message listOnlineUsers () {
            return new Message(nickname, null, OP_CODE.LIST_ONLINE_USER, null, null, null);
        }

        private Message listProjects () {
            return new Message(nickname, null, OP_CODE.LIST_PROJECTS, null, null, null);
        }

        private Message createProject () {
            String projectTitle;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            return new Message(nickname, null, OP_CODE.CREATE_PROJECT, projectTitle, null, null);
        }

        private Message addMember () {
            String projectTitle, user;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            System.out.print("Inserire il nome del utente da aggiungere al progetto:");
            user = scanner.next();
            return new Message(nickname, user, OP_CODE.ADD_MEMBER, projectTitle, null, null);
        }

        private Message showMember () {
            String projectTitle;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            return new Message(nickname, null, OP_CODE.SHOW_MEMBERS, projectTitle, null, null);
        }

        private Message showProjectCards () {
            String projectTitle;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            return new Message(nickname, null, OP_CODE.SHOW_PROJECT_CARDS, projectTitle, null, null);
        }

        private Message showCard () {
            String projectTitle, card;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            System.out.print("Inserire il nome della Card:");
            card = scanner.next();
            return new Message(nickname, null, OP_CODE.SHOW_CARD, projectTitle, card, null);
        }

        private Message addCard () {
            String projectTitle, card;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            System.out.print("Inserire il nome della Card:");
            card = scanner.next();
            return new Message(nickname, null, OP_CODE.ADD_CARD, projectTitle, card, null);
        }

        private Message moveCard () {
            String projectTitle, card, extra;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            System.out.print("Inserire il nome della Card:");
            card = scanner.next();
            System.out.print("Inserire lista di partenza:"); // TODO: 25/01/21 Migliorare scelta lista!
            extra = scanner.next();
            extra += "->";
            System.out.print("Inserire lista di destinazione:");
            extra += scanner.next();
            return new Message(nickname, extra, OP_CODE.MOVE_CARD, projectTitle, card, null);
        }

        private Message getCardHistory () {
            String projectTitle, card;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            System.out.print("Inserire il nome della Card:");
            card = scanner.next();
            return new Message(nickname, null, OP_CODE.GET_CARD_HISTORY, projectTitle, card, null);
        }

        private Message cancelProject () {
            String projectTitle;
            System.out.print("Inserire il nome del Progetto:");
            projectTitle = scanner.next();
            return new Message(nickname, null, OP_CODE.CANCEL_PROJECT, projectTitle, null, null);
        }

        // TODO: 26/01/21 Chiusura in caso di errore
        private Message closeConnection () {
            return new Message(nickname, null, OP_CODE.CLOSE_CONNECTION, null, null, null);
        }
    }
