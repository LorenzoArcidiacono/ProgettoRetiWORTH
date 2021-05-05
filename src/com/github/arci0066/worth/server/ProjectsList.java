/*
*
* @Author Lorenzo Arcidiacono
* @Mail l.arcidiacono1@studenti.unipi.it
* @Matricola 534235
*
*/
package com.github.arci0066.worth.server;
import com.github.arci0066.worth.enumeration.ANSWER_CODE;

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
    private static ProjectsList instance; //instanza della projectsList per il costruttore singleton
    private List<Project> projectsList;
    ReadWriteLock lock; //variabili di mutua esclusione

    private static String multicastIpPrefix = "239.0.0."; //prefisso per l'ip multicast, non cambia
    private Integer lastUsedIP; //suffisso per l' indirizzo ip multicast
    private int lastUsedPort; //porta per il multicast socket

// ------ Constructors ------

    private ProjectsList() {
        projectsList = new ArrayList<>();

        lastUsedIP = 0;
        lastUsedPort = ServerSettings.REGISTRY_PORT;

        lock = new ReentrantReadWriteLock();
    }

    private ProjectsList(List<Project> oldProjects) {
        lock = new ReentrantReadWriteLock();
        projectsList = oldProjects;

        lastUsedIP = 0;
        lastUsedPort = ServerSettings.REGISTRY_PORT;

        String suffix, address;

        for (Project p : projectsList) {
            //alloco e sistemo le variabili che non sono salvate nel file di backup
            suffix = (++lastUsedIP).toString();
            address = multicastIpPrefix + suffix;
            p.resetAfterBackup(address,--lastUsedPort);
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
     * EFFECTS: instanzia un oggetto singleton della classe nel caso di backup
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
    public void add(String projectTitle, String userNickname) {
        lock.writeLock().lock(); // TODO: 03/05/21 Serve questa lock? se aggiungo due progetti in contemporanea 
        String suffix = (++lastUsedIP).toString();
        String address = multicastIpPrefix + suffix;
        lock.writeLock().unlock();
        
        if(lastUsedIP < 0 || lastUsedIP > 255){ //indirizzi da 239.0.0.0 a 239.0.0.255
            System.err.println("Errore indirizzamento chat del progetto.");
            return;
        }

        System.out.println("Indirizzo chat del progetto"+projectTitle+":"+address+":"+(lastUsedPort-1));
        lock.writeLock().lock();
        try {
            projectsList.add(new Project(projectTitle,userNickname,address,--lastUsedPort));
        } finally {
            lock.writeLock().unlock();
        }
    }


    /*
     * REQUIRES: project != null
     * EFFECTS: rimuove il progetto alla lista se questo esiste ( Nota. tutti i controlli sono fatti dal chiamante )
     */
    public ANSWER_CODE remove(Project project, String userNickname) {
        //Salvo il path della cartella
        Path path = Paths.get(serverBackupDirPath + "/" + project.getProjectTitle());

        //Controllo di poter cancellare il progetto
        ANSWER_CODE answer = project.isCancellable(userNickname);

        if (answer == ANSWER_CODE.OP_OK){
            //cancello il progetto dalla lista e libero la memoria
            lock.writeLock().lock();
            try {
                projectsList.remove(project);
                project.cancelProject(userNickname);
            } finally {
                lock.writeLock().unlock();
            }
            //Cancello la cartella dal File System
            if(Files.isDirectory(path)){
                deleteDir(new File(String.valueOf(path)));
            }
        }
        return answer;
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        System.err.println(contents);
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }
            }
        }
        file.delete();
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
             ObjectOutputStream out = new ObjectOutputStream(fos)) {
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
                //salva le card del progetto
                prj.saveCard(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            lock.readLock().unlock();
        }
    }
}

