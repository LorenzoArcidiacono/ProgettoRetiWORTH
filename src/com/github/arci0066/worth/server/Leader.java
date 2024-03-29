/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */
package com.github.arci0066.worth.server;


import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;


public class Leader extends Thread {
    private final SocketList socketList;
    private ThreadPoolExecutor pool;
    private int countOperation = 0;
    private boolean exit = false;

    // ------ Constructors ------
    public Leader(ThreadPoolExecutor pool) {
        socketList = SocketList.getSingletonInstance();
        this.pool = pool;
    }

    @Override
    public void run() {
        int lastCountOp;
        while (!exit) {
            if(isInterrupted()) { //controlla se è stato inviato un segnale di interruzione e nel caso fa pulizia prima di chiudere
                System.out.println("Leader: ricevuto segnale di interruzione");
                socketList.clean();
                exit = true;
                continue;
            }
            //Valuto, in base ai thread attivi e al numero di op. dall'ultimo salvataggio, se sia necessario fare un backup e se è un buon momento
            if ((pool.getActiveCount() <= ServerSettings.THREAD_SAFE_NUMBER && countOperation >= ServerSettings.SAFE_UNSAVED_OPERATION)
                    || countOperation >= ServerSettings.MAX_UNSAVED_OPERATION) {
                countOperation = 0;
                pool.execute(new BackupTask()); //chiedo al pool di eseguire un task di backup
            }
            lastCountOp = countOperation;
            synchronized (socketList) {
                Iterator<Connection> iterator = socketList.iterator();
                while (iterator.hasNext()) { //scorro la lista delle connessioni
                    Connection connection = iterator.next();
                    try {
                        //Se la connessione non è chiusa, non è già servita ed ha inviato un messaggio
                        if (!connection.isClosed() && connection.isReaderReady() && !connection.isInUse()) {
                            // setto la connessione come servita per evitare di avviare più task
                            connection.setInUse(true);
                            // passo al pool un nuovo task per questa connessione
                            pool.execute(new Task(connection));
                            countOperation++;
                        }
                        if (connection.isClosed()) {
                            // se la connessione è chiusa la rimuovo dalla lista
                            System.out.println("Connessione chiusa:" + connection.getSocket());
                            connection.close();
                            iterator.remove();
                        }
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if(lastCountOp == countOperation){ //se non sono state svolte operazioni aspetto
                try {
                    Thread.sleep( ServerSettings.SLEEP_TIMEOUT);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }
    }
}

