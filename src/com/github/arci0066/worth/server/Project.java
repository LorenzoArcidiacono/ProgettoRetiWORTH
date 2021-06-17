/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.*;

import static com.github.arci0066.worth.server.ServerSettings.*;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;


//CLASSE THREAD SAFE
public class Project implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private String projectTitle;
    private List<Card> todoList, inProgressList, toBeRevisedList, doneList;
    private List<String> projectUsers; //Lista dei membri del progetto

    transient private ReadWriteLock lock; //lock per la mutua esclusione

    //--------- CHAT ----------
    transient private MulticastSocket ms;
    transient private InetAddress ia;
    transient private int port;
    transient private String address;

    transient private List<String> chatMsgs; //Lista dei messaggi inviati sulla chat


    // ------ Constructors ------
    public Project(String projectTitle, String userNickname, String address, int port) {
        this.projectTitle = projectTitle;
        projectUsers = new ArrayList<>();
        projectUsers.add(userNickname);

        todoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        toBeRevisedList = new ArrayList<>();
        doneList = new ArrayList<>();

        //Apre la connessione per la chat del progetto
        try {
            this.port = port;
            this.address = address;
            ms = new MulticastSocket(port);
            ia = InetAddress.getByName(address);
            ms.joinGroup(ia);
        } catch (IOException e) {
            e.printStackTrace();
        }
        chatMsgs = new ArrayList<>();

        lock = new ReentrantReadWriteLock();
    }

    // Costruttore nel caso di progetto letto dalla memoria
    public Project(Path path, String address, int port) {
        projectTitle = path.toString().replaceAll(serverBackupDirPath+"/","");
        todoList = new ArrayList<>();
        inProgressList = new ArrayList<>();
        toBeRevisedList = new ArrayList<>();
        doneList = new ArrayList<>();
        
        //legge la lista dei membri del progetto
        readUsersBackup(path);
        
        //legge la lista delle card del progetto
        readCardBackup(path.toString());

        //Apre la connessione per la chat del progetto
        try {
            this.port = port;
            this.address = address;
            ms = new MulticastSocket(port);
            ia = InetAddress.getByName(address);
            ms.joinGroup(ia);
        } catch (IOException e) {
            e.printStackTrace();
        }
        chatMsgs = new ArrayList<>();
        
        lock = new ReentrantReadWriteLock();
    }


    // ------ Getters -------
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
        return ANSWER_CODE.PERMISSION_DENIED.toString();
    }

    /*
     * REQUIRES: @params != null && userNickname appartiene al progetto
     * RETURN: restituisce la card con titolo = cardTitle, null altrimenti
     */
    public Card getCard(String cardTitle, String cardStatus, String userNickname) {
        Card card = null;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                card = findCardInList(cardTitle, getStatus(cardStatus));
            } finally {
                lock.readLock().unlock();
            }
        }
        return card;
    }

    public String getCardHistory(String cardTitle, String cardStatus, String userNickname) {
        String answer = null;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                answer = getCard(cardTitle, cardStatus, userNickname).getCardHistory();
            } finally {
                lock.readLock().unlock();
            }
        }
        return answer;
    }

    public String getChatAddress(String userNickname) {
        String chatAddress;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                chatAddress = address + ":" + port;
            } finally {
                lock.readLock().unlock();
            }
            return chatAddress;
        }
        return ANSWER_CODE.PERMISSION_DENIED.toString();
    }

    public String getChatHistory() {
        Gson gson = new Gson();
        String response;
        reciveAllMessagge();
        response = gson.toJson(chatMsgs);
        return response;
    }

