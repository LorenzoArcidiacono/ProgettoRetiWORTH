package com.github.arci0066.worth.server;

import java.io.IOException;
import java.util.Iterator;
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
            synchronized (socketList) { // TODO Non esco mai e il server non può aggiungere?
                Iterator<Connection> iterator = socketList.iterator();
                while (iterator.hasNext()){
                    Connection connection = iterator.next();
                    try {
                        if(!connection.isClosed() && connection.isReaderReady() && !connection.isInUse()){
                            System.out.println("Leader: "+socketList);
                            connection.setInUse(true);
                            pool.execute(new Task(connection));
                        }
                        if (connection.isClosed()){
                            System.out.println("Connessione chiusa:"+connection.getSocket());
                            iterator.remove();
                            System.out.println("Leader removing: "+socketList);
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
