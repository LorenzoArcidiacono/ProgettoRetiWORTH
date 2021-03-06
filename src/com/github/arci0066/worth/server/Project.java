package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.arci0066.worth.server.ServerSettings.projectUsersBackupFile;
import static com.github.arci0066.worth.server.ServerSettings.serverBackupDirPath;

//CLASSE THREAD SAFE
public class Project implements com.github.arci0066.worth.interfaces.ProjectInterface {
    private static final String UTENTE_ERRATO = "Utente non membro del progetto.";

    private String projectTitle;
    private List<Card> todoList, inProgressList, toBeRevisedList, doneList;
    private List<String> projectUsers;
    private ReadWriteLock lock;

    //Come la implemento?
    // TODO: 12/01/21  private Chat projectChat;


    // ------ Constructors ------
    public Project(String projectTitle, String userNickname) {
        this.projectTitle = projectTitle;
        projectUsers = new ArrayList<>();
        todoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        toBeRevisedList = new ArrayList<>();
        doneList = new ArrayList<>();
        projectUsers.add(userNickname);
        lock = new ReentrantReadWriteLock();
    }

    public Project(Path path) {
        String usersNickname = "";
        Path nicknamePath = Paths.get(path+projectUsersBackupFile);
        Gson gson = new Gson();
        projectTitle = path.toString().replaceAll(serverBackupDirPath+"/","");

        try (BufferedReader reader = Files.newBufferedReader(nicknamePath)){
            String line;
            while ((line = reader.readLine()) != null)
                usersNickname += line;
        } catch (IOException e) {
            e.printStackTrace();
        }

        projectUsers = gson.fromJson(usersNickname,new TypeToken<List<String>>() {}.getType());
        System.err.println(projectUsers);

        todoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        toBeRevisedList = new ArrayList<>();
        doneList = new ArrayList<>();
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

    /*
     * REQUIRES: @params != null && userNickname appartiene al progetto
     * RETURN: restituisce la card con titolo = cardTitle, null altrimenti
     */
    // TODO: 14/01/21 passare una copia? una stringa?
    @Override
    public Card getCard(String cardTitle, String cardStatus, String userNickname) {
        Card card = null;
        if (isUserRegisteredToProject(userNickname)){
            lock.readLock().lock();
            try {
                card = findCardInList(cardTitle, getStatus(cardStatus));
            } finally {
                lock.readLock().unlock();
            }
        }
        return card;
    }

    @Override
    public String getCardHistory(String cardTitle, String cardStatus, String userNickname) {
        String answer;
        lock.readLock().lock();
        try {
            answer = getCard(cardTitle, cardStatus, userNickname).getCardHistory();
        } finally {
            lock.readLock().unlock();
        }
        return answer;
    }

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
        ANSWER_CODE answer = ANSWER_CODE.OP_FAIL;
        if (!checkStep(fromStatus, toStatus))
            return ANSWER_CODE.WRONG_LIST;
        else if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        lock.writeLock().lock();
        try {
            // Se supera i controlli cerco la card nella lista.
            Card card = findCardInList(cardTitle, fromStatus); // In teoria potrebbe trovare la card nella lista DONE, questo non è possibile grazie al controllo sugli step.
            if (card == null) {
                answer = ANSWER_CODE.UNKNOWN_CARD;
            } else if (getList(fromStatus).remove(card)) { // Se è rimossa dalla lista e aggiunta alla lista successiva.
                if (getList(toStatus).add(card)) {
                    answer = card.moveAndAdjournHistory(userNickname, toStatus); // Provo ad aggiornare la Card e ritorno.
                } else { // In caso non sia riuscito ad aggiungerla alla lista successiva provo a ripristinare tutto.
                    getList(fromStatus).add(card);
                    answer = ANSWER_CODE.OP_FAIL;
                }
            } else // In caso non sia riuscita a rimuoverla dalla lista.
                answer = ANSWER_CODE.OP_FAIL;
        }catch (NullPointerException e){
            System.err.println("Errore in remove frome list: "+e );
        }
        finally {
            lock.writeLock().unlock();
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
            projectUsers.add(newUserNickname);
            answer = ANSWER_CODE.OP_OK;
        } catch (Exception e) {
            answer = ANSWER_CODE.OP_FAIL;
        } finally {
            lock.writeLock().unlock();
        }
        return answer;
    }

    // TODO: 12/01/21 implementare

    /*
     * REQUIRES: userNickname != null && userNickname registrato al progetto
     * EFFECTS: se tutte le card sono segnate come done chiude il progetto
     * RETURN:  OP_OK in assenza di errori
     *          || PERMISSIONE_DENIED se l'utente non è registrato al progetto
     *          || PROJECT_NOT_FINISHED se ci sono card | card.getCardStatus != DONE
    */
    @Override
    public ANSWER_CODE cancelProject(String userNickname) {
        if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;
// TODO: 01/03/21 controllare che tutte le card siano in done!
        lock.writeLock().lock();
        try {
            projectTitle = null;
            emptyList(todoList);
            emptyList(inProgressList);
            emptyList(toBeRevisedList);
            emptyList(doneList);
            projectUsers.clear();
            projectUsers = null;
        } finally {
            lock.writeLock().unlock();
        }
        return ANSWER_CODE.OP_OK;
    }