// ------ Methods ------

    /*
     * REQUIRES: @params != null && user registrato al progetto.
     * EFFECTS: Aggiunge una nuova card alla lista TO_DO del progetto
     * RETURN: OP_OK se op. a buon fine
     * 			|| PERMISSION_DENIED se l'utente non è registrato al progetto
     * 			|| OP_FAIL in caso di errore.
     */
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
    public ANSWER_CODE moveCard(String cardTitle, String fromListTitle, String toListTitle, String userNickname) {
        CARD_STATUS fromStatus = getStatus(fromListTitle);
        CARD_STATUS toStatus = getStatus(toListTitle);
        ANSWER_CODE answer = ANSWER_CODE.OP_FAIL;

        if (fromStatus == null || toStatus == null) {
            return answer;
        }

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
            } else {
                if (getList(fromStatus).remove(card)) { // Se è rimossa dalla lista e aggiunta alla lista successiva.
                    getList(toStatus).add(card);
                    answer = card.moveAndAdjournHistory(userNickname, toStatus); // Provo ad aggiornare la Card e ritorno.
                }
                // In caso non sia riuscita a rimuoverla dalla lista resta OP_FAIL
            }
        } catch (NullPointerException e) {
            System.err.println("Errore in remove from list: " + e);
        } finally {
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
    public ANSWER_CODE addUser(String oldUserNickname, String newUserNickname) {
        if (!isUserRegisteredToProject(oldUserNickname))
            return ANSWER_CODE.PERMISSION_DENIED;
        //Se il nuovo utente fa già parte del progetto esco
        if(isUserRegisteredToProject(newUserNickname)){
            return ANSWER_CODE.EXISTING_USER;
        }
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

    /*
     * REQUIRES: userNickname != null && userNickname registrato al progetto
     * EFFECTS: se tutte le card sono segnate come done chiude il progetto
     * RETURN:  OP_OK in assenza di errori
     *          || PERMISSIONE_DENIED se l'utente non è registrato al progetto
     *          || PROJECT_NOT_FINISHED se ci sono card | card.getCardStatus != DONE
     */
    public void cancelProject(String userNickname) {

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
    }

    /*
    * REQUIRES: userNickname != null && userNickname registrato al progetto
    * EFFECTS: controlla che il progetto possa essere chiuso
    * RETURN: PERMISSION_DENIED se l'utente non fa parte del progetto
    *           || PROJECT_NOT_FINISHED se non tutte le card sono nella lista DONE
    *           || OP_OK altrimenti
    */
    public ANSWER_CODE isCancellable(String userNickname) {
        if (!isUserRegisteredToProject(userNickname))
            return ANSWER_CODE.PERMISSION_DENIED;

        //Controllo che tutte le card siano nella lista DONE
        if (!(todoList.isEmpty()
                && inProgressList.isEmpty()
                && toBeRevisedList.isEmpty()))
            return ANSWER_CODE.PROJECT_NOT_FINISHED;

        return ANSWER_CODE.OP_OK;
    }

    /*
     * REQUIRES: userNickname registrato al progetto
     * RETURN: una stringa conentente tutte le card del progetto suddivise nelle liste in assenza di errori
     *          || PERMISSION_DENIED altrimenti
     */
    public String showCards(String userNickname) {
        String answer;
        if (isUserRegisteredToProject(userNickname)) {
            lock.readLock().lock();
            try {
                answer = "Progetto: " + projectTitle +
                        "\n Todo: " + printList(todoList) +
                        "\n In Progress: " + printList(inProgressList) +
                        "\n To Be Revised: " + printList(toBeRevisedList) +
                        "\n Done: " + printList(doneList);
            } finally {
                lock.readLock().unlock();
            }
            return answer;
        }
        return ANSWER_CODE.PERMISSION_DENIED.toString();
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
        else
            return fromStatus == CARD_STATUS.TOBEREVISED && (toStatus == CARD_STATUS.INPROGRESS || toStatus == CARD_STATUS.DONE);
    }

    /*
     * REQUIRES: @params != null
     * EFFECTS: Cerca la card nella lista.
     * RETURN: La card se la trova, null altrimenti.
     */
    /* todo Non è thread safe ma i metodi che la invocano hanno chiamato la lock */
    private Card findCardInList(String cardTitle, CARD_STATUS fromListTitle) {
        List<Card> selectedList = getList(fromListTitle);
        if (selectedList == null) return null;
        Card card = null;
        for (Card crd : selectedList) {     // Cerco la card nella lista.
            if (crd.getCardTitle().equals(cardTitle)) {
                card = crd;
            }
        }
        return card;
    }

    /*
    * REQUIRES: list != null
    * RETURN: una stringa contenente le card della lista
    */
    private String printList(List<Card> list) {
        String s = "";
        for (Card c : list) {
            s += c.getCardTitle() + ",";
        }
        return s;
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


    /*
     * EFFECTS: legge tutti i messaggi inviati sulla chat del progetto dall'ultima invocazione di questo metodo
     *          e li salva in memoria
     */
    private void reciveAllMessagge() {
        boolean done = false;
        byte[] data = new byte[1024];
        DatagramPacket dp = new DatagramPacket(data, data.length);

        lock.writeLock().lock();
        try {
            ms.setSoTimeout(ServerSettings.MULTICAST_MSG_WAITING);
            while (!done) {
                ms.receive(dp);
                String s = new String(dp.getData(), 0, dp.getLength());
                chatMsgs.add(s);
            }
        } catch (SocketTimeoutException e) {
        } //Non ci sono altri messaggi in coda quindi posso uscire.
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.writeLock().unlock();
        }
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
    
    /*
     * REQUIRES: cardStatus != null
     * EFFECTS: restituisce il CARD_STATUS in base alla stringa passata
     * RETURN: il CARD_STATUS corretto
     */
    private CARD_STATUS getStatus(String cardStatus) {
        cardStatus = cardStatus.toUpperCase();
        return switch (cardStatus) {
            case "TODO" -> CARD_STATUS.TODO;
            case "INPROGRESS" -> CARD_STATUS.INPROGRESS;
            case "TOBEREVISED" -> CARD_STATUS.TOBEREVISED;
            case "DONE" -> CARD_STATUS.DONE;
            default -> null;
        };
    }


    // ---------- Closing && Backup methods -----------------

    /*
     * REQUIRES: cardList != null
     * EFFECTS: libera lo spazio di memoria delle singole card della lista e della lista stessa
     */
    private void emptyList(List<Card> cardList) {
        if (cardList == null) {
            return;
        }
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


    /*
     * REQUIRES: path != null
     * EFFECTS: chiede di fare il backup di tutte le liste di card
     */
    public void saveCard(Path path) {
        lock.readLock().lock();
        try {
            for (Card crd : todoList) {
                backupCard(path, crd);
            }
            for (Card crd : inProgressList) {
                backupCard(path, crd);
            }
            for (Card crd : toBeRevisedList) {
                backupCard(path, crd);
            }
            for (Card crd : doneList) {
                backupCard(path, crd);
            }
        } finally {
            lock.readLock().unlock();
        }
    }


    /*
     * REQUIRES: @params != null
     * EFFECTS: scrive le informazioni della card sul file indicato da path
     */
    private void backupCard(Path path, Card crd) {
        Path cardPath;
        Gson gson = new Gson();
        cardPath = Paths.get(path + "/" + crd.getCardTitle() + ".crd");
        try (BufferedWriter writer = Files.newBufferedWriter(cardPath, StandardCharsets.UTF_8)) {
           /* writer.write("Title: " + crd.getCardTitle() + "<\n");
            writer.write("Description: " + crd.getCardDescription() + "<\n");
            writer.write("List:"+crd.getCardStatus()+"<\n");
            writer.write("History: " + crd.getCardHistory() + "<\n");*/
            writer.write(gson.toJson(crd));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /*
    * REQUIRES: projectPath != null
    * MODIFIES: this
    * EFFECTS: legge le card dal backup del progetto e le salva nella lista corretta.
    */
    private void readCardBackup(String projectPath) {
        Gson gson = new Gson();
        List<Path> result = null;

        //Legge i nomi dei file nella cartella
        try (Stream<Path> paths = Files.walk(Paths.get(projectPath), 1)) {
            result = paths.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
            //result.remove(Paths.get(serverBackupDirPath));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //prende i path delle card
        if (result != null) {
            // TODO: 18/05/21 capire questa lambda
            result.removeIf(path -> !(path.toString().contains(".crd")));
        }
        //Legge tutte le card e le aggiunge alla lista
        String cardTextFile;
        for (Path path: result) {
            cardTextFile = "";
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String line;
                while ((line = reader.readLine()) != null)
                    cardTextFile += line;
            } catch (IOException e) {
                e.printStackTrace();
            }
            //creo la card in base al json salvato
            Card crd = gson.fromJson(cardTextFile, new TypeToken<Card>() {}.getType());
            
            //Seleziono la lista corretta e vi aggiungo la card
            List<Card> crdList = getList(crd.getCardStatus());
            if(crdList == null){
                System.err.println("Errore lista sbagliata:"+crd+","+crd.getCardStatus() );
                return;
            }
            crdList.add(crd);
        }
    }

    /*
     * REQUIRES: path != null
     * MODIFIES: projectUsers
     * EFFECTS: legge dalla memoria la lista dei membri del progetto.
     */
    private void readUsersBackup(Path path) {
        String usersNickname = "";
        Path nicknamePath = Paths.get(path + projectUsersBackupFile); //path del file degli utenti
        Gson gson = new Gson();
        projectTitle = path.toString().replaceAll(serverBackupDirPath + "/", ""); //Estrae il tiolo del progetto

        try (BufferedReader reader = Files.newBufferedReader(nicknamePath)) {
            String line;
            while ((line = reader.readLine()) != null)
                usersNickname += line;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //trascrive gli utenti registrati
        projectUsers = gson.fromJson(usersNickname, new TypeToken<List<String>>() {
        }.getType());
    }
    
    /*
    * REQUIRES: userListPath != null
    * EFFECTS: Scrive in memeoria la lista dei membri del progetto.
    */
    public void saveUsersList(Path userListPath) {
        Gson gson = new Gson();
        try {
            BufferedWriter writer = Files.newBufferedWriter(userListPath, StandardCharsets.UTF_8);
            writer.write(gson.toJson(projectUsers));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
