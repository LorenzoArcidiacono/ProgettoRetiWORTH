package com.github.arci0066.worth.server;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/*CLASSE SINGLETON && THREAD SAFE*/
public class SocketList implements Iterable<Connection>{
    private static SocketList instance;
    private List<Connection> connectionsList;


    // ------ Constructors ------
    private SocketList() {
        connectionsList = Collections.synchronizedList(new ArrayList<>());
    }

    // ------ Getters -------
    public static SocketList getSingletonInstance() {
        if (instance == null) {
            synchronized (SocketList.class) {
                if (instance == null)
                    instance = new SocketList();
            }
        }
        return instance;
    }


// ------ Setters -------

    // ------ Methods ------
    public void add(Socket socket) throws IOException {
        connectionsList.add(new Connection(socket));
    }

    public void remove(Connection connection) throws IOException {
        // TODO: 26/01/21 devo chiudere il socket??
        if (connection != null)
            connection.close();
            connectionsList.remove(connection);
    }

    private Connection findBySocket(Socket socket) {
        synchronized (connectionsList) {
            for (Connection connection : connectionsList) {
                if (connection.getSocket().equals(socket))
                    return connection;
            }
        }
        return null;
    }

    @Override
    // TODO: 26/01/21 controllare che si faccia così 
    public Iterator<Connection> iterator() {
        return connectionsList.iterator();
    }

    @Override
    public String toString() {
        return "SocketList{" +
                "connectionsList=" + connectionsList +
                '}';
    }

}
