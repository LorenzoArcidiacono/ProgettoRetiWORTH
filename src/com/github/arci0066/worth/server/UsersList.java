/*
*
* @Author Lorenzo Arcidiacono
* @Mail l.arcidiacono1@studenti.unipi.it
* @Matricola 534235
*
*/
package com.github.arci0066.worth.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class UsersList {
    private static UsersList instance; //instanza per rendere la classe singleton
    @Expose
    List<User> usersList;

    ReadWriteLock lock; // variabile di mutua esclusione


// ------ Constructors ------

    private UsersList() {
        usersList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
    }

    private UsersList(List<User> oldUserList) {
        lock = new ReentrantReadWriteLock();
        usersList = oldUserList;
        for (User u : usersList) {
            //alloco e sistemo le variabili che non sono salvate nel file di backup
            u.resetAfterBackup();
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

    public static UsersList getSingletonInstance(List<User> oldUsersList) {
        if (instance == null) {
            synchronized (UsersList.class) {
                if (instance == null)
                    instance = new UsersList(oldUsersList);
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


    // ----------- Serialization -----------
    public void saveAll() {
        Path path = Paths.get(ServerSettings.usersBackupFile);
        lock.readLock().lock();
        try (BufferedWriter writer = Files.newBufferedWriter(path, Charset.forName("UTF-8"))) {
            for (User user : usersList) {
                writer.write(user.getNickname() + ServerSettings.usersDataDivider + user.getPassword() + "\n" + ServerSettings.usersDivider);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void serialize() {
        try(FileOutputStream fos = new FileOutputStream(ServerSettings.serverBackupDirPath + "/Users.bkp");
            ObjectOutputStream out = new ObjectOutputStream(fos);) {
            out.writeObject(usersList);
        }
        catch(IOException ex) {ex.printStackTrace();}
    }

    public String jsonString() {
        Gson gson = new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        List<String> usersStatus = new ArrayList<>();
        for (User u: usersList) {
            usersStatus.add(u.getNickname()+" : "+u.getUserStatus());
        }
        return gson.toJson(usersStatus);
    }
}
