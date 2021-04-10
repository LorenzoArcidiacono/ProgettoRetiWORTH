/*
*
* @Author Lorenzo Arcidiacono
* @Mail l.arcidiacono1@studenti.unipi.it
* @Matricola 534235
*
*/
package com.github.arci0066.worth.server;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.github.arci0066.worth.server.ServerSettings.projectsBackupFile;
import static com.github.arci0066.worth.server.ServerSettings.serverBackupDirPath;

/*CLASSE SINGLETON && THREAD SAFE*/
public class ProjectsList  {
    private static ProjectsList instance;
    private List<Project> projectsList;
    ReadWriteLock lock;

// ------ Constructors ------

    private ProjectsList() {
        projectsList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
    }

    private ProjectsList(List<Project> oldProjects) {
        lock = new ReentrantReadWriteLock();
        projectsList = oldProjects;
        for (Project p : projectsList) {
            p.resetAfterBackup();
        }
    }

    // ------ Getters -------
    /* Doppio controllo per evitare che troppi Thread aspettino la mutex,
     * secondo controllo per casi in cui un thread si blocchi nell' if prima di completare
     */

    /*
     * EFFECTS: instanzia un oggetto singleton della classe
     * RETURN: l' istanza dell' oggetto
    */
    public static ProjectsList getSingletonInstance() {
        if (instance == null) {
            synchronized (ProjectsList.class) {
                if (instance == null)
                    instance = new ProjectsList();
            }
        }
        return instance;
    }

    /*
     * EFFECTS: instanzia un oggetto singleton della classe
     * RETURN: l' istanza dell' oggetto
     */
    public static ProjectsList getSingletonInstance(List<Project> oldProjects) {
        if (instance == null) {
            synchronized (ProjectsList.class) {
                if (instance == null)
                    instance = new ProjectsList(oldProjects);
            }
        }
        return instance;
    }


    /*
     * RETURN: una stringa con i titoli di tutti i progetti
     */
    public String getProjectsTitle() {
        String str = "Progetti:";
        lock.readLock().lock();
        try {
            for (Project prj : projectsList) {
                str += "\n* " + prj.getProjectTitle();
            }
        } finally {
            lock.readLock().unlock();
        }

        return str;
    }

    // ------ Methods ------

    /*
     * REQUIRES: project != null
     * EFFECTS: aggiunge il progetto alla lista se questo non è esistente ( Nota. tutti i controlli sono fatti dal chiamante )
     */
    public void add(Project project) {
        lock.writeLock().lock();
        try {
            projectsList.add(project);
        } finally {
            lock.writeLock().unlock();
        }
    }


    /*
     * REQUIRES: project != null
     * EFFECTS: rimuove il progetto alla lista se questo esiste ( Nota. tutti i controlli sono fatti dal chiamante )
     */
    public void remove(Project project) {
        lock.writeLock().lock();
        try {
            projectsList.remove(project);
        } finally {
            lock.readLock().unlock();
        }
    }


    /*
     * REQUIRES: projectTitle != null
     * EFFECTS: restituisce il progetto con titolo projectTitle se esite
     * RETURN: project tale che project.getProjectTitle == projectTitle, null altrimenti
     */
    public Project findProject(String projectTitle) {
        Project project = null;

        lock.readLock().lock();
        try {
            for (Project prj : projectsList) {
                if (prj.getProjectTitle().equals(projectTitle))
                    project = prj;
            }
        } finally {
            lock.readLock().unlock();
        }
        return project;
    }


    // ---------- Serialization ------------

    /*
     * EFFECTS: salva su un file di backup la lista dei progetti serializzata
    */
    public void serialize() {
        lock.readLock().lock();
        try (FileOutputStream fos = new FileOutputStream(projectsBackupFile);
             ObjectOutputStream out = new ObjectOutputStream(fos);) {
            out.writeObject(projectsList);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }

    /*
     * EFFECTS: salva in memoria la lista dei progetti: una cartella per ogni progetto, un file per ogni card
     */
    public void saveAll() {
        lock.readLock().lock();
        try {
            for (Project prj : projectsList) {
                // per ogni progetto crea una cartella e salva le card nella cartella
                Path path = Paths.get(serverBackupDirPath + "/" + prj.getProjectTitle());
                Files.createDirectories(path);
                prj.saveCard(path);
                //Path userListPath = Paths.get(path + projectUsersBackupFile);
                //prj.saveUsersList(userListPath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }
}

