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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Thread.sleep;

public class Client {


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
    private static File inputFileTest;
    private static Scanner scanner;    //Per leggere le richieste da tastiera o da file di input

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
        userStatus = new ArrayList<>();

        //Uso CopyOnWriteArrayList perchè sia thread safe
        chatAddresses = new CopyOnWriteArrayList<>();
        chatMessages = new CopyOnWriteArrayList<>();

        //Primo menu per la scelta di come accedere
        int operazione = -1;
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
// TODO: 09/04/21 dovrei farlo dopo che si è registrato
                serverInterface = (ServerRMI) registry.lookup("SERVER");
                callbackObj = new NotifyEventInterfaceImpl();
                stub = (NotifyEventInterface) UnicastRemoteObject.exportObject(callbackObj, 0);
            } catch (AccessException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (NotBoundException e) {
                e.printStackTrace();
            }
        }

        switch (operazione) {
            case 1 -> check = register();
            case 2 -> message = login();
            default -> exit = true; // TODO: 27/01/21 riprovare
        }
        // TODO: 09/04/21 se esco qui non faccio pulizia!
        if (!check) { //registrazione non andata a buon fine
            System.err.println("È avvenuto un errore durante la registrazione.");
            exit = true;
        }
        if (!exit) { // TODO: 22/04/21 posso evitarlo?
            if (!openConnection()) { //Apre la connessione TCP verso il server
                exit = true;
                System.err.println("Errore di connessione.");
            }
            if (!exit && (message != null)) { //se l'op scelta è Login
                sendMessage(message);
                ANSWER_CODE answer_code = serverAnswer();
                if (answer_code != ANSWER_CODE.OP_OK) { //se il login non è andato a buon fine
                    exit = true;
                    System.err.println("Chiusura dovuta a errore di Login.");
                }
            }
        }
        if (!exit) { // Se la connessione è stata stabilita
            System.out.println("Client: connesso al server.");
            try { // mi registro per le future callback sullo stato degli utenti
                serverInterface.registerForCallback(stub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            //avvio il thread daemon che si occupa di leggere le chat dei progetti
            daemonChatSniffer();

            // Loop principale in cui scegliere le operazioni
            while (!exit) {
                if (clientSocket.isClosed()) {
                    System.err.println("Connessione chiusa");
                    break;
                }

                //Scelgo l'operazione e setta il messaggio da inviare al server di conseguenza
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
                    case 14 -> sendChatMessage();
                    case 15 -> reciveChatMessages();
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

                //Caso di operazione tramite messaggio TCP
                if (msg != null) {
                    sendMessage(msg);
                    //Nel caso debba aspettare una risposta dal server
                    if (!msg.getOperationCode().equals(OP_CODE.CLOSE_CONNECTION) && !msg.getOperationCode().equals(OP_CODE.LOGOUT))
                        serverAnswer();
                }

            }
            try {
                //Chiudo tutte le comunicazioni e i buffer, deregistro il client dalle callback
                System.out.println("Chiudo Socket");
                scanner.close();
                clientSocket.close();
                readerIn.close();
                writerOut.close();
                serverInterface.unregisterForCallback(stub);
                // TODO: 19/04/21 chiudere i socket delle chat 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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


    /*
     * REQUIRES: msg != null
     * EFFECTS: invia il messaggio sulla connessione TCP
    */
    private static void sendMessage(Message msg) {
        if (msg == null) {
            return;
            // TODO: 22/04/21 sollevare eccezione
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
        String message = "", read = "";
        boolean end = false;
        // TODO: 26/01/21 Capire come gestire questo while
        try {
            while (!end && (message = readerIn.readLine()) != null) {
                if (!message.contains(ServerSettings.MESSAGE_TERMINATION_CODE)) {
                    read += message;
                    //System.out.println("Task leggo " + read);
                    //System.out.println("Provo a uscire");
                    //read = read.replace("END","");
                } else
                    end = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Message answer = gson.fromJson(read, Message.class);
        //System.out.println("Server answer:" + answer);
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
            case GET_PRJ_CHAT: {
                System.out.println("\n@> chat ricevuta:" + answer.getExtra() + "\n");
                if (answer.getAnswerCode().equals(ANSWER_CODE.OP_OK)) {
                    if (answer.getExtra().equals("Utente non membro del progetto.")) // TODO: 15/04/21 sarebbe carino metterlo come stringa predefinita
                        System.out.println("@> " + answer.getExtra() + "\n");
                    else {
                        try {
                            //synchronized (chatAddresses) {
                                chatAddresses.add(new ChatAddress(answer.getProjectTitle(), answer.getExtra()));
                            //}
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
     * EFFECTS: legge il numero selezionato dall' utente
     * RETURN: il numero letto
    */
    private static int scegliOperazione() {
        System.out.print("Inserisci numero operazione e premi invio: ");
        return scanner.nextInt();
    }


    /*
     * EFFECTS: stampa il menu delle operazioni
    */
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
                "\n 14. Invia un Messaggio in una Chat," +
                "\n 15. Leggi la Chat del Progetto," +
                "\n 16. Cancella un Progetto," +
                "\n 17. Esci.");
    }

    //------ POSSIBILI OPERAZIONI RICHIESTE ------

    // TODO: 27/01/21 cambiare ritorno

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


    /*
     * EFFECTS: setta un messaggio per una richiesta di login
     * RETURN: il messaggio
    */
    private static Message login() {
        System.out.print("Username:");
        nickname = scanner.next();
        System.out.print("Password:");
        password = scanner.next();

        return new Message(nickname, password, OP_CODE.LOGIN, null, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per una richiesta di logout
     * RETURN: il messaggio
     */

    private static Message logout() {
        return new Message(nickname, null, OP_CODE.LOGOUT, null, null, null);
    }


    // TODO: 20/04/21 rendere questo un metodo locale in base a una struttura locale
    private static Message listUsers() {
        return new Message(nickname, null, OP_CODE.LIST_USER, null, null, null);
    }

    // TODO: 20/04/21 rendere questo un metodo locale in base a una struttura locale
    private static Message listOnlineUsers() {
        return new Message(nickname, null, OP_CODE.LIST_ONLINE_USER, null, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per richiedere la lista degli utenti
     * RETURN: il messaggio
     */
    private static Message listProjects() {
        return new Message(nickname, null, OP_CODE.LIST_PROJECTS, null, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per creare un nuovo progetto
     * RETURN: il messaggio
     */
    private static Message createProject() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.CREATE_PROJECT, projectTitle, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per aggiungere un membro a un progetto
     * RETURN: il messaggio
     */
    private static Message addMember() {
        String projectTitle, user;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome del utente da aggiungere al progetto:");
        user = scanner.next();
        return new Message(nickname, user, OP_CODE.ADD_MEMBER, projectTitle, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere i membri del progetto
     * RETURN: il messaggio
     */
    private static Message showMember() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_MEMBERS, projectTitle, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere le card del progetto
     * RETURN: il messaggio
     */
    private static Message showProjectCards() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.SHOW_PROJECT_CARDS, projectTitle, null, null);
    }

    /*
     * EFFECTS: setta un messaggio per vedere una card specifica
     * RETURN: il messaggio
     */
    private static Message showCard() {
        String projectTitle, card, extra;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        System.out.print("Inserire il nome della Card:");
        card = scanner.next();
        System.out.print("Inserire lista in cui si trova:"); // TODO: 25/01/21 Migliorare scelta lista!
        extra = scanner.next();
        return new Message(nickname, extra, OP_CODE.SHOW_CARD, projectTitle, card, null);
    }

    /*
     * EFFECTS: setta un messaggio per aggiungere una card
     * RETURN: il messaggio
     */
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

    /*
     * EFFECTS: setta un messaggio per spostare una card
     * RETURN: il messaggio
     */
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

    /*
     * EFFECTS: setta un messaggio per ricevere la history della card
     * RETURN: il messaggio
     */
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

    /*
     * EFFECTS: setta un messaggio per cancellare un progetto
     * RETURN: il messaggio
     */
    private static Message cancelProject() {
        String projectTitle;
        System.out.print("Inserire il nome del Progetto:");
        projectTitle = scanner.next();
        return new Message(nickname, null, OP_CODE.CANCEL_PROJECT, projectTitle, null, null);
    }

    /*
     * EFFECTS: se l'indirizzo della chat del progetto è già in memoria invia un messaggio sulla connessione UDP,
     *          altrimenti prima invia un messaggio per richiedere l'indirizzo (ricevendo anche la chat history) e poi invia il messaggio
     */
    private static void sendChatMessage() {
        String projectTitle, message;
        ANSWER_CODE response = ANSWER_CODE.OP_FAIL;
        ChatAddress chatAddress = null;
        byte[] data;
        DatagramPacket dp;
        DatagramSocket ds;

        System.out.print("Nome del Progetto: ");
        projectTitle = scanner.next();
        System.out.print("Messaggio: ");
        message = scanner.next();
        message = "@" + nickname + ": " + message;

        chatAddress = getProjectChatAddress(projectTitle);
        if (chatAddress == null) { //Caso in cui non abbia i riferimenti del progetto in memoria
            response = requestProjectChat(projectTitle);
            // TODO: 15/04/21 in realtà in ogni caso ritorna op_ok... dovrei cambiare questa cosa
            if (response != ANSWER_CODE.OP_OK) {
                System.err.println("@> " + response + ": messaggio non inviato.");
                return;
            }
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
    }

    /*
     * EFFECTS: se l'indirizzo della chat del progetto è già in memoria leggo la chat dalla memoria locale,
     *          altrimenti richiedo l'indirizzo della chat e ricevo anche i messaggi dal server.
     */
    private static void reciveChatMessages() {
        String projectTitle, message;
        ANSWER_CODE response;
        ChatAddress chatAddress;
        byte[] data;
        DatagramPacket dp;
        MulticastSocket ms;

        System.out.print("Nome del Progetto: ");
        projectTitle = scanner.next();

        //se non sono iscritto alla chat mi iscrivo
        chatAddress = getProjectChatAddress(projectTitle);
        if (chatAddress == null) { //Caso in cui non abbia i riferimenti del progetto in memoria
            requestProjectChat(projectTitle);
        }
        System.out.println(findProjectChat(projectTitle).getMessages());
    }

    // --------- OPERAZIONI PER LA CHAT ---------

    /*
     * EFFECTS: avvia un thread daemon che si occupa di ricevere i messaggi dalle chat dei progetti
     *          e salvarli in memoria.
    */
    private static void daemonChatSniffer() {
        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println("Daemon sniffer running");
                //per ogni progetto di cui il client fa parte chiede al server il chat address
                //per ogni progetto di cui fa parte chiede la lista dei messaggi vecchi e la salva

                //Scorre la lista dei Progetti di cui il client fa parte
                //per ogni chatAddress legge la lista dei messaggi con un timout dopo il quale va oltre
                //salva i messagi in un' oggetto condiviso
                int index = 0;
                byte[] data = new byte[1024];
                MulticastSocket ms;
                ChatAddress chat;
                boolean empty = false;

                while (true) {
                    //synchronized (chatAddresses) {
                        if (chatAddresses.isEmpty()) {
                            try { // TODO: 19/04/21 questo tiene chataddress occupato e fa un po' schifo
                                Thread.sleep(1000);
                                continue;
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    //}
                    //synchronized (chatAddresses) {
                        chat = chatAddresses.get(index);
                    //}
                    ms = chat.getMulticastSocket();
                    DatagramPacket dp = new DatagramPacket(data, data.length);
                    empty = false;
                    try {
                        ms.setSoTimeout(1000);
                        ms.receive(dp);
                    } catch (SocketTimeoutException | SocketException e) {
                        empty = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (!empty) {
                        String s = new String(dp.getData(), 0, dp.getLength());
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
    }

    // TODO: 20/04/21 mettere insieme chatMessages e chatAddresses 
    
    /*
     * REQUIRES: projectTitle != null
     * EFFECTS: cerca in memoria la chatHistory collegata a projectTitle
     * RETURN: la chat se presente, null altrimenti
    */
    private static ChatMessages findProjectChat(String projectTitle) {
        if (projectTitle == null) {
            return null;
            // TODO: 22/04/21 sollevare eccezione
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
            return null;
            // TODO: 22/04/21 sollevare eccezione
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
     * RETURN: la rispsorta del server.
    */
    private static ANSWER_CODE requestProjectChat(String projectTitle) {
        if (projectTitle == null) {
            return ANSWER_CODE.OP_FAIL;
            // TODO: 22/04/21 sollevare eccezione
        }
        ANSWER_CODE response;
        Message request = new Message(nickname, null, OP_CODE.GET_PRJ_CHAT, projectTitle, null, null);
        sendMessage(request);
        response = serverAnswer();  // TODO: 15/04/21 in rispostaServer() devo salvare il chat address
        if (response == ANSWER_CODE.OP_OK) { //se l'iscrizione è andata a buon fine chiedo i messaggi precedenti
            request = new Message(nickname, null, OP_CODE.GET_CHAT_HST, projectTitle, null, null);
            sendMessage(request);
            return serverAnswer();
        }
        return response;
    }


    // ------- CHIUSURA ------
    
    // TODO: 26/01/21 Chiusura in caso di errore

    /*
     * EFFECTS: setta un messaggio per chiudere la connessione col server.
     * RETURN: il messaggio
    */
    private static Message closeConnection() {
        return new Message(nickname, null, OP_CODE.CLOSE_CONNECTION, null, null, null);
    }

}
