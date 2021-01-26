package com.github.arci0066.worth.server;

import java.io.IOException;
import java.util.concurrent.ThreadPoolExecutor;

public class Leader extends Thread {
    private SocketList socketList;
    ThreadPoolExecutor pool;

    // ------ Constructors ------
    public Leader(ThreadPoolExecutor pool) {
        socketList = SocketList.getSingletonInstance();
        this.pool = pool;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (socketList) { //Non esco mai e il server non può aggiungere?

                for (Connection connection : socketList) {
                    try {
                        if(!connection.isClosed() && connection.isReaderReady()){
                            System.out.println("Leade "+socketList);
                            pool.execute(new Task(connection));
                        }
                        // TODO: 26/01/21 aggiungere caso connessione chiusa
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
