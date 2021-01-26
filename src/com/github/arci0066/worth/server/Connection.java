package com.github.arci0066.worth.server;

import java.io.*;
import java.net.Socket;

public class Connection {
    Socket socket;
    BufferedReader reader;
    BufferedWriter writer;

    // ------ Constructors ------
    public Connection(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
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

    // ------ Methods ------
    public boolean isReaderReady() throws IOException {
        return reader.ready();
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
}