    /*
     * REQUIRES: userNickname registrato al progetto
     * RETURN: una stringa conentente tutte le card del progetto suddivise nelle liste in assenza di errori
     *          || PERMISSION_DENIED altrimenti
    */
    @Override
    public String showCards(String userNickname) {
        String answer;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                answer = "Progetto: " + projectTitle +
                        ",\n Todo: " + todoList.toString() +
                        ",\n In Progress: " + inProgressList.toString() +
                        ",\n To Be Revised: " + toBeRevisedList.toString() +
                        ",\n Done: " + doneList.toString();
            } finally {
                lock.readLock().unlock();
            }
            return answer;
        }
        // TODO: 01/03/21 cambiare con una risposta standard
        return ANSWER_CODE.PERMISSION_DENIED.toString();
    }



    /*
     * EFFECTS: Crea una stringa con il titolo di ogni card in ogni lista e gli utenti del progetto.
     * RETURN: La stringa creata.
     */
    // TODO: 14/01/21 se non registrato non dovrebbe vedere la lista degli utenti
    @Override
    public String prettyPrint(String userNickname) {
        String str;
        lock.readLock().lock();
        try {
            str = showCards(userNickname) +
                    ",\n Utenti Registrati: " + projectUsers;
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

//    ------- Private Methods --------

    /*
     * EFFECTS: controlla che lo step scelto sia corretto per il flusso di lavoro.
     * RETURN: true se lo è, false altrimenti.
     */
    private boolean checkStep(CARD_STATUS fromStatus, CARD_STATUS toStatus) {
        if (fromStatus == CARD_STATUS.TODO && toStatus == CARD_STATUS.INPROGRESS)
            return true;
        else if (fromStatus == CARD_STATUS.INPROGRESS && (toStatus == CARD_STATUS.TOBEREVISED || toStatus == CARD_STATUS.DONE))
            return true;
        else if (fromStatus == CARD_STATUS.TOBEREVISED && (toStatus == CARD_STATUS.INPROGRESS || toStatus == CARD_STATUS.DONE))
            return true;
        else
            return false;
    }

    /*
     * REQUIRES: @params != null
     * EFFECTS: Cerca la card nella lista.
     * RETURN: La card se la trova, null altrimenti.
     */
    /* Non è thread safe ma i metodi che la invocano hanno chiamato la lock */
    private Card findCardInList(String cardTitle, CARD_STATUS fromListTitle) {
        List<Card> selectedList = getList(fromListTitle);
        if (selectedList == null) return null;
        Card card = null;
        for (Card crd : selectedList) {     // Cerco la card nella lista.
            if (crd.getCardTitle().equals(cardTitle)) {
                card = crd;
            }
        }
        //TODO se si blocca qui e un altro thread la sposta????
        return card;
    }

    /*
     * EFFECTS: Controlla se l'utente è registrato al progetto.
     * RETURN: true se lo è, false altrimenti.
     */
    private boolean isUserRegisteredToProject(String userNickname) {
        boolean answer;
        lock.readLock().lock();
        try {
            answer = projectUsers.contains(userNickname);
        } finally {
            lock.readLock().unlock(); /* Rilascio la lock, l'utente non può essere rimosso da altri thread */
        }
        return answer;
    }

    //----------------- Status Select --------------------------
    /*
     * RETURN: La lista relativa al titolo della lista.
     */
    private List<Card> getList(CARD_STATUS listTitle) {
        List<Card> selectedList;

        switch (listTitle) {         // Scelgo la lista corretta.
            case TODO:
                selectedList = todoList;
                break;
            case INPROGRESS:
                selectedList = inProgressList;
                break;
            case TOBEREVISED:
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
        cardStatus=cardStatus.toUpperCase();
        return switch (cardStatus) {
            case "TODO" -> CARD_STATUS.TODO;
            case "INPROGRESS" -> CARD_STATUS.INPROGRESS;
            case "TOBEREVISED" -> CARD_STATUS.TOBEREVISED;
            case "DONE" -> CARD_STATUS.DONE;
            default -> null;
        };
    }


// ---------- Closing && Backup methods -----------------
    private void emptyList(List<Card> cardList) {
        lock.writeLock().lock();
        try {
            for (Card card : cardList) {
                card.empty();
            }
            cardList.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void saveCard(Path path) {
        lock.readLock().lock();
        try {
            for (Card crd : todoList) {
                backupCard(path, crd);
            }
            for (Card crd : inProgressList) {
                backupCard(path,crd);
            }
            for (Card crd : toBeRevisedList) {
                backupCard(path,crd);
            }
            for (Card crd : doneList) {
                backupCard(path,crd);
            }
        } finally {
            lock.readLock().unlock();
        }

    }

    private void backupCard(Path path, Card crd) {
        Path cardPath;
        cardPath = Paths.get(path +"/"+ crd.getCardTitle()+".txt");
        try(BufferedWriter writer = Files.newBufferedWriter(cardPath, Charset.forName("UTF-8"))){
            writer.write("Title:"+ crd.getCardTitle()+"\n");
            writer.write("Description:"+ crd.getCardDescription()+"\n");
            writer.write("List:"+ crd.getCardStatus()+"\n");
            writer.write(crd.getCardHistory()+"\n");
        }catch(IOException ex){
            ex.printStackTrace();
        }
    }

    public void saveUsersList(Path userListPath) {
        Gson gson = new Gson();
        try {
            BufferedWriter writer = Files.newBufferedWriter(userListPath, Charset.forName("UTF-8"));
            writer.write(gson.toJson(projectUsers));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
