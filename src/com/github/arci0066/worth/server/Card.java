
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

// TODO: 11/06/21 Necessario serializable?
public class Card implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private String cardTitle;
    private String cardDescription;
    private CARD_STATUS cardStatus; //Descrittore della lista in cui si trova

    //implementazione separata per una migliore gestione
    private CardHistory cardHistory;


    //    -------- Constructors -------
    public Card(String cardTitle, String cardDescription, String nickname) {
        this.cardTitle = cardTitle;
        this.cardDescription = cardDescription;
        cardHistory = new CardHistory(nickname);
        cardStatus = CARD_STATUS.TODO;
    }

    //    ---------- Getters -------------
    public String getCardTitle() {
        return cardTitle;
    }

    public CARD_STATUS getCardStatus() {
        return cardStatus;
    }

    public String getCardHistory() {
        return cardHistory.toString();
    }

    //    ----------- Methods ------------

    /*
     * REQUIRES: userNick != null && newCardStatus != null
     * EFFECTS: cambia lo status della card in base a newCardStatus e aggiorna la history della card.
     * RETURN: il codice ritorno di cardHistory.add(...)
    */
    public ANSWER_CODE moveAndAdjournHistory(String userNickname, CARD_STATUS newCardStatus) {
        CARD_STATUS oldCardStatus = cardStatus;
        cardStatus = newCardStatus;
        return cardHistory.add(userNickname, oldCardStatus, newCardStatus);
    }


    /*
     * EFFECTS: Svuota la history della card e la setta a null per aiutare il GC
    */
    public void empty() {
        cardHistory.empty();
        cardHistory = null;
    }

    @Override
    public String toString() {
        return "[ Titolo: " + cardTitle +
                " in: "+ cardStatus +
                ", Descrizione: " + cardDescription + ']';
    }
}
