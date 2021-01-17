import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.server.Card;
import com.github.arci0066.worth.server.Server;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


public class MainTest {

    public static final String PROGETTO = "Progetto 1";
    private static final String PIPPO = "Pippo";
    private static  final String PLUTO = "Pluto";

    @Test
    @DisplayName("Server General Test offline")
    public static void main(String[] args) {
        Server server = new Server();

        //Registro un po' di utenti
        Assertions.assertEquals(ANSWER_CODE.OP_OK,server.register(PIPPO, "Pippo1"));
        Assertions.assertEquals(ANSWER_CODE.OP_OK,server.register(PLUTO, "Pluto1"));
//        Provo a registrare un utente esistente
        Assertions.assertEquals(ANSWER_CODE.EXISTING_USER,server.register(PIPPO, "Pippo1"));

        //Stampo le liste
        System.out.println(server.listUsers());
        System.out.println(server.listOnlineUsers());

        //Provo il login
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.login(PIPPO, "Pippo1"));

        System.out.println(server.listUsers());
        System.out.println(server.listOnlineUsers());

        //Provo progetto
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.createProject(PROGETTO, PLUTO));

        System.out.println(server.showMembers("Progetto 1", PLUTO));
        System.out.println(server.showMembers("Progetto 1", PIPPO));

        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.addMember("Progetto 1", PLUTO, PIPPO));

        System.out.println(server.showMembers("Progetto 1", PIPPO));

//        Provo gestione Card
        System.out.println(server.showCards("Progetto 1", PIPPO));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.addCard(PROGETTO,"Sommare","Sommare tutti i valori", PIPPO));

        System.out.println(server.showCards(PROGETTO, PIPPO));
        Assertions.assertEquals(ANSWER_CODE.OP_OK, server.moveCard(PROGETTO,"Sommare","TODO","INPROGRESS", PLUTO));
        System.out.println(server.showCards(PROGETTO, PIPPO));
        System.out.println(
                server.showCard(PROGETTO,"Sommare","INPROGRESS", PIPPO)
                .getCardHistory());
    }

}
