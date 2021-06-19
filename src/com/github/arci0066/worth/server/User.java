/*
 *
 * @Author Lorenzo Arcidiacono
 * @Mail l.arcidiacono1@studenti.unipi.it
 * @Matricola 534235
 *
 */

package com.github.arci0066.worth.server;

import com.github.arci0066.worth.enumeration.USER_STATUS;
import com.google.gson.annotations.Expose;

import java.io.Serial;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*CLASSE THREAD SAFE*/
public class User implements Serializable{
    @Serial
    private static final long serialVersionUID = 1;
    @Expose
    private String nickname;
    private transient String password;
    private String encPwd; //password criptata per poter essere salvata in memoria
    @Expose
    private transient USER_STATUS userStatus;
    private transient ReadWriteLock lock; //Variabile di mutua esclusione

// ------ Constructors ------

    public User(String nickname, String password, boolean encrypted) {
        this.nickname = nickname;
        if(encrypted) {
            this.encPwd = password;
            this.password = decryptPassword();
        }
        else{
            this.password = password;
            this.encPwd = encryptPassword();
        }
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

    public String getPassword() {
        String str;
        lock.readLock().lock();
        try {
            str = encPwd;
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

    public USER_STATUS getUserStatus() {
        USER_STATUS status;
        lock.readLock().lock();
        try {
            status = userStatus;
        } finally {
            lock.readLock().unlock();
        }
        return status;
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

    /*
     * REQUIRES: @params != null
     * EFFECTS: controlla che il nickname e la password corrispondano
     * RETURN: true se corrispondono, false altrimenti
    */
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


    /*
     * RETURN: true se l'utente è online, false altrimenti
    */
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


    /*
     * RETURN: la password criptata
    */
    private String encryptPassword() {
        encPwd = Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8));
        return encPwd;
    }

    /*
     * RETURN: la password originale
     */
    private String decryptPassword() {
        return new String( Base64.getDecoder().decode(encPwd));
    }

    @Override
    public String toString() {
        String str;
        lock.readLock().lock();
        try {
            str = "{ nickname='" + nickname + '\'' +
                    ": " + userStatus +
                    '}';
        } finally {
            lock.readLock().unlock();
        }
        return str;
    }

    /*
     * EFFECTS: inizializza gli oggetti dopo che è stato creato un progetto a partire da un backup.
     */
    public void resetAfterBackup() {
        password = decryptPassword(); //restituisce la password reale
        userStatus = USER_STATUS.OFFLINE; //setto tutti gli utenti offline
        lock = new ReentrantReadWriteLock();
    }
}
