package com.github.arci0066.worth.server;

import java.io.*;
import java.net.Socket;

public class Connection {
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;

    boolean inUse; //booleano per indicare se lo user collegato a questa connessione
                    // è servito da un Task Thread in questo momento

    // ------ Constructors ------
    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        inUse = false;
    }

    // ------ Getters -------
    public Socket getSocket() {
        return socket;
    }

    public BufferedReader getReader() {
        return reader;
    }

    public BufferedWriter getWriter() {
        return writer;
    }

    // ----- SETTER ------
    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    // ------ Methods ------

    /*
     * EFFECTS: controlla se lo stream reader è pronto per essere letto
     * RETURN: reader.ready()
    */
    public boolean isReaderReady() throws IOException {
        return reader.ready();
    }

    public boolean isInUse() {
        return inUse;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    @Override
    public String toString() {
        return "Connection{" +
                "socket=" + socket.getRemoteSocketAddress() +
                "is open:" + !socket.isClosed()+
                '}';
    }


    /*
     * EFFECTS: chiude il socket, il reader e il writer
    */
    public void close() throws IOException {
        socket.close();
        reader.close();
        writer.close();
    }
}
