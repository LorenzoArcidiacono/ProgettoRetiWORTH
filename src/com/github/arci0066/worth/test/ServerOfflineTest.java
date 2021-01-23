import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.github.arci0066.worth.server.Card;
import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class ServerOfflineTest {

    private static final String PROGETTO = "Progetto 1";
    private static final String PROGETTO_2 = "Progetto 2";
    private static final String PIPPO = "Pippo";
    private static final String PLUTO = "Pluto";
    private static final String TOPOLINO = "Topolino";
    private static final String PAPERINO = "Paperino";

    @Test
    @DisplayName("Server General Test offline")
    public static void main(String[] args) {
        Server server = new Server();
        Message request;
        //Registro un po' di utenti
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.register(PIPPO, "Pippo1"));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.register(PLUTO, "Pluto1"));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.register(TOPOLINO, "Topo1"));
//        Controllo gestione errori di registrazione
        Assertions.assertEquals(ANSWER_CODE.EXISTING_USER, server.register(PIPPO, "Pippo1"));
        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.register(PLUTO, null));

        //Stampo le liste degli utenti
        request = new Message(PIPPO,null,OP_CODE.LIST_USER,null,null,null);
        server.reciveMessage(request);
        prettyPrint(server.listOnlineUsers());

        //Provo il login
        //Assertions.assertEquals(ANSWER_CODE.OP_OK, server.login(PIPPO, "Pippo1"));
        request = new Message(PIPPO, "Pippo1", OP_CODE.LOGIN, null, null, null);
        server.reciveMessage(request);
        request = new Message(PLUTO, "Pluto1", OP_CODE.LOGIN, null, null, null);
        server.reciveMessage(request);
        request = new Message(TOPOLINO, "Topo1", OP_CODE.LOGIN, null, null, null);
        server.reciveMessage(request);
//       Errori di Login
        Assertions.assertEquals(ANSWER_CODE.WRONG_PASSWORD, server.login(PIPPO, "Pippo"));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_USER, server.login(PAPERINO, "Pippo"));
        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.login(PIPPO, null));

        prettyPrint(server.listUsers());
        // se non riusltano online è per via dei thread che non hanno finito
        prettyPrint(server.listOnlineUsers());

        //Provo progetto
        prettyPrint(server.listProjects());
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.createProject(PROGETTO, PLUTO));

        //qui i thread dovrebbero avere finito
        prettyPrint(server.listOnlineUsers());

        //        Errori creazione progetto
        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.createProject(null, PLUTO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_USER, server.createProject(PROGETTO, PAPERINO));
        Assertions.assertEquals(ANSWER_CODE.EXISTING_PROJECT, server.createProject(PROGETTO, PLUTO));

        prettyPrint(server.listProjects());

        prettyPrint(server.showMembers("Progetto 1", PLUTO));
        prettyPrint(server.showMembers("Progetto 1", PIPPO));

//        Aggiungo membro al progetto
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.addMember("Progetto 1", PLUTO, PIPPO));

        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.addMember("Progetto 1", null, PIPPO));
        Assertions.assertEquals(ANSWER_CODE.PERMISSION_DENIED, server.addMember("Progetto 1", TOPOLINO, PLUTO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_USER, server.addMember("Progetto 1", PAPERINO, PIPPO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_PROJECT, server.addMember(PROGETTO_2, PLUTO, PIPPO));


        prettyPrint(server.showMembers("Progetto 1", PIPPO));

//        Provo gestione Card
        prettyPrint(server.showCards("Progetto 1", PIPPO));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.addCard(PROGETTO, "Sommare", "Sommare tutti i valori", PIPPO));

        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_PROJECT, server.addCard(PROGETTO_2, "Sommare", "Sommare tutti i valori", PIPPO));
        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.addCard(PROGETTO, null, "Sommare tutti i valori", PIPPO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_USER, server.addCard(PROGETTO, "Sommare", "Sommare tutti i valori", PAPERINO));
        Assertions.assertEquals(ANSWER_CODE.PERMISSION_DENIED, server.addCard(PROGETTO, "Sommare", "Sommare tutti i valori", TOPOLINO));


        prettyPrint(server.showCards(PROGETTO, PIPPO));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.moveCard(PROGETTO, "Sommare", "TODO", "INPROGRESS", PLUTO));

        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_PROJECT, server.moveCard(PROGETTO_2, "Sommare", "INPROGRESS", "TOBEREVISED", PLUTO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_CARD, server.moveCard(PROGETTO, "Sottrarre", "INPROGRESS", "TOBEREVISED", PLUTO));
        Assertions.assertEquals(ANSWER_CODE.UNKNOWN_USER, server.moveCard(PROGETTO, "Sommare", "INPROGRESS", "TOBEREVISED", PAPERINO));
        Assertions.assertEquals(ANSWER_CODE.WRONG_LIST, server.moveCard(PROGETTO, "Sommare", "INPROGRESS", "TODO", PLUTO));
        Assertions.assertEquals(ANSWER_CODE.PERMISSION_DENIED, server.moveCard(PROGETTO, "Sommare", "INPROGRESS", "TOBEREVISED", TOPOLINO));
        Assertions.assertEquals(ANSWER_CODE.OP_FAIL, server.moveCard(PROGETTO, "Sommare", null, "TOBEREVISED", PLUTO));

        prettyPrint(server.showCards(PROGETTO, PIPPO));
        prettyPrint(
                server.showCard(PROGETTO, "Sommare", "INPROGRESS", PIPPO)
                        .getCardHistory());

    }

    private static void prettyPrint(String opString) {
        System.out.println("@ " + opString + "\n");
    }

}
