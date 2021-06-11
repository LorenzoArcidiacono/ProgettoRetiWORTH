/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.google.gson.Gson;

import java.io.IOException;
import java.net.SocketException;

public class Task extends Thread {
    // ----- COMUNICAZIONE -----
    Message message;
    Connection connection;

    // --- MEMORIA DATI -----
    private ProjectsList projectsList;
    private UsersList registeredUsersList;

    // ------- SERIALIZZAZIONE -----  
    Gson gson;

    // ------ Constructors ------
    public Task(Connection connection) {
        this.connection = connection;
        gson = new Gson();
        projectsList = ProjectsList.getSingletonInstance();
        registeredUsersList = UsersList.getSingletonInstance();
    }

    @Override
    public void run() {
        try {
            readMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        if (message == null) { //in caso di errore di lettura ritorna
            System.err.println("Errore ricezione messaggio.");
            return;
        }

        User user = findUserByNickname(message.getSenderNickname());
        if (user == null) {
            System.err.println("Task: Utente inesistente");
            message.setAnswer(ANSWER_CODE.OP_FAIL,null);
            try {
                sendAnswer();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // TODO: 10/06/21 LOGIN non viene più inviato
        if (!user.isOnline() && !message.getOperationCode().equals(OP_CODE.LOGIN)) { //in caso il mittente risulti offline
            message.setAnswer(ANSWER_CODE.USER_OFFLINE, null);
            // TODO: 10/06/21 in caso l'utente faccia il logout risulta offline prima che gli venga inviato un messaggio e stampa errore
            try {
                sendAnswer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Messaggio ricevuto: " + message);

        ANSWER_CODE answer_code = ANSWER_CODE.OP_OK;
        String string = message.getExtra();
        Message answer = null;

        switch (message.getOperationCode()) {
            case LOGIN: {
                message = login(message);
                break;
            }
            case LOGOUT: {
                logout(message.getSenderNickname());
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            /*case LIST_USER: {
                string = listUsers();
                break;
            }
            case LIST_ONLINE_USER: {
                string = listOnlineUsers();
                break;
            }*/
            case LIST_PROJECTS: {
                message = listProjects(message);
                break;
            }
            case CREATE_PROJECT: {
                message = createProject(message);
                break;
            }
            case ADD_MEMBER: {
                message = addMember(message);
                break;
            }
            case ADD_CARD: {
                message = addCard(message);
                break;
            }
            case MOVE_CARD: {
                message = moveCard(message);
                break;
            }
            case SHOW_CARD: {
                message = showCard(message);
                break;
            }
            case SHOW_MEMBERS: {
                message = showMembers(message);
                break;
            }
            case GET_CARD_HISTORY: {
                message = getCardHistory(message);
                break;
            }
            case SHOW_PROJECT_CARDS: {
                message = showCards(message);
                break;
            }
            case CANCEL_PROJECT: {
                message = cancelProject(message);
                break;
            }
            case GET_PRJ_CHAT: {
                message = getProjectChat(message); // TODO: 13/05/21 provare a levare message = ... e vedere se funziona 
                break;
            }
            case GET_CHAT_HST: {
                message = getChatHistory(message);
                break;
            }
            /*case CLOSE_CONNECTION: {
                try {
                    connection.close();
                    registeredUsersList.findUser(message.getSenderNickname()).logout(); // TODO: 30/04/21 Se l'utente non esiste NullPointerException
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }*/
            default: {
                message.setAnswer(ANSWER_CODE.OP_FAIL,null);
            }
        }
        // se l'operazione richiede una risposta la invia
        if ((message.getOperationCode() != OP_CODE.CLOSE_CONNECTION) && (message.getOperationCode() != OP_CODE.LOGOUT)) {
            try {
                //message.setAnswer(answer_code, string);
                sendAnswer();
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
        }
        //setta la connessione per una nuova op
        connection.setInUse(false);
    }


    /*
     * EFFECTS: Invia un messaggio in risposta alla operazione svolta
     */
    private void sendAnswer() throws IOException {
        try{
            connection.getWriter().write(gson.toJson(message) + "\n");
            connection.getWriter().write(ServerSettings.MESSAGE_TERMINATION_CODE + "\n");
            connection.getWriter().flush();
        }
        catch (SocketException e){
            e.printStackTrace();
        }
    }


    /*
     * EFFECTS: legge il messaggio inviato dal client
     */
    private void readMessage() throws IOException {
        String connectionMessage, read = "";
        boolean end = false;
        while (!end && (connectionMessage = connection.getReader().readLine()) != null) {
            if (!connectionMessage.contains(ServerSettings.MESSAGE_TERMINATION_CODE)) {
                read += connectionMessage;

                //read = read.replace("END","");
            } else
                end = true;
            //break;
        }
        message = gson.fromJson(read, Message.class);
    }

    // ------ Methods ------
    // TODO: 22/01/21 quando ritorno project e poi ci lavoro sopra è thread safe?
    // TODO: 22/01/21 leggere la lista degli utenti mi richiede più tempo di calcolo ma evita di prendere due lock nel caso l'utente non esista?
    /*
     * REQUIRES: Strings != null, nickname già registrato, password corretta
     * EFFECTS: se l'utente è registrato viene segnato come online
     * RETURN:	OP_OK se è andata a buon fine
     *			|| UNKNOWN_USER se il nickname non è registrato,
     *			|| WRONG_PASSWORD se la password è sbagliata
     *			|| OP_FAIL in caso di errore
     */
    public Message login(Message msg) {
        String userNickname = msg.getSenderNickname();
        String userPassword = msg.getExtra();

        if (isNull(userNickname, userPassword)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        //Cerco l'utente
        User usr = findUserByNickname(userNickname);
        if (usr == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }
        //Controllo che le credenziali siano corrette e nel caso lo setto Online
        if (usr.checkCredential(userNickname, userPassword)) {
            usr.login();
            msg.setAnswer(ANSWER_CODE.OP_OK, null);
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.WRONG_PASSWORD, null);
        return msg;
    }

    /*
     * REQUIRES: String != null
     * EFFECTS: Se il nickname è registrato e online viene settato com offline,
     */
    public void logout(String userNickname) {
        if (isNull(userNickname)) {
            return;
        }
        //Cerco l'utente
        User usr = findUserByNickname(userNickname);
        if (usr == null) {
            return;
        }
        if (usr.isOnline()) {
            usr.logout();
        }
    }

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti registrati
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
    /*public String listUsers() {
        return registeredUsersList.getUsersNickname();
    }*/

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti online al momento
     * RETURN: Una stringa contenente i nickname degli utenti online
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
 /*   public String listOnlineUsers() {
        return registeredUsersList.getOnlineUsersNickname();
    }*/


    /*
     * EFFECTS: Restituisce la lista dei progetti di cui l'utente è membro
     * RETURN: Una stringa contenente i nomi dei progetti
     */
    //Meglio se restituisse una List?
    public Message listProjects(Message msg) {
        String answer = projectsList.getProjectsTitle();
        msg.setAnswer(ANSWER_CODE.OP_OK, answer);
        return msg;
    }

    /*
     * REQUIRES: @params != null
     *      && UserNickName registrato
     *      && projectTitle non deve essere già in uso.
     * EFFECTS: Se il titolo non esiste già crea un progetto con quel titolo e aggiunge l'utente al progetto.
     * RETURN: OP_OK se l'op. ha successo
     *          || UNKNOWN_USER se l'utente non è registrato.
     *			|| EXISTING_PROJECT se il titolo è già in uso.
     *			|| OP_FAIL altrimenti.
     */
    public Message createProject(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String userNickname = msg.getSenderNickname();

        //Controllo Parametri
        if (isNull(projectTitle, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }
        if (findProjectByTitle(projectTitle) == null) {
            projectsList.add(projectTitle, userNickname);
            msg.setAnswer(ANSWER_CODE.OP_OK, null);
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.EXISTING_PROJECT, null);
        return msg;
    }

    /*
     * REQUIRES: @params != null && userNickname di un utente registrato && projectTitle di un progetto esistente && user registrato al progetto.
     * EFFECTS: Aggiunge l'utente alla lista degli utenti del progetto (senza chiedere conferma all'utente)
     * RETURN: OP_OK in caso non ci siano stati errori
     *			|| UNKNOWN_PROJECT se projectTitle non è registrato
     *			|| UNKNOWN_USER se userNickname non corrisponde a un utente
     *          || PERMISSION_DENIED se l'utente oldUserNickname non è registrato al progetto
     *			|| OP_FAIL altrimenti.
     */
    public Message addMember(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String oldUserNickname = msg.getSenderNickname();
        String newUserNickname = msg.getExtra();
        if (isNull(projectTitle, oldUserNickname, newUserNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(newUserNickname) == null || findUserByNickname(oldUserNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
            return msg;
        }
        msg.setAnswer(prj.addUser(oldUserNickname, newUserNickname), null);
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista degli utenti registrati al progetto
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List?
    public Message showMembers(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String userNickname = msg.getSenderNickname();
        if (isNull(projectTitle, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
            return msg;
        }
        //todo se si bloccasse qui e qualcuno aggiungesse un User? devo bloccare il progetto?  è solo una copia?
        msg.setAnswer(ANSWER_CODE.OP_OK, prj.getProjectUsers(userNickname));
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista delle com.github.arci0066.worth.extra.Card del progetto
     * RETURN: Una stringa contenente i titoli delle card divisi per status
     */
    public Message showCards(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String userNickname = msg.getSenderNickname();
        if (isNull(projectTitle, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
            return msg;
        }

        msg.setAnswer(ANSWER_CODE.OP_OK, prj.showCards(userNickname));
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la card corrispondente a cardTitle in pojectTitle se questa esiste
     * RETURN: Una copia della card corrispondente se esiste, null altrimenti.
     */
    public Message showCard(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String cardTitle = msg.getCardTitle();
        String cardStatus = msg.getExtra();
        String userNickname = msg.getSenderNickname();

        if (isNull(projectTitle, cardTitle, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            Card card = prj.getCard(cardTitle, cardStatus, userNickname);
            if (card != null) {
                msg.setAnswer(ANSWER_CODE.OP_OK, card.toString());
                return msg;
            }
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_CARD, null);
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TO_DO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     *          || UNKNOWN_USER se l'utente non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    public Message addCard(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String cardTitle = msg.getCardTitle();
        String cardDescription = msg.getExtra();
        String userNickname = msg.getSenderNickname();
        if (isNull(projectTitle, cardTitle, cardDescription, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            msg.setAnswer(prj.addCard(cardTitle, cardDescription, userNickname), null);
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Sposta la card da una lista alla successiva
     * RETURN: OP_OK se op. a buon fine,
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     * 			|| UNKNOWN_CARD se la card non esiste nel progetto,
     *          || UNKNOWN_USER se l'utente non è registrato,
     *			|| WRONG_LIST se il titolo della lista è sbagliato o se lo spostamento non segue il flusso di lavoro,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    public Message moveCard(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String cardTitle = msg.getCardTitle();
        String extra = msg.getExtra();
        String userNickname = message.getSenderNickname();
        if (isNull(projectTitle, cardTitle, extra, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }
        String[] status = extra.split("->");  //Extra è una stringa tipo INPROGRESS->TOBEREVISED
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            msg.setAnswer(prj.moveCard(cardTitle, status[0], status[1], userNickname), null);
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS:
     * RETURN: Restituisce la history della card in caso non ci siano problemi, null altrimenti.
     */
    public Message getCardHistory(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String cardTitle = msg.getCardTitle();
        String cardStatus = msg.getExtra();
        String userNickname = msg.getSenderNickname();
        if (isNull(projectTitle, cardTitle, cardStatus, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }

        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            msg.setAnswer(ANSWER_CODE.OP_OK, prj.getCardHistory(cardTitle, cardStatus, userNickname));
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }


    /*
     * REQUIRES: @params != null
     * RETURN: l' indirizzo della chat collegata al progetto projectTitle
     */
    private Message getProjectChat(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String senderNickname = msg.getSenderNickname();
        if (isNull(projectTitle, senderNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(senderNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            msg.setAnswer(ANSWER_CODE.OP_OK, prj.getChatAddress(senderNickname));
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }


    /*
     * REQUIRES: @params != null
     * RETURN: la history della chat collegata al progetto projectTitle
     */
    private Message getChatHistory(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String senderNickname = msg.getSenderNickname();
        if (isNull(projectTitle, senderNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(senderNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            msg.setAnswer(ANSWER_CODE.OP_OK, prj.getChatHistory());
            return msg;
        }

        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }


    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Cancella il progetto se tutte le card sono in Done
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| PROJECT_NOT_FINISHED se esiste almeno una card non nella lista DONE
     * 			|| OP_FAIL in caso di errore.
     */
    public Message cancelProject(Message msg) {
        String projectTitle = msg.getProjectTitle();
        String userNickname = msg.getSenderNickname();
        if (isNull(projectTitle, userNickname)) {
            msg.setAnswer(ANSWER_CODE.OP_FAIL, null);
            return msg;
        }
        if (findUserByNickname(userNickname) == null) {
            msg.setAnswer(ANSWER_CODE.UNKNOWN_USER, null);
            return msg;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) { // TODO: 03/05/21 pulire
            msg.setAnswer(projectsList.remove(prj, userNickname), null);
            return msg;
        }
        msg.setAnswer(ANSWER_CODE.UNKNOWN_PROJECT, null);
        return msg;
    }


    /*---------  Private Methods -----------*/


    /*
     * RETURN: true se tutte le stringhe sono diverse da null
     */
    private boolean isNull(String... strings) {
        for (String str : strings) {
            if (str == null) {
                System.err.println("ERROR: isNull returned TRUE.");
                return true;
            }
        }
        return false;
    }


    /*
     * REQUIRES: userNickname != null
     * RETURN: se esiste l'utente con nickname == userNickname, null altrimenti
     */
    // TODO: 17/05/21 potrei cambiare in find user in project, cercando subito di capire se l'utente è membro del progetto 
    private User findUserByNickname(String userNickname) {
        return registeredUsersList.findUser(userNickname);
    }

    /*
     * REQUIRES: projectTitle != null
     * RETURN: se esiste il progetto con titolo == projectTitle, null altrimenti
     */
    private Project findProjectByTitle(String projectTitle) {
        return projectsList.findProject(projectTitle);
    }
}
