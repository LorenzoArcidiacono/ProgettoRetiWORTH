/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */

package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class CardHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    List<String> events; //Lista degli eventi della card
    transient DateTimeFormatter dtf;

    //    ------ Costruttore --------
    public CardHistory(String nickname) {
        dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        events = new ArrayList<>();
        events.add("Creata da @"+nickname + " @ "+dtf.format(LocalDateTime.now()));
    }

//    ------ Metodi -------
    
    
    /*
     * REQUIRES: @params != null
     * EFFECTS: aggiunge lo spostamento della card da cardStatus a newCardStatus alla lista degli eventi
     * RETURN: OP_OK se è andata a buon fine, OP_FAIL in caso di errore
    */
    public ANSWER_CODE add(String userNickname, CARD_STATUS cardStatus, CARD_STATUS newCardStatus) {
        if(dtf == null){ //inizializzazione dopo un backup
            dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        }
        events.add("@"+userNickname +": " +cardStatus+ " -> " + newCardStatus+ " @"+ dtf.format(LocalDateTime.now()));
        return ANSWER_CODE.OP_OK;
    }

    @Override
    public String toString() {
        return "Eventi: " + events;
    }


    /*
     * EFFECTS: svuota la lista degli eventi e la setta a null per aiutare il GC
    */
    public void empty() {
        events.clear();
        events = null;
        dtf = null;
    }
}
