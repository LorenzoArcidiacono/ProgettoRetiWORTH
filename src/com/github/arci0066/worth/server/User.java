package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.USER_STATUS;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*CLASSE THREAD SAFE*/
public class User {
    private String nickname, password;
    private USER_STATUS userStatus;
    private ReadWriteLock lock;
    //TODO descrittore della connessione

// ------ Constructors ------

    public User(String nickname, String password) {
        this.nickname = nickname;
        this.password = password;
        userStatus = USER_STATUS.OFFLINE;
        lock = new ReentrantReadWriteLock();
    }

    // ------ Getters -------
    public String getNickname() {
        String str;
        lock.readLock().lock();
        try {
            str = nickname;
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

// ------ Setters -------

    public void login() {
        lock.writeLock().lock();
        try {
            this.userStatus = USER_STATUS.ONLINE;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void logout() {
        lock.writeLock().lock();
        try {
            this.userStatus = USER_STATUS.OFFLINE;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // ------ Methods ------
    public boolean checkCredential(String nickname, String password) {
        boolean answer;
        lock.readLock().lock();
        try {
            answer = this.nickname.equals(nickname) && this.password.equals(password);
        } finally {
            lock.readLock().unlock();
        }
        return answer;
    }

    public boolean isOnline() {
        boolean answer;
        lock.readLock().lock();
        try {
            answer = userStatus.equals(USER_STATUS.ONLINE);
        } finally {
            lock.readLock().unlock();
        }
        return answer;
    }

    @Override
    public String toString() {
        String str;
        lock.readLock().lock();
        try {
            str = "com.github.arci0066.worth.User{" +
                    "nickname='" + nickname + '\'' +
                    ": " + userStatus +
                    '}';
        }
        finally {
            lock.readLock().unlock();
        }
        return str;
    }
}
