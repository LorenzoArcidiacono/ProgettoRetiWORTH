package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.interfaces.ServerInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Server implements ServerInterface {
    private ProjectsList projectsList;
    private UsersList registeredUsersList;


// ------ Constructors ------

    public Server() {
        projectsList = ProjectsList.getSingletonInstance();
        registeredUsersList = UsersList.getSingletonInstance();
    }


    // ------ Methods --------
// TODO: 17/01/21 Mancano controlli se utente è online
    /*
     * REQUIRES: Strings != null
     * EFFECTS: Registra un utente se il nickname non è già in uso, nel caso di esito positivo l'utente viene messo online
     * RETURN: OP_OK se è andata a buon fine
     *			|| EXISTING_USER se il nickname è già registrato
     *			|| OP_FAIL in caso di errore
     */
    @Override
    public ANSWER_CODE register(String userNickname, String userPassword) {
        if (userNickname == null || userPassword == null)
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) != null) {
            return ANSWER_CODE.EXISTING_USER;
        }
        registeredUsersList.add(new User(userNickname, userPassword));
        return ANSWER_CODE.OP_OK;
    }

    /*
     * REQUIRES: Strings != null, nickname già registrato, password corretta
     * EFFECTS: se l'utente è registrato viene segnato come online
     * RETURN:	OP_OK se è andata a buon fine
     *			|| UNKNOWN_USER se il nickname non è registrato,
     *			|| WRONG_PASSWORD se la password è sbagliata
     *			|| OP_FAIL in caso di errore
     */
    @Override
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
    @Override
    public void logout(String userNickname) {
        User usr = findUserByNickname(userNickname);
        if (usr.isOnline()) {
            usr.logout();
        }
        //Se non era online non faccio nulla.
    }

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti registrati
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
    @Override
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
    @Override
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
    @Override
    public ANSWER_CODE createProject(String projectTitle, String userNickname) {
        //Controllo Parametri
        if (isNull(projectTitle, userNickname)) {
            return ANSWER_CODE.OP_FAIL;
        }
        if (findUserByNickname(userNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }
        if (findProjectByTitle(projectTitle) != null) {
            return ANSWER_CODE.EXISTING_PROJECT;
        }

        projectsList.add(new Project(projectTitle, userNickname));
        return ANSWER_CODE.OP_OK;
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
    @Override
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
    @Override
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
    @Override
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
    @Override
    public Card showCard(String projectTitle, String cardTitle, String cardStatus, String userNickname) {
        if (isNull(projectTitle, cardTitle, userNickname)) {
            return null;
        }
        if (findUserByNickname(userNickname) == null) {
            return null;
        }
        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.getCard(cardTitle, cardStatus, userNickname);
        return null;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TODO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     *          || UNKNOWN_USER se l'utente non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    @Override
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
    @Override
    public ANSWER_CODE moveCard(String projectTitle, String cardTitle, String fromListTitle, String toListTitle, String userNickname) {
        if (isNull(projectTitle, cardTitle, fromListTitle, toListTitle, userNickname))
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) == null)
            return ANSWER_CODE.UNKNOWN_USER;

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null)
            return prj.moveCard(cardTitle, fromListTitle, toListTitle, userNickname);
        return ANSWER_CODE.UNKNOWN_PROJECT;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS:
     * RETURN: Restituisce la history della card in caso non ci siano problemi, null altrimenti.
     */
    @Override
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
     * REQUIRES: String != null && user registrato al progetto.
     * RETURN: La chat del progetto, null in caso di errore.
     */
    // TODO: 13/01/21 Restituire la chat
    @Override
    public String readChat(String projectTitle, String userNickname) {
        return null;
    }

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Scrive il messaggio nella chat del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    @Override
    public ANSWER_CODE sendChatMsg(String projectTitle, String message, String userNickname) {
        return ANSWER_CODE.OP_FAIL;
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
    @Override
    public ANSWER_CODE cancelProject(String projectTitle, String userNickname) {
        if (isNull(projectTitle, userNickname))
            return ANSWER_CODE.OP_FAIL;
        if (findUserByNickname(userNickname) == null) {
            return ANSWER_CODE.UNKNOWN_USER;
        }

        Project prj = findProjectByTitle(projectTitle);
        if (prj != null) {
            prj.cancelProject(userNickname);
            projectsList.remove(prj);
            return ANSWER_CODE.OP_OK;
        }
        return ANSWER_CODE.UNKNOWN_PROJECT;
    }

// ----- Private Methods -------

    private User findUserByNickname(String userNickname) {
        return registeredUsersList.findUser(userNickname);
    }

    private Project findProjectByTitle(String projectTitle) {
        return projectsList.findProject(projectTitle);
    }

    private boolean isNull(String... strings) {
        for (String str : strings) {
            if (str == null)
                return true;
        }
        return false;
    }
}
