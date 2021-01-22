package com.github.arci0066.worth.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*CLASSE SINGLETON && THREAD SAFE*/
public class ProjectsList {
    private static ProjectsList instance;
    private List<Project> projectsList;
    ReadWriteLock lock;

// ------ Constructors ------

    private ProjectsList() {
        projectsList = new ArrayList<>();
        lock = new ReentrantReadWriteLock();
    }

    // ------ Getters -------
    /* Doppio controllo per evitare che troppi Thread aspettino la mutex,
     * secondo controllo per casi in cui un thread si blocchi nell' if prima di completare
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

// ------ Setters -------

    // ------ Methods ------
    public void add(Project project) {
        lock.writeLock().lock();
        try {
            projectsList.add(project);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void remove(Project project) {
        lock.writeLock().lock();
        try {
            projectsList.remove(project);
        } finally {
            lock.readLock().unlock();
        }
    }

    public Project findProject(String projectTitle) {
        Project project = null;

        lock.readLock().lock();
        try {
            for (Project prj : projectsList) {
                if (prj.getProjectTitle().equals(projectTitle))
                    project = prj;
            }
        }
        finally{
                lock.readLock().unlock();
            }
            return project;
        }
    }
