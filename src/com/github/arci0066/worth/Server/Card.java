
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

    public ANSWER_CODE adjournHistory(String userNickname, CARD_STATUS newCardStatus) {
//TODO       Se il nuovo stato non è compatibile ritorno errore (dovrebbe controllarlo il progetto)

        cardStatus = newCardStatus;
        return cardHistory.add(userNickname,cardStatus,newCardStatus);
    }
}
