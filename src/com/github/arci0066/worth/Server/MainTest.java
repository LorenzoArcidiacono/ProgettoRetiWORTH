public class MainTest {

    public static final String PROGETTO = "Progetto 1";

    public static void main(String[] args) {
        Server server = new Server();

        //Registro un po' di utenti
        System.out.println(server.register("Pippo", "Pippo1"));
        System.out.println(server.register("Pluto", "Pluto1"));

        //Stampo le liste
        System.out.println(server.listUsers());
        System.out.println(server.listOnlineUsers());

        //Provo il login
        System.out.println(server.login("Pippo", "Pippo1"));
        System.out.println(server.listUsers());
        System.out.println(server.listOnlineUsers());

        //Provo progetto
        System.out.println(server.createProject(PROGETTO, "Pluto"));
        System.out.println(server.showMembers("Progetto 1","Pluto"));
        System.out.println(server.showMembers("Progetto 1","Pippo"));

        System.out.println(server.addMember("Progetto 1","Pluto","Pippo"));
        System.out.println(server.showMembers("Progetto 1","Pippo"));
        System.out.println(server.showCards("Progetto 1", "Pippo"));
        server.addCard(PROGETTO,"Sommare","Sommare tutti i valori","Pippo");
        System.out.println(server.showCards(PROGETTO,"Pippo"));
        Card crd = server.showCard(PROGETTO,"Sommare","TODO","Pippo");
        crd = null;
        System.out.println(server.showCards(PROGETTO,"Pippo"));
        server.moveCard(PROGETTO,"Sommare","TODO","INPROGRESS","Pluto");
        System.out.println(server.showCards(PROGETTO,"Pippo"));
        System.out.println(
                server.showCard(PROGETTO,"Sommare","INPROGRESS","Pippo")
                .getCardHistory());
    }



}
