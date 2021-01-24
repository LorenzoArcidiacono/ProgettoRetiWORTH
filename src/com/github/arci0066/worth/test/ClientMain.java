import com.github.arci0066.worth.client.Client;
import com.github.arci0066.worth.server.Server;

import java.io.IOException;

import static java.lang.Thread.sleep;

public class ClientMain {
    public static void main(String[] args) throws InterruptedException, IOException {

        Client c1 = new Client();
        c1.start();
    }

}
