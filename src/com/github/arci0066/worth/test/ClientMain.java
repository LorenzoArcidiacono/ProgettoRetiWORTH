import com.github.arci0066.worth.client.Client;
import com.github.arci0066.worth.server.Server;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class ClientMain {
    public static void main(String[] args) throws InterruptedException, IOException {

        /*Client c1 = new Client("/Users/lore/Documents/University/III_anno/Reti/Progetto_Reti_2021/extra/Test1");
        c1.start();
        Client c2 = new Client("/Users/lore/Documents/University/III_anno/Reti/Progetto_Reti_2021/extra/Test2");
        c2.start();
        Client c3 = new Client("/Users/lore/Documents/University/III_anno/Reti/Progetto_Reti_2021/extra/Test3");
        c3.start();*/
        Client c4 = new Client(null);
        c4.start();
        c4.join();
        System.out.println("Client uscito");
        System.err.println(c4.getState());
        System.err.println(Thread.currentThread().getState());
    }
}
