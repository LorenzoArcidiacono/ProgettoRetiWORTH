package com.github.arci0066.worth.server;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;

import static com.github.arci0066.worth.server.ServerSettings.maxUnsavedOperation;

public class Leader extends Thread {
    private SocketList socketList;
    private ThreadPoolExecutor pool;
    private int countOperation = 0;

    // ------ Constructors ------
    public Leader(ThreadPoolExecutor pool) {
        socketList = SocketList.getSingletonInstance();
        this.pool = pool;
    }

    @Override
    public void run() {
        while (true) { // TODO: 27/01/21 posso impostare un timeout? se ho pochi client giro molto a vuoto, posso cambiarlo in base al lavoro del pool o al numero di client 
            synchronized (socketList) { // TODO Non esco mai e il server non può aggiungere?
                if(countOperation >= maxUnsavedOperation){
                    countOperation = 0;
                    backupServerStatus();
                }
                Iterator<Connection> iterator = socketList.iterator();
                while (iterator.hasNext()){
                    Connection connection = iterator.next();
                    try {
                        if(!connection.isClosed() && connection.isReaderReady() && !connection.isInUse()){
                            System.out.println("Leader: "+socketList);
                            connection.setInUse(true);
                            pool.execute(new Task(connection));
                            countOperation++;
                        }
                        if (connection.isClosed()){
                            System.out.println("Connessione chiusa:"+connection.getSocket());
                            connection.close();
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

    private void backupServerStatus() {
    }
}
