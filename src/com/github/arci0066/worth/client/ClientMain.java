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
import com.github.arci0066.worth.enumeration.USER_STATUS;
import com.github.arci0066.worth.interfaces.NotifyEventInterface;
import com.github.arci0066.worth.interfaces.RemoteRegistrationInterface;
import com.github.arci0066.worth.interfaces.ServerRMI;
import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.ServerSettings;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;


public class ClientMain {

    // -------- DATI UTENTE ---------
    private static String password;
    private static String nickname;

    // ------- CHAT DEI PROGETTI --------
    private static List<ChatAddress> chatAddresses; //Lista degli indirizzi dei progetti
    private static List<ChatMessages> chatMessages; //Lista delle chat dei progetti

    // ---- CONNESSIONE CON IL SERVER ----
    private static Socket clientSocket;
    private static BufferedReader readerIn = null;
    private static BufferedWriter writerOut = null;

    // ------- MEMORIA UTENTI -------
    private static List<String> userStatus;

    // ----- OPERAZIONI REMOTE --------
    private static RemoteRegistrationInterface serverObj;
    private static Remote remote;
    private static Registry registry;
    private static ServerRMI serverInterface = null;
    private static NotifyEventInterface callbackObj;
    private static NotifyEventInterface stub = null;

    // ------- UTILITÀ ----------
    private static Gson gson;
    private static Scanner scanner;    //Per leggere le richieste da tastiera o da file di input
    static Thread daemon;

