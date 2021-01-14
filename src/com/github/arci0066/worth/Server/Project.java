import java.util.ArrayList;
import java.util.List;

public class Project {
    public static final String UTENTE_ERRATO = "Utente non membro del progetto.";
    private String projectTitle;
    private List<Card> todoList, inProgresList, toBeRevisedList, doneList;
    private List<String> projectUsers;

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
    }

    // ------ Getters -------
    public String getProjectTitle() {
        return projectTitle;
    }

    public String getProjectUsers(String userNickname) {
        if(isUserRegisteredToProject(userNickname))
            return projectUsers.toString();
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
    public ANSWER_CODE addCard(String cardTitle, String cardDescription, String userNickname) {
        if(!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        Card card = new Card(cardTitle,cardDescription,userNickname);
        todoList.add(card);

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
    public ANSWER_CODE moveCard(String cardTitle, String fromListTitle, String toListTitle, String userNickname) {
        CARD_STATUS fromStatus = getStatus(fromListTitle);
        CARD_STATUS toStatus = getStatus(toListTitle);
        if (!checkStep(fromStatus,toStatus))
            return ANSWER_CODE.WRONG_LIST;
        else if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;
        else { // Se supera i controlli cerco la card nella lista.
            Card card = findCardInList(cardTitle, fromStatus); // In teoria potrebbe trovare la card nella lista DONE, questo non è possibile grazie al controllo sugli step.
            if (card == null) {
                return ANSWER_CODE.UNKNOWN_CARD;
            }

            if(getList(fromStatus).remove(card)){ // Se è rimossa dalla lista e aggiunta alla lista successiva.
                 if(getList(toStatus).add(card)){
                     return card.moveAndAdjournHistory(userNickname, toStatus); // Provo ad aggiornare la Card e ritorno.
                 }
                 else{ // In caso non sia riuscito ad aggiungerla alla lista successiva provo a ripristinare tutto.
                     getList(fromStatus).add(card);
                     return ANSWER_CODE.OP_FAIL;
                 }
            }
            else // In caso non sia riuscita a rimuoverla dalla lista.
                return ANSWER_CODE.OP_FAIL;
        }
    }

    /*
     * REQUIRES: String != null
     *  && userNickname di un utente registrato (Controllato dal Server)
     *  && projectTitle di un progetto esistente  (Controllato dal Server)
     *  && user registrato al progetto.
     *
     * EFFECTS: Aggiunge l'utente alla lista degli utenti del progetto.
     *
     * RETURN: OP_OK in caso non ci siano stati errori
     *			|| PERMISSION_DENIED se l'utente oldUserNickname non è registrato al progetto,
     *			|| OP_FAIL altrimenti.
     */
    public ANSWER_CODE addUser(String oldUserNickname, String newUserNickname) {
        if (!isUserRegisteredToProject(oldUserNickname))
            return ANSWER_CODE.PERMISSION_DENIED;
        try {
            if(!projectUsers.add(newUserNickname))
                return ANSWER_CODE.OP_FAIL;
        }
        catch (Exception e){
            return ANSWER_CODE.OP_FAIL;
        }
        return ANSWER_CODE.OP_OK;
    }

    // TODO: 12/01/21 implementare
    public ANSWER_CODE cancelProject(){ return ANSWER_CODE.OP_FAIL; }

    /*
     * EFFECTS: Crea una stringa con il titolo di ogni card in ogni lista e gli utenti del progetto.
     * RETURN: La stringa creata.
     */
    // TODO: 14/01/21 se non registrato non dovrebbe vedere la lista degli utenti
    public String prettyPrint(String userNickname) {
        return showCards(userNickname) +
                ",\n Utenti Registrati: " + projectUsers;
    }

    public String showCards(String userNickname) {
        if(isUserRegisteredToProject(userNickname)){
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
    public Card getCard(String cardTitle, String cardStatus, String userNickname) {
        if(isUserRegisteredToProject(userNickname))
            return findCardInList(cardTitle, getStatus(cardStatus));
        return null;
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
        switch (cardStatus){
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
}
