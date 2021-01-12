import java.util.ArrayList;
import java.util.List;


public class CardHistory {

    List<String> events;

//    ------ Costruttore --------
    public CardHistory(String nickname) {
        events = new ArrayList<>();
//        TODO aggiungere stampa del tempo
        events.add(nickname + " created this card @...");
    }

//    ------ Metodi -------
    public ANSWER_CODE add(String userNickname, CARD_STATUS cardStatus, CARD_STATUS newCardStatus) {
        events.add(userNickname +": " +cardStatus+ " -> " + newCardStatus+ "@...");
        return ANSWER_CODE.OP_OK;
    }

    @Override
    public String toString() {
        return "CardHistory{" +
                events +
                '}';
    }
}

/*
 * Salvare momento della modifica e nickname es. Pippo: TO_DO -> IN_PROGRESS @11/01/2020 13:45
 *
 *
 * */