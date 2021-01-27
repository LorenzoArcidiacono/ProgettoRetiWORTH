package com.github.arci0066.worth.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsersList {
    private static UsersList instance;
    List<User> usersList;
    ReadWriteLock lock;


// ------ Constructors ------

    private UsersList() {
        usersList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
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
        return "UsersList{" +
                "usersList=" + usersList.toString() +
                '}';
    }
}
