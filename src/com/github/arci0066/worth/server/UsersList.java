package com.github.arci0066.worth.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsersList {
    private static UsersList instance;
    @Expose
    List<User> usersList;
    ReadWriteLock lock;


// ------ Constructors ------

    private UsersList() {
        usersList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
    }

    private UsersList(String[] usersString) {
        lock = new ReentrantReadWriteLock();
        usersList = new ArrayList<>();
        String[] userData = null;
        for (String stringPart : usersString) {
            userData = stringPart.split(ServerSettings.usersDataDivider); //Separo nickname e password
            usersList.add(new User(userData[0], userData[1], true));
        }
    }

    // ------ Getters -------
    /* Doppio controllo per evitare che troppi Thread aspettino la mutex,
     * secondo controllo per casi in cui un thread si blocchi nell' if prima di completare
     */
    public static UsersList getSingletonInstance() {
        if (instance == null) {
            synchronized (UsersList.class) {
                if (instance == null)
                    instance = new UsersList();
            }
        }
        return instance;
    }

    public static UsersList getSingletonInstance(String[] usersString) {
        if (instance == null) {
            synchronized (UsersList.class) {
                if (instance == null)
                    instance = new UsersList(usersString);
            }
        }
        return instance;
    }

    public String getUsersNickname() {
        String str = "Utenti Registrati: ";
        lock.readLock().lock();
        try {
            for (User usr : usersList) {
                str += "\n* " + usr.getNickname();
            }
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

    public String getOnlineUsersNickname() {
        String str = "Utenti Online: ";
        lock.readLock().lock();
        try {
            for (User usr : usersList) {
                if (usr.isOnline())
                    str += "\n* " + usr.getNickname();
            }
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

    // ------ Methods ------
// TODO: 27/01/21 evitare duplicati
    public void add(User user) {
        lock.writeLock().lock();
        try {
            usersList.add(user);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public User findUser(String userNickname) {
        User user = null;
        lock.readLock().lock();
        try {
            for (User usr : usersList) {
                System.out.println("Trovato " + usr);
                if (usr.getNickname().equals(userNickname))
                    user = usr;
            }
        } finally {
            lock.readLock().unlock();
        }
        return user;
    }

    @Override
    public String toString() {
        return "UsersList{" + usersList.toString() +
                '}';
    }

    public void saveAll() {
        Path path = Paths.get(ServerSettings.serverBackupDirPath + "/Users.txt");
        lock.readLock().lock();
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            for (User user : usersList) {
                writer.write(user.getNickname() + ServerSettings.usersDataDivider + user.getPassword() + "\n" + ServerSettings.usersDivider);
                /*writer.write("Name:"+user.getNickname()+"\n");
                writer.write("Password:"+user.getPassword()+"\n");
                writer.write("@\n");*/
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }
}
