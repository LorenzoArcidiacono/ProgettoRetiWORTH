package com.github.arci0066.worth.interfaces;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.server.Card;

public interface ProjectInterface {
    // ------ Getters -------
    /* SYNCHRONIZE:
     *   READ: title
     *   WRITE:
     */
    String getProjectTitle();

    /* SYNCHRONIZE:
     *   READ: projectUser
     *   WRITE:
     */
    String getProjectUsers(String userNickname);

    /*
     * REQUIRES: @params != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TO_DO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto
     * 			|| OP_FAIL in caso di errore.
     */
    /* SYNCHRONIZE:
     *   READ:  projectUser
     *   WRITE: cardList
     */
    ANSWER_CODE addCard(String cardTitle, String cardDescription, String userNickname);

    /*
     * REQUIRES: @params != null
     *   && cardTitle è in fromListTitle
     *   && toListTitle deve seguire il flusso di lavoro
     *   && userNick è registrato al progetto
     *
     * EFFECTS: Sposta la card nella lista selezionata
     *   && aggiorna la History della card
     *
     * RETURN: OP_OK in assenza di errori
     * 			|| UNKNOWN_CARD se la card non esiste nel progetto o nella lista scelta
     *			|| WRONG_LIST se lo spostamento non segue il flusso di lavoro,
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto,
     * 			|| OP_FAIL in caso di altro errore.
     * */
    /* SYNCHRONIZE:
     *   READ: projectUser
     *   WRITE: cardList, card
     */
    ANSWER_CODE moveCard(String cardTitle, String fromListTitle, String toListTitle, String userNickname);

    /*
     * REQUIRES: String != null
     *  && userNickname di un utente registrato (Controllato dal com.github.arci0066.worth.Server)
     *  && projectTitle di un progetto esistente  (Controllato dal com.github.arci0066.worth.Server)
     *  && user registrato al progetto.
     *
     * EFFECTS: Aggiunge l'utente alla lista degli utenti del progetto.
     *
     * RETURN: OP_OK in caso non ci siano stati errori
     *			|| PERMISSION_DENIED se l'utente oldUserNickname non è registrato al progetto,
     *			|| OP_FAIL altrimenti.
     */
    /* SYNCHRONIZE:
     *   READ:
     *   WRITE: projectUser
     */
    ANSWER_CODE addUser(String oldUserNickname, String newUserNickname);

    // TODO: 12/01/21 implementare
    /* SYNCHRONIZE:
     *   READ:
     *   WRITE: this
     */
    ANSWER_CODE cancelProject(String userNickname);

    /*
     * EFFECTS: Crea una stringa con il titolo di ogni card in ogni lista e gli utenti del progetto.
     * RETURN: La stringa creata.
     */
    // TODO: 14/01/21 se non registrato non dovrebbe vedere la lista degli utenti
    /* SYNCHRONIZE:
     *   READ: this
     *   WRITE:
     */
    String prettyPrint(String userNickname);

    /* SYNCHRONIZE:
     *   READ: cardList, projectUser
     *   WRITE:
     */
    String showCards(String userNickname);

    /*
     * REQUIRES:
     * EFFECTS:
     * RETURN:
     */
    /* SYNCHRONIZE:
     *   READ: card, cardList, projectUser
     *   WRITE:
     */
    // TODO: 14/01/21 passare una copia? una stringa?
    Card getCard(String cardTitle, String cardStatus, String userNickname);

    /* SYNCHRONIZE:
     *   READ: card, cardList, projectUser
     *   WRITE:
     */
    String getCardHistory(String cardTitle, String cardStatus, String userNickname);
}
