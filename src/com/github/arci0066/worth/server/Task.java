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
        System.out.println("Messaggio ricevuto: " + message);
        ANSWER_CODE answer_code = ANSWER_CODE.OP_OK;
        String string = message.getExtra();
        switch (message.getOperationCode()) {
            case LOGIN: {
                answer_code = login(message.getSenderNickname(), message.getExtra());
                break;
            }
            case LOGOUT: {
                answer_code = logout(message.getSenderNickname());
                try {
                    connection.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
            case LIST_USER: {
                string = listUsers();
                break;
            }
            case LIST_ONLINE_USER: {
                string = listOnlineUsers();
                break;
            }
            case LIST_PROJECTS: {
                string = listProjects();
                break;
            }
            case CREATE_PROJECT: {
                answer_code = createProject(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case ADD_MEMBER: {
                answer_code = addMember(message.getProjectTitle(), message.getSenderNickname(), message.getExtra());
                break;
            }
            case ADD_CARD: {
                answer_code = addCard(message.getProjectTitle(), message.getCardTitle(), message.getExtra(), message.getSenderNickname());
                break;
            }
            case MOVE_CARD: {
                answer_code = moveCard(message.getProjectTitle(), message.getCardTitle(), message.getExtra(), message.getSenderNickname());
                break;
            }
            case SHOW_CARD: {
                string = showCard(message.getProjectTitle(), message.getCardTitle(), message.getExtra(), message.getSenderNickname());
                if (string == null) {
                    answer_code = ANSWER_CODE.OP_FAIL;
                }
                break;
            }
            case SHOW_MEMBERS: {
                string = showMembers(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case GET_CARD_HISTORY: {
                string = getCardHistory(message.getProjectTitle(), message.getCardTitle(), message.getExtra(), message.getSenderNickname());
                break;
            }
            case SHOW_PROJECT_CARDS: {
                string = showCards(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case CANCEL_PROJECT: {
                answer_code = cancelProject(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case GET_PRJ_CHAT: {
                string = getProjectChat(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case GET_CHAT_HST: {
                string = getChatHistory(message.getProjectTitle(), message.getSenderNickname());
                break;
            }
            case CLOSE_CONNECTION: {
                try {
                    connection.close();
                    registeredUsersList.findUser(message.getSenderNickname()).logout(); // TODO: 30/04/21 Se l'utente non esiste NullPointerException
                } catch (IOException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
            default: {
                answer_code = ANSWER_CODE.OP_FAIL;
                string = null;
            }
        }
        // se l'operazione richiede una risposta la invia
        // TODO: 22/04/21 in caso di extra restituisce sempre op_ok... dovrei cambiare e il metodo restituisce il messaggio
        if (message.getOperationCode() != OP_CODE.CLOSE_CONNECTION && message.getOperationCode() != OP_CODE.LOGOUT) {
            try {
                message.setAnswer(answer_code, string);
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
        connection.getWriter().write(gson.toJson(message) + "\n");
        connection.getWriter().write(ServerSettings.MESSAGE_TERMINATION_CODE + "\n");
        connection.getWriter().flush();
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
    public ANSWER_CODE login(String userNickname, String userPassword) {
        if (isNull(userNickname, userPassword)) {
            return ANSWER_CODE.OP_FAIL;
        }
        //Cerco l'utente
        User usr = findUserByNickname(userNickname);
        if (usr == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }
        //Controllo che le credenziali siano corrette e nel caso lo setto Online
        if (usr.checkCredential(userNickname, userPassword)) {
            usr.login();
            return ANSWER_CODE.OP_OK;
        }
        return ANSWER_CODE.WRONG_PASSWORD;
    }

    /*
     * REQUIRES: String != null
     * EFFECTS: Se il nickname è registrato e online viene settato com offline,
     */
    public ANSWER_CODE logout(String userNickname) {
        if (isNull(userNickname)) {
            return ANSWER_CODE.OP_FAIL;
        }
        //Cerco l'utente
        User usr = findUserByNickname(userNickname);
        if (usr == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }
        if (usr.isOnline()) {
            usr.logout();
        }
        //Se non era online non faccio nulla.
        return ANSWER_CODE.OP_OK;
    }

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti registrati
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
    public String listUsers() {
        return registeredUsersList.getUsersNickname();
    }

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti online al momento
     * RETURN: Una stringa contenente i nickname degli utenti online
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
    public String listOnlineUsers() {
        return registeredUsersList.getOnlineUsersNickname();
    }


    /*
     * EFFECTS: Restituisce la lista dei progetti di cui l'utente è membro
     * RETURN: Una stringa contenente i nomi dei progetti
     */
    //Meglio se restituisse una List?
    public String listProjects() {
        return projectsList.getProjectsTitle();
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
    public ANSWER_CODE createProject(String projectTitle, String userNickname) {
        //Controllo Parametri
        if (isNull(projectTitle, userNickname)) {
            return ANSWER_CODE.OP_FAIL;
        }
        if (findUserByNickname(userNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }
        if (findProjectByTitle(projectTitle) == null) {
            projectsList.add(projectTitle, userNickname);
            return ANSWER_CODE.OP_OK;
        }
        return ANSWER_CODE.EXISTING_PROJECT;
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
    public ANSWER_CODE addMember(String projectTitle, String oldUserNickname, String newUserNickname) {
        if (isNull(projectTitle, oldUserNickname, newUserNickname)) {
            return ANSWER_CODE.OP_FAIL;
        }
        if (findUserByNickname(newUserNickname) == null || findUserByNickname(oldUserNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            return ANSWER_CODE.UNKNOWN_PROJECT;
        }
        return prj.addUser(oldUserNickname, newUserNickname);
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista degli utenti registrati al progetto
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List?
    public String showMembers(String projectTitle, String userNickname) {
        if (isNull(projectTitle, userNickname)) {
            return "Errore nella richiesta.";
        }
        if (findUserByNickname(userNickname) == null) {
            return "L'utente non è registrato";
        }

        //findProjectByTitle è thread safe
        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            return "Progetto inesistente.";
        }
        //se si bloccasse qui e qualcuno aggiungesse un User? devo bloccare il progetto?  è solo una copia?
        return prj.getProjectUsers(userNickname);
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista delle com.github.arci0066.worth.extra.Card del progetto
     * RETURN: Una stringa contenente i titoli delle card divisi per status
     */
    public String showCards(String projectTitle, String userNickname) {
        if (isNull(projectTitle, userNickname)) {
            return "Errore nella richiesta.";
        }
        if (findUserByNickname(userNickname) == null) {
            return "L'utente non è registrato";
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj == null) {
            return "Progetto inesistente.";
        }

        return prj.showCards(userNickname);
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la card corrispondente a cardTitle in pojectTitle se questa esiste
     * RETURN: Una copia della card corrispondente se esiste, null altrimenti.
     */
    public String showCard(String projectTitle, String cardTitle, String cardStatus, String userNickname) {
        if (isNull(projectTitle, cardTitle, userNickname)) {
            return null;
        }
        if (findUserByNickname(userNickname) == null) {
            return null;
        }
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            Card card = prj.getCard(cardTitle, cardStatus, userNickname);
            if (card != null) {
                return card.toString();
            }
        }
        return null;
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
    public ANSWER_CODE addCard(String projectTitle, String cardTitle, String cardDescription, String userNickname) {
        if (isNull(projectTitle, cardTitle, cardDescription, userNickname))
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.addCard(cardTitle, cardDescription, userNickname);
        return ANSWER_CODE.UNKNOWN_PROJECT;
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
    public ANSWER_CODE moveCard(String projectTitle, String cardTitle, String extra, String userNickname) {
        if (isNull(projectTitle, cardTitle, extra, userNickname))
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) == null)
            return ANSWER_CODE.UNKNOWN_USER;
        String[] status = extra.split("->");  //Extra è una stringa tipo INPROGRESS->TOBEREVISED
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.moveCard(cardTitle, status[0], status[1], userNickname);
        return ANSWER_CODE.UNKNOWN_PROJECT;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS:
     * RETURN: Restituisce la history della card in caso non ci siano problemi, null altrimenti.
     */
    public String getCardHistory(String projectTitle, String cardTitle, String cardStatus, String userNickname) {
        if (isNull(projectTitle, cardTitle, cardStatus, userNickname)) {
            return null;
        }

        if (findUserByNickname(userNickname) == null) {
            return null;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.getCardHistory(cardTitle, cardStatus, userNickname);

        return null;
    }


    /*
     * REQUIRES: @params != null
     * RETURN: l' indirizzo della chat collegata al progetto projectTitle
     */
    private String getProjectChat(String projectTitle, String senderNickname) {
        if (isNull(projectTitle, senderNickname)) {
            return null;
        }
        if (findUserByNickname(senderNickname) == null) {
            return null;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.getChatAddress(senderNickname);

        return null;
    }


    /*
     * REQUIRES: @params != null
     * RETURN: la history della chat collegata al progetto projectTitle
     */
    private String getChatHistory(String projectTitle, String senderNickname) {
        if (isNull(projectTitle, senderNickname)) {
            return null;
        }
        if (findUserByNickname(senderNickname) == null) {
            return null;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.getChatHistory();

        return null;
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
    public ANSWER_CODE cancelProject(String projectTitle, String userNickname) {
        if (isNull(projectTitle, userNickname))
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) { // TODO: 03/05/21 pulire
            //ANSWER_CODE answer = prj.cancelProject(userNickname);
            //if (answer == ANSWER_CODE.OP_OK) {
                return projectsList.remove(prj,userNickname);
                //return ANSWER_CODE.OP_OK;
            //}
        }
        return ANSWER_CODE.UNKNOWN_PROJECT;
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
