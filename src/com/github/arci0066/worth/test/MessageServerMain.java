import com.github.arci0066.worth.enumeration.CARD_STATUS;
import com.github.arci0066.worth.enumeration.OP_CODE;
import com.github.arci0066.worth.server.Message;
import com.github.arci0066.worth.server.Server;

import static java.lang.Thread.sleep;

public class MessageServerMain {
    private static final String PROGETTO = "Progetto 1";
    private static final String PROGETTO_2 = "Progetto 2";

    private static final String PIPPO = "Pippo";
    private static final String PIPPO_PWD = "Pippo1";
    private static final String PLUTO = "Pluto";
    private static final String PLUTO_PWD = "Pluto1";
    private static final String TOPOLINO = "Topolino";
    private static final String TOPOLINO_PWD = "Topolino1";
    private static final String PAPERINO = "Paperino";


    public static void main(String[] args) throws InterruptedException {
        Server server = new Server();
        Message request;

        //Registrazione
        server.register(PIPPO,PIPPO_PWD);
        server.register(PLUTO,PLUTO_PWD);
        server.register(TOPOLINO,TOPOLINO_PWD);

        //LOGIN
        request = new Message(PIPPO,PIPPO_PWD, OP_CODE.LOGIN,null,null,null);
        server.reciveMessage(request);
        request = new Message(PLUTO,PLUTO_PWD, OP_CODE.LOGIN,null,null,null);
        server.reciveMessage(request);
        request = new Message(TOPOLINO,TOPOLINO_PWD, OP_CODE.LOGIN,null,null,null);
        server.reciveMessage(request);

        //LIST USERS
        request = new Message(PIPPO,null,OP_CODE.LIST_USER,null,null,null);
        server.reciveMessage(request);

        //CREATE PROJECTS
        request = new Message(PIPPO,null,OP_CODE.CREATE_PROJECT,PROGETTO,null,null);
        server.reciveMessage(request);
        sleep(100);
        request = new Message(PLUTO,null,OP_CODE.CREATE_PROJECT,PROGETTO,null,null);
        server.reciveMessage(request);

        sleep(100);
        request = new Message(PIPPO,null,OP_CODE.LIST_PROJECTS,null,null,null);
        server.reciveMessage(request);

        //ADD MEMBER
        request = new Message(PIPPO,PLUTO,OP_CODE.ADD_MEMBER,PROGETTO,null,null);
        server.reciveMessage(request);

        request = new Message(TOPOLINO,PLUTO,OP_CODE.ADD_MEMBER,PROGETTO,null,null);
        server.reciveMessage(request);

        //ADD CARD
        request = new Message(PIPPO,"Qualcosa che dice di qualcos'altro.",OP_CODE.ADD_CARD,PROGETTO,"CARD1",null);
        server.reciveMessage(request);

        sleep(100);
        //SHOW CARDS LIST
        request = new Message(PIPPO,null,OP_CODE.SHOW_CARD_LIST,PROGETTO,null,null);
        server.reciveMessage(request);

        request = new Message(TOPOLINO,null,OP_CODE.SHOW_CARD_LIST,PROGETTO,null,null);
        server.reciveMessage(request);

        //SHOW CARD
        request = new Message(PIPPO,"TODO",OP_CODE.SHOW_CARD,PROGETTO,"CARD1",null);
        server.reciveMessage(request);
        request = new Message(TOPOLINO,"TODO",OP_CODE.SHOW_CARD,PROGETTO,"CARD1",null);
        server.reciveMessage(request);
        request = new Message(PIPPO,"TODO",OP_CODE.SHOW_CARD,PROGETTO,"CARD",null);
        server.reciveMessage(request);


        //MOVE CARD
        request = new Message(PIPPO, CARD_STATUS.TODO+"->"+CARD_STATUS.INPROGRESS,OP_CODE.MOVE_CARD,PROGETTO,"CARD1",null);
        server.reciveMessage(request);


        sleep(100);
        request = new Message(PIPPO,null,OP_CODE.SHOW_CARD_LIST,PROGETTO,null,null);
        server.reciveMessage(request);

        request = new Message(PIPPO,CARD_STATUS.INPROGRESS.toString(),OP_CODE.GET_CARD_HISTORY,PROGETTO,"CARD1",null);
        server.reciveMessage(request);

        request = new Message(PIPPO,null,OP_CODE.LIST_ONLINE_USER,null,null,null);
        server.reciveMessage(request);

        request = new Message(PIPPO,null,OP_CODE.SHOW_MEMBERS,PROGETTO,null,null);
        server.reciveMessage(request);
        request = new Message(TOPOLINO,null,OP_CODE.SHOW_MEMBERS,PROGETTO,null,null);
        server.reciveMessage(request);

        request = new Message(PIPPO,null,OP_CODE.LOGOUT,null,null,null);
        server.reciveMessage(request);

        sleep(100);
        request = new Message(PIPPO,null,OP_CODE.LIST_ONLINE_USER,null,null,null);
        server.reciveMessage(request);

        server.shutServerDown();

    }
}
