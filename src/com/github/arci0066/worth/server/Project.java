package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

//CLASSE THREA SAFE
public class Project implements com.github.arci0066.worth.interfaces.ProjectInterface {
    private static final String UTENTE_ERRATO = "Utente non membro del progetto.";

    private String projectTitle;
    private List<Card> todoList, inProgresList, toBeRevisedList, doneList;
    private List<String> projectUsers;
    private ReadWriteLock lock;

    //Come la implemento?
    // TODO: 12/01/21  private Chat projectChat;


    // ------ Constructors ------
    public Project(String projectTitle, String userNickname) {
        this.projectTitle = projectTitle;
        projectUsers = new ArrayList<>();
        todoList = new ArrayList<>();
        inProgresList = new ArrayList<>();
        toBeRevisedList = new ArrayList<>();
        doneList = new ArrayList<>();
        projectUsers.add(userNickname);
        lock = new ReentrantReadWriteLock();
    }

    // ------ Getters -------
    @Override
    public String getProjectTitle() {
        String str;
        lock.readLock().lock();
        try {
            str = projectTitle;
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

    @Override
    public String getProjectUsers(String userNickname) {
        String str;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                str = projectUsers.toString();
            } finally {
                lock.readLock().unlock();
            }
            return str;
        }
        return UTENTE_ERRATO;
    }
// ------ Setters -------

// ------ Methods ------

