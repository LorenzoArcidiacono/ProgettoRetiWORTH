package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CardHistory {

    List<String> events;
    SimpleDateFormat formatter;
    Date date;

    //    ------ Costruttore --------
    public CardHistory(String nickname) {
        formatter = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");
        date = new Date();
        events = new ArrayList<>();
//        TODO aggiungere stampa del tempo
        events.add(nickname + " created this card @ "+formatter.format(date));
    }

//    ------ Metodi -------
    public ANSWER_CODE add(String userNickname, CARD_STATUS cardStatus, CARD_STATUS newCardStatus) {
        events.add(userNickname +": " +cardStatus+ " -> " + newCardStatus+ " @"+ formatter.format(date));
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

        formatter = null;
        date = null;
    }
}

/*
 * Salvare momento della modifica e nickname es. Pippo: TO_DO -> IN_PROGRESS @11/01/2020 13:45
 *
 *
 * */