/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;

import org.junit.jupiter.engine.discovery.predicates.IsPotentialTestContainer;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;


public class Leader extends Thread {
    private SocketList socketList;
    private ThreadPoolExecutor pool;
    private int countOperation = 0;

    // ------ Constructors ------
    public Leader(ThreadPoolExecutor pool) {
        socketList = SocketList.getSingletonInstance();
        this.pool = pool;
    }

    // TODO: 26/02/21 salvare tutto prima di uscire
    @Override
    public void run() {
        while (true) { // TODO: 27/01/21 posso impostare un timeout? se ho pochi client giro molto a vuoto, posso cambiarlo in base al lavoro del pool o al numero di client 
            synchronized (socketList) { // TODO Non esco mai e il server non può aggiungere?
                //ogni maxUnsavedOperation faccio un backup in memoria di tutte le informazioni del server
                /*if (countOperation >= maxUnsavedOperation) { // TODO: 09/04/21 salvare in base al numero di utenti attivi?
                    countOperation = 0;
                    backupServerStatus();
                }*/
                //Valuto se è necessario salvare e se è un buon momento
                if ((pool.getActiveCount() <= ServerSettings.THREAD_SAFE_NUMBER && countOperation >= ServerSettings.SAFE_UNSAVED_OPERATION)
                        || countOperation >= ServerSettings.MAX_UNSAVED_OPERATION){
                    countOperation = 0;
                    backupServerStatus();
                }
                    Iterator<Connection> iterator = socketList.iterator();
                while (iterator.hasNext()) { //scorro la lista delle connessioni
                    Connection connection = iterator.next();
                    try {
                        if (!connection.isClosed() && connection.isReaderReady() && !connection.isInUse()) {
                            // se la connessione è pronta a scrivere creo un task che gestisca l'operazione
                            System.out.println("Leader: " + socketList);
                            // setto la connessione come servita per evitare di avviare più task
                            connection.setInUse(true);
                            pool.execute(new Task(connection));
                            countOperation++; // TODO: 12/04/21 valutare se conviene contare solo le op. di scrittura
                            System.out.println("Leader: ActiveThread:"+pool.getActiveCount()+", Operation:"+countOperation);
                        }
                        if (connection.isClosed()) {
                            // se la connessione è chiusa la rimuovo dalla lista
                            System.out.println("Connessione chiusa:" + connection.getSocket());
                            connection.close();
                            iterator.remove();
                            System.out.println("Leader removing: " + socketList);
                        }
                        // TODO: 26/01/21 aggiungere caso connessione chiusa
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void backupServerStatus() { // TODO: 26/02/21 posso mettere il codice direttamente
        // avvio un task per il backup
        pool.execute(new BackupTask());
    }
}
