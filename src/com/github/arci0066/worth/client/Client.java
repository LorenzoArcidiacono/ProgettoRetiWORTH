/*
*
* @Author Lorenzo Arcidiacono
* @Mail l.arcidiacono1@studenti.unipi.it
* @Matricola 534235
*
*/
package com.github.arci0066.worth.client;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;
import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.ServerSettings;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Scanner;

import static java.lang.Thread.sleep;

public class Client {
    private static BufferedReader readerIn = null;
    private static BufferedWriter writerOut = null;

    private static String password;
    private static String nickname;
    private static Scanner scanner;    //Per leggere le richieste dell'utente

    // ---- Connessione con il Server ----
    private static Socket clientSocket;


    // ----- Operazioni Remote --------
    private static RemoteRegistrationInterface serverObj;
    private static Remote remote;
    private static Registry r;
    private static ServerRMI serverInterface = null;
    private static NotifyEventInterface callbackObj;
    private static NotifyEventInterface stub = null;

    private static Gson gson;
    private static File inputFileTest;

    // TODO: 08/02/21 farlo diventare un main e pulire tutto!
    public static void main(String[] args) {

        if (args.length > 0) {
            try { // se è specificato un file usa quello come input del client
                System.setIn(new FileInputStream(new File(args[0])));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        scanner = new Scanner(System.in);
        scanner.useDelimiter(System.lineSeparator()); //Evita di lasciare un '\n' in sospeso
        clientSocket = new Socket();
        gson = new Gson();
// TODO: 09/04/21 devo allocare dopo aver capito cosa vuole fare per evitare errori
        try { //collego la RMI
            r = LocateRegistry.getRegistry(ServerSettings.REGISTRY_PORT);
            remote = r.lookup(ServerSettings.REGISRTY_OP_NAME);
            serverObj = (RemoteRegistrationInterface) remote;

            serverInterface = (ServerRMI) r.lookup("SERVER");
            callbackObj = new NotifyEventInterfaceImpl();
            stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);


        } catch (AccessException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
        //Primo menu per la scelta di come accedere
        int operazione = -1;
        boolean exit = false;
        printWelcomeMenu();
        operazione = scegliOperazione();
        boolean check = true;
        Message message = null;
        switch (operazione) {
            case 1 -> check = register();
            case 2 -> message = login();
            default -> exit = true; // TODO: 27/01/21 riprovare
        }
        // TODO: 09/04/21 se esco qui non faccio pulizia!
        if (!check) {//registrazione non andata a buon fine
            System.err.println("È avvenuto un errore durante la registrazione.");
            exit = true;
        }
        if (!exit) {
            if (!openConnection()) {
                exit = true;
                System.err.println("Errore di connessione.");
            }
            try {
                serverInterface.registerForCallback(stub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if(!exit && (message != null)) { //se l'op era di login
                sendMessage(message);
                ANSWER_CODE answer_code = rispostaServer();
                if(answer_code != ANSWER_CODE.OP_OK){ //se il login non è andato a buon fine
                    exit = true;
                    System.err.println("Chiusura dovuta a errore di Login.");
                }
            }
        }
        if (!exit) {
                System.out.println("Client: connesso al server.");

            // Loop principale in cui scegliere le operazioni
            while (!exit) {
                if (clientSocket.isClosed()) {
                    System.err.println("Connessione chiusa");
                    break;
                }
                Message msg = null;
                printOperationMenu();
                operazione = scegliOperazione();
                switch (operazione) {
                    case 1 -> msg = login(); // TODO: 23/01/21 posso eliminarlo e se logout lo mando al menù prima; chiede di nuovo Nick e pwd
                    case 2 -> {
                        msg = logout();
                        exit = true;
                    }
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
                    if (!msg.getOperationCode().equals(OP_CODE.CLOSE_CONNECTION) && !msg.getOperationCode().equals(OP_CODE.LOGOUT))
                        rispostaServer();
                }

            }
            try {
                System.out.println("Chiudo Socket");
                scanner.close();
                clientSocket.close();
                readerIn.close();
                writerOut.close();
                serverInterface.unregisterForCallback(stub);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Esco dal programma.");
        //Thread.currentThread().interrupt();
    }

    private static boolean openConnection() {
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

    private static void sendMessage(Message msg) {
        try {
            writerOut.write(gson.toJson(msg) + "\n");
            writerOut.write(ServerSettings.MESSAGE_TERMINATION_CODE + "\n");
            writerOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ANSWER_CODE rispostaServer() {
        String message = "", read = "";
        boolean end = false;
        // TODO: 26/01/21 Capire come gestire questo while
        try {
            while (!end && (message = readerIn.readLine()) != null) {
                if (!message.contains(ServerSettings.MESSAGE_TERMINATION_CODE)) {
                    read += message;
                    System.out.println("Task leggo " + read);
                    System.out.println("Provo a uscire");
                    //read = read.replace("END","");
                } else
                    end = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message answer = gson.fromJson(read, Message.class);
        System.out.println("Server answer:" + answer);
        switch (answer.getOperationCode()) {
            case LOGIN, LOGOUT, CREATE_PROJECT, ADD_CARD, ADD_MEMBER, MOVE_CARD, CANCEL_PROJECT: {
                System.out.println("\n@> " + answer.getAnswerCode() + "\n");
                break;
            }
            case LIST_USER, LIST_ONLINE_USER, LIST_PROJECTS, SHOW_CARD, SHOW_MEMBERS, SHOW_PROJECT_CARDS, GET_CARD_HISTORY: {
                System.out.println("\n@> " + answer.getAnswerCode() + "\n");
                if (answer.getAnswerCode().equals(ANSWER_CODE.OP_OK)) {
                    System.out.println("@> " + answer.getExtra() + "\n");
                }
                break;
            }
        }
        return answer.getAnswerCode();
    }


    // -------- METODI PRIVATI ---------
    private static void printWelcomeMenu() {
        System.out.println("Scegli operazione:\n 1. Registra Utente.\n 2. Login Utente.\n 3. Annulla e Esci.");
    }

    private static int scegliOperazione() {
        System.out.print("Inserisci numero operazione e premi invio: ");
        return scanner.nextInt();
    }

    private static void printOperationMenu() {
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

    // TODO: 27/01/21 cambiare ritorno
    private static boolean register() {
        System.out.print("Scegli uno Username:");
        nickname = scanner.next();
        System.out.print("Scegli una Password:");
        password = scanner.next();
        ANSWER_CODE answer_code = ANSWER_CODE.OP_FAIL;
        try {
            // TODO: 08/02/21 callback deve essere registrato dopo ma deve essere comunque inviato
            //serverInterface.registerForCallback(stub);
            answer_code = serverObj.register(nickname, password);
            System.err.println("Ricevuto " + answer_code);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (answer_code.equals(ANSWER_CODE.OP_OK)) {
            return true;
        }
        return false;
    }

    private static Message login() {
        System.out.print("Username:");
        nickname = scanner.next();
        System.out.print("Password:");
        password = scanner.next();

        return new Message(nickname, password, OP_CODE.LOGIN, null, null, null);
    }

    private static Message logout() {
        return new Message(nickname, null, OP_CODE.LOGOUT, null, null, null);
    }

    private static Message listUsers() {
        return new Message(nickname, null, OP_CODE.LIST_USER, null, null, null);
    }

    private static Message listOnlineUsers() {
        return new Message(nickname, null, OP_CODE.LIST_ONLINE_USER, null, null, null);
    }

    private static Message listProjects() {
        return new Message(nickname, null, OP_CODE.LIST_PROJECTS, null, null, null);
    }

    private static Message createProject() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.CREATE_PROJECT, projectTitle, null, null);
    }

    private static Message addMember() {
        String projectTitle, user;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome del utente da aggiungere al progetto:");
        user = scanner.next();
        return new Message(nickname, user, OP_CODE.ADD_MEMBER, projectTitle, null, null);
    }

    private static Message showMember() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_MEMBERS, projectTitle, null, null);
    }

    private static Message showProjectCards() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_PROJECT_CARDS, projectTitle, null, null);
    }

    private static Message showCard() {
        String projectTitle, card;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_CARD, projectTitle, card, null);
    }

    private static Message addCard() {
        String projectTitle, card, desc;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("Inserire Descrizione della Card:");
        desc = scanner.next();
        return new Message(nickname, desc, OP_CODE.ADD_CARD, projectTitle, card, null);
    }

    private static Message moveCard() {
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

    private static Message getCardHistory() {
        String projectTitle, card, list;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("In che lista si trova:");
        list = scanner.next();
        return new Message(nickname, list, OP_CODE.GET_CARD_HISTORY, projectTitle, card, null);
    }

    private static Message cancelProject() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.CANCEL_PROJECT, projectTitle, null, null);
    }

    // TODO: 26/01/21 Chiusura in caso di errore
    private static Message closeConnection() {
        return new Message(nickname, null, OP_CODE.CLOSE_CONNECTION, null, null, null);
    }
}
