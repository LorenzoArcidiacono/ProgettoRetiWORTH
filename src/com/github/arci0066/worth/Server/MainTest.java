public class MainTest {
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
        System.out.println(server.createProject("Progetto 1", "Pluto"));
        System.out.println(server.showMembers("Progetto 1","Pluto"));
        System.out.println(server.showMembers("Progetto 1","Pippo"));

        System.out.println(server.addMember("Progetto 1","Pluto","Pippo"));
        System.out.println(server.showMembers("Progetto 1","Pippo"));
    }



}
