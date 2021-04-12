package com.github.arci0066.worth.server;

public class ServerSettings {

    //-------------- Thread ------------
    public static final int MIN_THREAD_NUMBER = 4;
    public static final int MAX_THREAD_NUMBER = 10;
    public static final long THREAD_KEEP_ALIVE_TIME = 60L;

    // --------- Serializzazione -----------
    public static final int maxUnsavedOperation = 10; // TODO: 12/04/21 capire meglio sto numero
    public static final String serverBackupDirPath = "./extra/Server_Backup";
    public static final String usersBackupFile = "./extra/Server_Backup/Users.bkp";
    public static final String projectsBackupFile = "./extra/Server_Backup/Projects.bkp";

    // TODO: 04/03/21 impedire che l'utente usi nomi o pwd con : e , o cambiare uso 
    // TODO: 04/03/21 sembra che : e , non saranno mai usati del encoder https://www.base64decode.org/
    public static final String usersDivider = ":";
    public static final String usersDataDivider = ",";

    //---------- Messaggi ----------
    public static final String MESSAGE_TERMINATION_CODE = "END";

    //---------------- Connessioni ---------------
    public static final int SERVER_PORT = 65535;
    public static final int REGISTRY_PORT = 65534;
    public static final String REGISRTY_OP_NAME = "SERVER_REGISTRATION";

}
