package com.github.arci0066.worth.server;

public class ServerSettings {

    //-------------- Thread ------------
    public static final int MIN_THREAD_NUMBER = 4; //numero minimo di thread nel pool
    public static final int MAX_THREAD_NUMBER = 10; //Numero massimo di thread nel pool
    public static final long THREAD_KEEP_ALIVE_TIME = 60L; //tempo prima che un thread nel pool venga eliminato

    // --------- Serializzazione -----------
    public static final int MAX_UNSAVED_OPERATION = 10; // TODO: 12/04/21 capire meglio sto numero
    public static final int THREAD_SAFE_NUMBER = 4; //indica pochi client e quindi un buon momento per salvare
    public static final int SAFE_UNSAVED_OPERATION = 6; //minimo numero di op. da salvare


    public static final String serverBackupDirPath = "./extra/Server_Backup"; // Path della cartella dei file di backup
    public static final String usersBackupFile = "./extra/Server_Backup/Users.bkp"; // path del file di backup utenti
    public static final String projectsBackupFile = "./extra/Server_Backup/Projects.bkp"; // path del file di backup dei progetti

    // TODO: 04/03/21 impedire che l'utente usi nomi o pwd con : e , o cambiare uso 
    // TODO: 04/03/21 sembra che : e , non saranno mai usati del encoder https://www.base64decode.org/
    public static final String usersDivider = ":";
    public static final String usersDataDivider = ",";

    //---------- Messaggi ----------
    public static final String MESSAGE_TERMINATION_CODE = "END"; // codice di terminazione di un messaggio sulla connessione TCP

    //---------------- Connessioni ---------------
    public static final int SERVER_PORT = 65535; // Porta del server
    public static final int REGISTRY_PORT = 65534; // Porta per la RMI
    public static final String REGISRTY_OP_NAME = "SERVER_REGISTRATION"; // Nome della RMI

    //IP multicast unassigned 233.252.1.32-233.252.1.255 o 239.0.0.0-239.255.255.255
    // ( https://www.iana.org/assignments/multicast-addresses/multicast-addresses.xhtml )

}
