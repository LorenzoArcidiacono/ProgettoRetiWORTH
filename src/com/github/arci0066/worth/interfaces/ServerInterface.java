package com.github.arci0066.worth.interfaces;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.server.Card;

public interface ServerInterface {
    // ------ Methods --------
    // TODO: 17/01/21 Mancano controlli se utente è online
    /*
     * REQUIRES: Strings != null
     * EFFECTS: Registra un utente se il nickname non è già in uso, nel caso di esito positivo l'utente viene messo online
     * RETURN: OP_OK se è andata a buon fine
     *			|| EXISTING_USER se il nickname è già registrato
     *			|| OP_FAIL in caso di errore
     */
    /* Syncronized obj: registeredUserList
     *   READ:
     *   WRITE: registeredUserList
     */
    ANSWER_CODE register(String userNickname, String userPassword);

    /*
     * REQUIRES: Strings != null, nickname già registrato, password corretta
     * EFFECTS: se l'utente è registrato viene segnato come online
     * RETURN:	OP_OK se è andata a buon fine
     *			|| UNKNOWN_USER se il nickname non è registrato,
     *			|| WRONG_PASSWORD se la password è sbagliata
     *			|| OP_FAIL in caso di errore
     */
    /* Syncronized obj: user, userList
     *   READ: userList
     *   WRITE: user
     */
    ANSWER_CODE login(String userNickname, String userPassword);

    /*
     * REQUIRES: String != null
     * EFFECTS: Se il nickname è registrato e online viene settato com offline,
     */
    /* Syncronized obj: user, userList
     *   READ: userList
     *   WRITE: user
     */
    void logout(String userNickname);

    /*
     * REQUIRES:
     * EFFECTS: Restituisce la lista degli utenti registrati
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    /* Syncronized obj: userList
     *   READ: userList
     *   WRITE:
     */
    //Meglio se restituisse una List? No lui non deve manipolare nulla
    String listUsers();

    /*
     * EFFECTS: Restituisce la lista dei progetti di cui l'utente è membro
     * RETURN: Una stringa contenente i nomi dei progetti
     */
    /* Syncronized obj: projectList
     *   READ: projectList
     *   WRITE:
     */
    //Meglio se restituisse una List?
    String listProjects();

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
    /* Syncronized obj: projectList
     *   READ:
     *   WRITE: projectList
     */
    ANSWER_CODE createProject(String projectTitle, String userNickname);

    /*
     * REQUIRES: @params != null && userNickname di un utente registrato && projectTitle di un progetto esistente && user registrato al progetto.
     * EFFECTS: Aggiunge l'utente alla lista degli utenti del progetto (senza chiedere conferma all'utente)
     * RETURN: OP_OK in caso non ci siano stati errori
     *			|| UNKNOWN_PROJECT se projectTitle non è registrato
     *			|| UNKNOWN_USER se userNickname non corrisponde a un utente
     *          || PERMISSION_DENIED se l'utente oldUserNickname non è registrato al progetto
     *			|| OP_FAIL altrimenti.
     */
    /* Syncronized obj:
     *   READ: projectList, userList
     *   WRITE: project
     */
    ANSWER_CODE addMember(String projectTitle, String oldUserNickname, String newUserNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista degli utenti registrati al progetto
     * RETURN: Una stringa contenente i nickname degli utenti registrati
     */
    //Meglio se restituisse una List?
    /* Syncronized obj:
     *   READ: projectList, project, userList
     *   WRITE:
     */
    String showMembers(String projectTitle, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la lista delle com.github.arci0066.worth.extra.Card del progetto
     * RETURN: Una stringa contenente i titoli delle card divisi per status
     */
    /* Syncronized obj:
     *   READ: projectList, project, userList
     *   WRITE:
     */
    String showCards(String projectTitle, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Restituisce la card corrispondente a cardTitle in pojectTitle se questa esiste
     * RETURN: Una copia della card corrispondente se esiste, null altrimenti.
     */
    /* Syncronized obj:
     *   READ: projectList, project, userList
     *   WRITE:
     */
    Card showCard(String projectTitle, String cardTitle, String cardStatus, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TODO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     *          || UNKNOWN_USER se l'utente non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    /* Syncronized obj:
     *   READ: projectList, userList
     *   WRITE: project
     */
    ANSWER_CODE addCard(String projectTitle, String cardTitle, String cardDescription, String userNickname);

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
    /* Syncronized obj:
     *   READ: projectList, userList
     *   WRITE: project
     */
    ANSWER_CODE moveCard(String projectTitle, String cardTitle, String fromListTitle, String toListTitle, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS:
     * RETURN: Restituisce la history della card in caso non ci siano problemi, null altrimenti.
     */
    /* Syncronized obj:
     *   READ: projectList, project, userList
     *   WRITE:
     */
    String getCardHistory(String projectTitle, String cardTitle, String cardStatus, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * RETURN: La chat del progetto, null in caso di errore.
     */
    // TODO: 13/01/21 Restituire la chat
    String readChat(String projectTitle, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Scrive il messaggio nella chat del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di errore.
     */
    ANSWER_CODE sendChatMsg(String projectTitle, String message, String userNickname);

    /*
     * REQUIRES: String != null && user registrato al progetto.
     * EFFECTS: Cancella il progetto se tutte le card sono in Done
     * RETURN: OP_OK se op. a buon fine
     * 			|| UNKNOWN_PROJECT se il progetto non è registrato,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| PROJECT_NOT_FINISHED se esiste almeno una card non nella lista DONE
     * 			|| OP_FAIL in caso di errore.
     */
    /* Syncronized obj:
     *   READ: projectList, userList
     *   WRITE: project
     */
    ANSWER_CODE cancelProject(String projectTitle, String userNickname);
}
