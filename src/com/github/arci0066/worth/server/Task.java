package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;

public class Task extends Thread {
    Message message;

// ------ Constructors ------
    public Task(Message message) {
        this.message = message;
    }

    @Override
    public void run() {
        ANSWER_CODE answer_code;
        switch (message.getOperationCode()){
            case LOGIN:{
                answer_code = login(message.getSenderNickname(),message.getSenderPassword());
            }
        }
    }

    // ------ Methods ------
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
        if (isNull(userNickname,userPassword)) {
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

    private User findUserByNickname(String userNickname) {
        for (User user : registeredUsersList) {
            if (userNickname.equals(user.getNickname())) {
                return user;
            }
        }
        return null;
    }
