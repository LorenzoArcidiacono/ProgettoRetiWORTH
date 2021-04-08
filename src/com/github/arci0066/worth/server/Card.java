package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

import java.io.Serial;
import java.io.Serializable;

public class Card implements com.github.arci0066.worth.interfaces.CardInterface, Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    private String cardTitle;
    private String cardDescription;
    private CARD_STATUS cardStatus;

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
    @Override
    public String getCardTitle() {
        return cardTitle;
    }

    @Override
    public String getCardDescription() {
        return cardDescription;
    }

    @Override
    public CARD_STATUS getCardStatus() {
        return cardStatus;
    }

    @Override
    public String getCardHistory() {
        return cardHistory.toString();
    }

    //    ----------- Methods ------------
    @Override
    public ANSWER_CODE changeStatus(CARD_STATUS newCardStatus) {
        return null;
    }

    @Override
    public ANSWER_CODE moveAndAdjournHistory(String userNickname, CARD_STATUS newCardStatus) {
//TODO       Se il nuovo stato non è compatibile ritorno errore (dovrebbe controllarlo il progetto)
// TODO: 14/01/21 dovrei aggiornare dopo lo status
        CARD_STATUS oldCardStatus = cardStatus;
        cardStatus = newCardStatus;
        return cardHistory.add(userNickname, oldCardStatus, newCardStatus);
    }

    @Override
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