    public static void main(String[] args) {

        if (args.length > 0) {
            try { // se è specificato un file usa quello come input del client
                System.setIn(new FileInputStream(args[0]));
                System.out.println("Leggo input da:" + args[0]);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        scanner = new Scanner(System.in);
        scanner.useDelimiter(System.lineSeparator()); //Evita di lasciare un '\n' in sospeso
        clientSocket = new Socket();
        gson = new Gson();
        userStatus = new ArrayList<>();

        //Uso CopyOnWriteArrayList perchè sia thread safe
        chatAddresses = new CopyOnWriteArrayList<>();
        chatMessages = new CopyOnWriteArrayList<>();


        //Primo menu per la scelta di come accedere
        int operazione;
        boolean exit = false;
        printWelcomeMenu();
        boolean check = true;
        Message message = null;

        operazione = scegliOperazione();

        if (operazione == 1 || operazione == 2) { // se l' operazione richiesta è Registrazione o Login
            try {  //setto la RMI
                registry = LocateRegistry.getRegistry(ServerSettings.REGISTRY_PORT);
                remote = registry.lookup(ServerSettings.REGISRTY_OP_NAME);
                serverObj = (RemoteRegistrationInterface) remote;
                serverInterface = (ServerRMI) registry.lookup("SERVER");
                callbackObj = new NotifyEventInterfaceImpl(userStatus);
                stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }

        switch (operazione) {
            case 1 -> check = register();
            case 2 -> message = login();
            default -> exit = true;
        }

        if (!check) { //registrazione non andata a buon fine
            System.err.println("È avvenuto un errore durante la registrazione.");
            exit = true;
        }
        if (!exit) { //se l'op è login oppure mi sono appena registrato
            if (!openConnection()) { //Apre la connessione TCP verso il server
                exit = true;
                System.err.println("Errore di connessione.");
            }
            if (!exit && (message != null)) { //se l'op scelta è Login
                sendMessage(message);
                ANSWER_CODE answer_code = serverAnswer();
                if (answer_code != ANSWER_CODE.OP_OK) { //se il login non è andato a buon fine
                    exit = true;
                    System.err.println("ERRORE DI LOGIN, CHIUSURA.");
                }
            }
        }
        if (!exit) { // Se la connessione è stata stabilita e l'operazione è andata a buon fine
            System.out.println("Client: connesso al server.");
            try { // mi registro per le future callback sullo stato degli utenti
                serverInterface.registerForCallback(stub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //avvio il thread daemon che si occupa di leggere le chat dei progetti
            daemon = daemonChatSniffer();

            // Loop principale in cui scegliere le operazioni
            Message msg;
            while (!exit) {
                if (clientSocket.isClosed()) {
                    System.err.println("Connessione chiusa");
                    break;
                }

                //Scelgo l'operazione e setto il messaggio da inviare al server di conseguenza
                msg = null;
                printOperationMenu();
                operazione = scegliOperazione();
                switch (operazione) {
                    case 1 -> {
                        msg = logout();
                        exit = true;
                    }
                    case 2 -> listUsers();
                    case 3 -> listOnlineUsers();
                    case 4 -> msg = listProjects();
                    case 5 -> msg = createProject();
                    case 6 -> msg = addMember();
                    case 7 -> msg = showMember();
                    case 8 -> msg = showProjectCards();
                    case 9 -> msg = showCard();
                    case 10 -> msg = addCard();
                    case 11 -> msg = moveCard();
                    case 12 -> msg = getCardHistory();
                    case 13 -> sendChatMessage();
                    case 14 -> receiveChatMessages();
                    case 15 -> msg = cancelProject();
                    default -> System.out.println("\n@> Scelta non valida.\n");
                }

                //Caso di operazione tramite messaggio TCP
                if (msg != null) {
                    sendMessage(msg);
                    //Nel caso debba aspettare una risposta dal server
                    if (!msg.getOperationCode().equals(OP_CODE.CLOSE_CONNECTION) && !msg.getOperationCode().equals(OP_CODE.LOGOUT))
                        serverAnswer();
                }

            }
        }
        closeEverything();
        System.out.println("Esco dal programma.");
    }


    //--------- CONNESSIONE COL SERVER ---------

    /*
     * EFFECTS: Apre una connessione TCP con il server e alloca i buffer di lettura e scrittura della connessione
     * RETURN: true se la connessione è stata aperta, false altrimenti.
     */
    private static boolean openConnection() {
        try { //Prova a connettersi al server.
            clientSocket.connect(new InetSocketAddress(InetAddress.getLocalHost(), ServerSettings.SERVER_PORT));
            readerIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            writerOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        } catch (SocketException e) {
            System.out.println("Il Server ha chiuso la connessione o è avvenuto un errore: " + e);
            return false;
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            System.out.println("Il Server ha chiuso la connessione o  è avvenuto un errore:" + e);
            return false;
        }
        return true;
    }


    /*
     * REQUIRES: msg != null
     * EFFECTS: invia il messaggio sulla connessione TCP
     */
    private static void sendMessage(Message msg) {
        if (msg == null) {
            System.err.println("Messaggio non inviato: msg == null");
            return;
        }
        try {
            writerOut.write(gson.toJson(msg) + "\n");
            writerOut.write(ServerSettings.MESSAGE_TERMINATION_CODE + "\n");
            writerOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /*
     * EFFECTS: Legge un messaggio sulla connessione con il server e gestisce eventuali dati ricevuti dal server
     * RETURN: l' ANSWER_CODE contenuto nella risposta
     */
    private static ANSWER_CODE serverAnswer() {
        String message, read = "";
        boolean end = false;
        try {
            while (!end && (message = readerIn.readLine()) != null) {
                if (!message.contains(ServerSettings.MESSAGE_TERMINATION_CODE)) {
                    read += message;
                } else
                    end = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message answer = gson.fromJson(read, Message.class);

        switch (answer.getOperationCode()) {
            case LIST_USER, LIST_ONLINE_USER, LIST_PROJECTS, SHOW_CARD, SHOW_MEMBERS, SHOW_PROJECT_CARDS, GET_CARD_HISTORY: {
                //Stampo la risposta all'operazione
                System.out.println("\n@> " + answer.getAnswerCode() + "\n");
                //Stampo le informazioni contenute in extra quando necessario
                if (answer.getAnswerCode().equals(ANSWER_CODE.OP_OK)) {
                    System.out.println("@> " + answer.getExtra() + "\n");
                }
                break;
            }
            case CREATE_PROJECT, CANCEL_PROJECT, ADD_CARD, ADD_MEMBER, MOVE_CARD: {
                //Stampo la risposta all'operazione
                System.out.println("\n@> " + answer.getAnswerCode() + "\n");
                break;
            }
            case GET_PRJ_CHAT: {
                if (answer.getAnswerCode().equals(ANSWER_CODE.OP_OK)) {
                    if (answer.getExtra().equals(ANSWER_CODE.PERMISSION_DENIED.toString())) {
                        System.out.println("@> " + answer.getExtra() + "\n");
                        return ANSWER_CODE.PERMISSION_DENIED;
                    } else {
                        try {
                            chatAddresses.add(new ChatAddress(answer.getProjectTitle(), answer.getExtra()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            }
            case GET_CHAT_HST: {
                ChatMessages cm = new ChatMessages(answer.getProjectTitle(), answer.getExtra());
                chatMessages.add(cm);
                break;
            }
        }
        return answer.getAnswerCode();
    }


    // -------- OPERAZIONI MENU ---------

    /*
     * EFFECTS: stampa il menu iniziale
     */
    private static void printWelcomeMenu() {
        System.out.println("Scegli operazione:\n 1. Registra Utente.\n 2. Login Utente.\n 3. Annulla e Esci.");
    }


    /*
     * EFFECTS: legge il numero selezionato dall'utente
     * RETURN: il numero letto
     */
    private static int scegliOperazione() {
        System.out.print("Inserisci numero operazione e premi invio: ");
        int i;
        try {
            i = scanner.nextInt();
        } catch (InputMismatchException e) {
            scanner.next(); //pulisco l'input per evitare che rimangano dati compromessi da leggere
            i = -1;
        }
        return i;
    }


    /*
     * EFFECTS: stampa il menu delle operazioni
     */
    private static void printOperationMenu() {
        System.out.println("Scegli operazione:" +
                "\n 1.  Logout Utente," +
                "\n 2.  Vedi Lista Utenti Registrati," +
                "\n 3.  Vedi Lista Utenti Online," +
                "\n 4.  Vedi Lista dei Progetti," +
                "\n 5.  Crea un Progetto," +
                "\n 6.  Aggiungi un Utente a un Progetto," +
                "\n 7.  Vedi Membri del Progetto," +
                "\n 8.  Vedi Cards del Progetto," +
                "\n 9. Vedi Informazioni di una Card," +
                "\n 10. Aggiungi una Card al progetto," +
                "\n 11. Sposta una Card in un'altra Lista," +
                "\n 12. Vedi la History della Card," +
                "\n 13. Invia un Messaggio in una Chat," +
                "\n 14. Leggi la Chat del Progetto," +
                "\n 15. Cancella un Progetto,");
    }

    //------ POSSIBILI OPERAZIONI RICHIESTE ------


    /*
     * EFFECTS: Tramite RMI registra l' utente al server
     * RETURN: true in caso sia andata a buon fine, false altrimenti
     */
    private static boolean register() {
        System.out.print("Scegli uno Username:");
        nickname = scanner.next();
        System.out.print("Scegli una Password:");
        password = scanner.next();
        ANSWER_CODE answer_code = ANSWER_CODE.OP_FAIL;
        try {
            answer_code = serverObj.register(nickname, password);
            System.out.println("\n@> " + answer_code);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return answer_code.equals(ANSWER_CODE.OP_OK);
    }


    /*
     * EFFECTS: setta un messaggio per una richiesta di login
     * RETURN: il messaggio
     */
    private static Message login() {
        System.out.print("Username:");
        nickname = scanner.next();
        System.out.print("Password:");
        password = scanner.next();

        return new Message(nickname, password, OP_CODE.LOGIN, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per una richiesta di logout.
     * RETURN: il messaggio.
     */

    private static Message logout() {
        return new Message(nickname, null, OP_CODE.LOGOUT, null, null);
    }

    /*
     * EFFECTS: stampa la lista degli utenti registrati.
     */
    private static void listUsers() {
        synchronized (userStatus) {
            System.out.println("\n@> Utenti:\n" + userStatus);
        }
    }

    /*
     * EFFECTS: Stampa la lista degli utenti online.
     */
    private static void listOnlineUsers() {
        String online = "\n@> Utenti Online:\n";
        synchronized (userStatus) {
            for (String s : userStatus) {
                if (s.contains(USER_STATUS.ONLINE.toString())) {
                    online += s + "\n";
                }
            }
        }
        System.out.println(online);
    }

    /*
     * EFFECTS: setta un messaggio per richiedere la lista degli utenti
     * RETURN: il messaggio
     */
    private static Message listProjects() {
        return new Message(nickname, null, OP_CODE.LIST_PROJECTS, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per creare un nuovo progetto
     * RETURN: il messaggio
     */
    private static Message createProject() {
        String projectTitle;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();

        //Controllo che il titolo sia ammissibile
        if (projectTitle.equals(".") || projectTitle.equals("..") || projectTitle.equals("") || projectTitle.startsWith(".")) {
            System.err.println("Errore di titolo.");
            return null;
        }
        return new Message(nickname, null, OP_CODE.CREATE_PROJECT, projectTitle, null);
    }

    /*
     * EFFECTS: setta un messaggio per aggiungere un membro a un progetto
     * RETURN: il messaggio
     */
    private static Message addMember() {
        String projectTitle, user;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome dell' utente da aggiungere al progetto:");
        user = scanner.next();
        return new Message(nickname, user, OP_CODE.ADD_MEMBER, projectTitle, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere i membri del progetto
     * RETURN: il messaggio
     */
    private static Message showMember() {
        String projectTitle;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_MEMBERS, projectTitle, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere le card del progetto
     * RETURN: il messaggio
     */
    private static Message showProjectCards() {
        String projectTitle;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_PROJECT_CARDS, projectTitle, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere una card specifica
     * RETURN: il messaggio
     */
    private static Message showCard() {
        String projectTitle, card, extra;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("Inserire lista in cui si trova [todo, inprogress, toberevised, done]:");
        extra = scanner.next();
        return new Message(nickname, extra, OP_CODE.SHOW_CARD, projectTitle, card);
    }

    /*
     * EFFECTS: setta un messaggio per aggiungere una card
     * RETURN: il messaggio
     */
    private static Message addCard() {
        String projectTitle, card, desc;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("Inserire descrizione della Card:");
        desc = scanner.next();

        //Controllo che il titolo della card e della descrizione siano ammissibili
        if (card.equals(".") || card.equals("") || card.equals("..") || card.startsWith(".")
                || desc.equals(".") || desc.equals("") || desc.equals("..")) {
            System.err.println("Errore nel titolo o nella descrizione.");
            return null;
        }
        return new Message(nickname, desc, OP_CODE.ADD_CARD, projectTitle, card);
    }

    /*
     * EFFECTS: setta un messaggio per spostare una card
     * RETURN: il messaggio
     */
    private static Message moveCard() {
        String projectTitle, card, extra;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("Titolo lista di partenza [todo, inprogress, toberevised, done]:");
        extra = scanner.next();
        extra += "->";
        System.out.print("Titolo lista di destinazione [inprogress, toberevised, done]:");
        extra += scanner.next();
        return new Message(nickname, extra, OP_CODE.MOVE_CARD, projectTitle, card);
    }

    /*
     * EFFECTS: setta un messaggio per ricevere la history della card
     * RETURN: il messaggio
     */
    private static Message getCardHistory() {
        String projectTitle, card, list;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("In che lista si trova [todo, inprogress, toberevised, done]:");
        list = scanner.next();
        return new Message(nickname, list, OP_CODE.GET_CARD_HISTORY, projectTitle, card);
    }

    /*
     * EFFECTS: setta un messaggio per cancellare un progetto
     * RETURN: il messaggio
     */
    private static Message cancelProject() {
        String projectTitle;
        System.out.print("Inserire il Titolo del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.CANCEL_PROJECT, projectTitle, null);
    }

    /*
     * EFFECTS: se l'indirizzo della chat del progetto è già in memoria invia un messaggio sulla connessione UDP,
     *          altrimenti prima invia un messaggio per richiedere l'indirizzo (ricevendo anche la chat history) e poi invia il messaggio
     */
    private static void sendChatMessage() {
        String projectTitle, message;
        ANSWER_CODE response;
        ChatAddress chatAddress;
        byte[] data;
        DatagramPacket dp;
        DatagramSocket ds;

        System.out.print("Titolo del Progetto: ");
        projectTitle = scanner.next();
        System.out.print("Messaggio: ");
        message = scanner.next();
        message = "@" + nickname + ": " + message;
        //Cerca la chat tra quelle in memoria
        chatAddress = getProjectChatAddress(projectTitle);
        if (chatAddress == null) { //Caso in cui non abbia i riferimenti del progetto in memoria
            //Chiede i riferimenti della chat al server
            response = requestProjectChat(projectTitle);
            if (response != ANSWER_CODE.OP_OK) {
                System.out.println("@> " + response + ": Messaggio non inviato.\n");
                return;
            }
            //Seleziona l'indirizzo del progetto
            chatAddress = getProjectChatAddress(projectTitle);
        }
        data = message.getBytes();

        try {
            ds = new DatagramSocket();
            dp = new DatagramPacket(data, data.length, chatAddress.getAddress(), chatAddress.getPort());
            ds.send(dp);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("@> Messaggio inviato.\n");
    }

    /*
     * EFFECTS: se l'indirizzo della chat del progetto è già in memoria leggo la chat dalla memoria locale,
     *          altrimenti richiedo l'indirizzo della chat e ricevo anche i messaggi dal server.
     */
    private static void receiveChatMessages() {
        String projectTitle;
        ANSWER_CODE response = ANSWER_CODE.OP_OK;
        ChatAddress chatAddress;

        System.out.print("Titolo del Progetto: ");
        projectTitle = scanner.next();

        //Cerca la chat in memoria
        chatAddress = getProjectChatAddress(projectTitle);
        if (chatAddress == null) { //Caso in cui non abbia i riferimenti del progetto in memoria li chiede al server
            response = requestProjectChat(projectTitle);
        }
        if (response == ANSWER_CODE.OP_OK) { //Se ha ricevuto la chat del progetto
            System.out.println(findProjectChat(projectTitle).getMessages()+"\n");
        }
        else{
            System.out.println("@> " + response+"\n");
        }
    }

    // --------- OPERAZIONI PER LA CHAT ---------

    /*
     * EFFECTS: avvia un thread daemon che si occupa di ricevere i messaggi dalle chat dei progetti
     *          e salvarli in memoria.
     */
    private static Thread daemonChatSniffer() {
        Thread t = new Thread() {
            @Override
            public void run() {
                //per ogni progetto di cui il client fa parte chiede al server il chat address
                //per ogni progetto di cui fa parte chiede la lista dei messaggi vecchi e la salva

                //Scorre la lista dei Progetti di cui il client fa parte
                //per ogni chatAddress legge la lista dei messaggi con un timout dopo il quale va oltre
                //salva i messagi in un' oggetto condiviso
                int index = 0;
                byte[] data = new byte[1024];
                MulticastSocket ms;
                ChatAddress chat;
                boolean empty;

                while (!isInterrupted()) {
                    if (chatAddresses.isEmpty()) {
                        try {
                            //tra una richiesta e la successiva aspetta
                            Thread.sleep(1000);
                            //controlla che non sia stato interrotto
                            continue;
                        } catch (InterruptedException e) { //Caso in cui venga interrotto durante la sleep
                            return;
                        }
                    }
                    chat = chatAddresses.get(index);
                    ms = chat.getMulticastSocket();
                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    empty = false;
                    try {
                        //aspetta di ricevere i messaggi in coda se ce ne sono
                        ms.setSoTimeout(1000);
                        ms.receive(dp);
                    } catch (SocketTimeoutException | SocketException e) {
                        //caso in cui non ci siano messaggi in coda
                        empty = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!empty) {
                        String s = new String(dp.getData(), 0, dp.getLength());
                        //seleziona la chat del progetto
                        ChatMessages cm = findProjectChat(chat.getProjectTitle());
                        if (cm != null) {
                            cm.add(s);
                        } else
                            System.err.println(chat.getProjectTitle() + "non trovata.");
                    }
                    index = (index + 1) % chatAddresses.size();
                }
            }
        };
        t.setDaemon(true);
        t.start();
        return t;
    }


    /*
     * REQUIRES: projectTitle != null
     * EFFECTS: cerca in memoria la chatHistory collegata a projectTitle
     * RETURN: la chat se presente, null altrimenti
     */
    private static ChatMessages findProjectChat(String projectTitle) {
        if (projectTitle == null) {
            System.err.println("findProjectChat(): projectTitle == null");
            return null;
        }
        for (ChatMessages cm : chatMessages) {
            if (cm.getProjectTitle().equals(projectTitle))
                return cm;
        }
        return null;
    }

    /*
     * REQUIRES: projectTitle != null
     * EFFECTS: cerca in memoria l'indirizzo della chat collegata a projectTitle
     * RETURN: l'indirizzo se presente, null altrimenti
     */
    private static ChatAddress getProjectChatAddress(String projectTitle) {
        if (projectTitle == null) {
            System.err.println("getProjectChatAddress(): projectTitle == null");
            return null;
        }
        for (ChatAddress ca : chatAddresses) {
            if (ca.getProjectTitle().equals(projectTitle))
                return ca;
        }
        return null;
    }


    /*
     * REQUIRES: projectTitle != null
     * EFFECTS: invia un messaggio di richiesta al server per ricevere l'indirizzo della chat e la chat history relativa a projectTitle
     * RETURN: la risposta del server.
     */
    private static ANSWER_CODE requestProjectChat(String projectTitle) {
        if (projectTitle == null) {
            System.err.println("requestProjectChat() : projectTitle == null");
            return ANSWER_CODE.OP_FAIL;
        }
        ANSWER_CODE response;
        Message request = new Message(nickname, null, OP_CODE.GET_PRJ_CHAT, projectTitle, null);
        sendMessage(request);
        response = serverAnswer();
        if (response == ANSWER_CODE.OP_OK) { //se l'iscrizione è andata a buon fine chiedo i messaggi precedenti
            request = new Message(nickname, null, OP_CODE.GET_CHAT_HST, projectTitle, null);
            sendMessage(request);
            return serverAnswer();
        }
        return response;
    }
    //-------- CHIUSURA -----------
    private static void closeEverything() {
        try {
            System.out.println("Chiudo tutto.");
            //Chiudo tutte le comunicazioni e i buffer, deregistro il client dalle callback e chiudo il thread daemon
            scanner.close();
            clientSocket.close();

            if (daemon != null) daemon.interrupt();
            if (readerIn != null) readerIn.close();
            if (writerOut != null) writerOut.close();
            if (serverInterface != null) serverInterface.unregisterForCallback(stub);
            for (ChatAddress ca : chatAddresses) {
                System.out.println("chiudo");
                ca.getMulticastSocket().close();
            }

            //Chiude il Thread legato alla RMI
            UnicastRemoteObject.unexportObject(callbackObj, true);

            chatMessages.clear();
            userStatus.clear();
            serverObj = null;
            remote = null;
            registry = null;
            serverInterface = null;
            callbackObj = null;
            stub = null;
            gson = null;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