    /*
     * REQUIRES: @params != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TO_DO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto
     * 			|| OP_FAIL in caso di errore.
     */
    @Override
    public ANSWER_CODE addCard(String cardTitle, String cardDescription, String userNickname) {
        if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        Card card = new Card(cardTitle, cardDescription, userNickname);
        lock.writeLock().lock();
        try {
            todoList.add(card);
        } finally {
            lock.writeLock().unlock();
        }
        return ANSWER_CODE.OP_OK;
    }

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
    @Override
    public ANSWER_CODE moveCard(String cardTitle, String fromListTitle, String toListTitle, String userNickname) {
        CARD_STATUS fromStatus = getStatus(fromListTitle);
        CARD_STATUS toStatus = getStatus(toListTitle);
        ANSWER_CODE answer;
        if (!checkStep(fromStatus, toStatus))
            return ANSWER_CODE.WRONG_LIST;
        else if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        else { // Se supera i controlli cerco la card nella lista.
            Card card = findCardInList(cardTitle, fromStatus); // In teoria potrebbe trovare la card nella lista DONE, questo non è possibile grazie al controllo sugli step.
            if (card == null) {
                return ANSWER_CODE.UNKNOWN_CARD;
            }

            lock.writeLock().lock();
            try{
                if (getList(fromStatus).remove(card)) { // Se è rimossa dalla lista e aggiunta alla lista successiva.
                    if (getList(toStatus).add(card)) {
                        answer = card.moveAndAdjournHistory(userNickname, toStatus); // Provo ad aggiornare la Card e ritorno.
                    } else { // In caso non sia riuscito ad aggiungerla alla lista successiva provo a ripristinare tutto.
                        getList(fromStatus).add(card);
                        answer = ANSWER_CODE.OP_FAIL;
                    }
                } else // In caso non sia riuscita a rimuoverla dalla lista.
                    answer = ANSWER_CODE.OP_FAIL;
            }
            finally{
                lock.writeLock().unlock();
            }
        }
        return answer;
    }

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
    @Override
    public ANSWER_CODE addUser(String oldUserNickname, String newUserNickname) {
        if (!isUserRegisteredToProject(oldUserNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        ANSWER_CODE answer;
        lock.writeLock().lock();
        try {
            if (!projectUsers.add(newUserNickname))
                answer = ANSWER_CODE.OP_FAIL;
            else
                answer = ANSWER_CODE.OP_OK;
        } catch (Exception e) {
            answer = ANSWER_CODE.OP_FAIL;
        }
        finally {
            lock.writeLock().unlock();
        }
        return answer;
    }

    // TODO: 12/01/21 implementare
    @Override
    public ANSWER_CODE cancelProject(String userNickname) {
        if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        projectTitle = null;
        emptyList(todoList);
        emptyList(inProgresList);
        emptyList(toBeRevisedList);
        emptyList(doneList);
        projectUsers.clear();
        projectUsers = null;
        return ANSWER_CODE.OP_OK;
    }


    /*
     * EFFECTS: Crea una stringa con il titolo di ogni card in ogni lista e gli utenti del progetto.
     * RETURN: La stringa creata.
     */
    // TODO: 14/01/21 se non registrato non dovrebbe vedere la lista degli utenti
    @Override
    public String prettyPrint(String userNickname) {
        return showCards(userNickname) +
                ",\n Utenti Registrati: " + projectUsers;
    }

    @Override
    public String showCards(String userNickname) {
        if (isUserRegisteredToProject(userNickname)) {
            return "Progetto: " + projectTitle +
                    ",\n Todo: " + todoList.toString() +
                    ",\n In Progres: " + inProgresList.toString() +
                    ",\n To Be Revised: " + toBeRevisedList.toString() +
                    ",\n Done: " + doneList.toString();
        }
        return UTENTE_ERRATO;
    }

    /*
     * REQUIRES:
     * EFFECTS:
     * RETURN:
     */
    // TODO: 14/01/21 passare una copia? una stringa?
    @Override
    public Card getCard(String cardTitle, String cardStatus, String userNickname) {
        if (isUserRegisteredToProject(userNickname))
            return findCardInList(cardTitle, getStatus(cardStatus));
        return null;
    }

    @Override
    public String getCardHistory(String cardTitle, String cardStatus, String userNickname) {
        return getCard(cardTitle, cardStatus, userNickname).getCardHistory();
    }
//    ------- Private Methods --------

    /*
     * EFFECTS: controlla che lo step scelto sia corretto per il flusso di lavoro.
     * RETURN: true se lo è, false altrimenti.
     */
    private boolean checkStep(CARD_STATUS fromStatus, CARD_STATUS toStatus) {
        if (fromStatus == CARD_STATUS.TODO && toStatus == CARD_STATUS.IN_PROGRESS)
            return true;
        else if (fromStatus == CARD_STATUS.IN_PROGRESS && (toStatus == CARD_STATUS.TO_BE_REVISED || toStatus == CARD_STATUS.DONE))
            return true;
        else if (fromStatus == CARD_STATUS.TO_BE_REVISED && (toStatus == CARD_STATUS.IN_PROGRESS || toStatus == CARD_STATUS.DONE))
            return true;
        else
            return false;
    }

    /*
     * REQUIRES: @params != null
     * EFFECTS: Cerca la card nella lista.
     * RETURN: La card se la trova, null altrimenti.
     */
    private Card findCardInList(String cardTitle, CARD_STATUS fromListTitle) {
        List<Card> selectedList = getList(fromListTitle);
        if (selectedList == null) return null;

        for (Card crd : selectedList) {     // Cerco la card nella lista.
            if (crd.getCardTitle().equals(cardTitle)) {
                return crd;
            }
        }
        return null;
    }

    /*
     * RETURN: La lista relativa al titolo della lista.
     */
    private List<Card> getList(CARD_STATUS listTitle) {
        List<Card> selectedList;

        switch (listTitle) {         // Scelgo la lista corretta.
            case TODO:
                selectedList = todoList;
                break;
            case IN_PROGRESS:
                selectedList = inProgresList;
                break;
            case TO_BE_REVISED:
                selectedList = toBeRevisedList;
                break;
            case DONE:
                selectedList = doneList;
                break;
            default:
                return null;
        }
        return selectedList;
    }

    private CARD_STATUS getStatus(String cardStatus) {
        switch (cardStatus) {
            case "TODO":
                return CARD_STATUS.TODO;
            case "INPROGRESS":
                return CARD_STATUS.IN_PROGRESS;
            case "TOBEREVISED":
                return CARD_STATUS.TO_BE_REVISED;
            case "DONE":
                return CARD_STATUS.DONE;
            default:
                return null;
        }
    }

    /*
     * EFFECTS: Controlla se l'utente è registrato al progetto.
     * RETURN: true se lo è, false altrimenti.
     */
    private boolean isUserRegisteredToProject(String userNickname) {
        return projectUsers.contains(userNickname);
    }

    private void emptyList(List<Card> cardList) {
        for (Card card : cardList) {
            card.empty();
        }
        cardList.clear();
    }

}
