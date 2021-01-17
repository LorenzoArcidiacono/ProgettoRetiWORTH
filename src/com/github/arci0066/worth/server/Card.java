package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

public class Card {
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
    public String getCardTitle() {
        return cardTitle;
    }

    public String getCardDescription() {
        return cardDescription;
    }

    public CARD_STATUS getCardStatus() {
        return cardStatus;
    }

    public String getCardHistory() {
        return cardHistory.toString();
    }

    //    ----------- Methods ------------
    public ANSWER_CODE changeStatus(CARD_STATUS newCardStatus) {
        return null;
    }

    public ANSWER_CODE moveAndAdjournHistory(String userNickname, CARD_STATUS newCardStatus) {
//TODO       Se il nuovo stato non è compatibile ritorno errore (dovrebbe controllarlo il progetto)
// TODO: 14/01/21 dovrei aggiornare dopo lo status
        CARD_STATUS oldCardStatus = cardStatus;
        cardStatus = newCardStatus;
        return cardHistory.add(userNickname, oldCardStatus, newCardStatus);
    }

    public void empty() {
        cardHistory.empty();
        cardHistory = null;
    }

    @Override
    public String toString() {
        return "[ Titolo: " + cardTitle +
                ", Descrizione: " + cardDescription + ']';
    }
}
