package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CardHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    List<String> events;
    transient DateTimeFormatter dtf = null;

    //    ------ Costruttore --------
    public CardHistory(String nickname) {
        dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        events = new ArrayList<>();
//        TODO aggiungere stampa del tempo
        events.add(nickname + " created this card @ "+dtf.format(LocalDateTime.now()));
    }

//    ------ Metodi -------
    public ANSWER_CODE add(String userNickname, CARD_STATUS cardStatus, CARD_STATUS newCardStatus) {
        if(dtf == null){
            dtf = DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        }
        events.add(userNickname +": " +cardStatus+ " -> " + newCardStatus+ " @"+ dtf.format(LocalDateTime.now()));
        return ANSWER_CODE.OP_OK;
    }

    @Override
    public String toString() {
        return "CardHistory{" +
                events +
                '}';
    }

    public void empty() {
        events.clear();
        events = null;
        dtf = null;
    }
}

/*
 * Salvare momento della modifica e nickname es. Pippo: TO_DO -> IN_PROGRESS @11/01/2020 13:45
 *
 *
 * */