package com.github.arci0066.worth.server;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*CLASSE SINGLETON*/
public class ProjectsList {
    private static ProjectsList instance;
    private List<Project> projectsList;

// ------ Constructors ------

    private ProjectsList() {
        projectsList = new ArrayList<>();
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
        for (Project prj : projectsList) {
            str += "\n* " + prj.getProjectTitle();
        }
        return str;
    }

// ------ Setters -------

    // ------ Methods ------
    public void add(Project project) {
        projectsList.add(project);
    }

    public void remove(Project prj) {
        projectsList.remove(prj);
    }

    public Project findProject(String projectTitle) {
        for (Project prj : projectsList) {
            if(prj.getProjectTitle().equals(projectTitle))
                return prj;
        }
        return null;
    }
}
