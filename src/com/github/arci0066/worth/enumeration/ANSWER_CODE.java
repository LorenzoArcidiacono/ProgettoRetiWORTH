package com.github.arci0066.worth.enumeration;

// Possibili risposte ricevute dal server
public enum ANSWER_CODE {
    OP_OK {
        @Override
        public String toString() {
            return "Operazione eseguita correttamente.";
        }
    },
    OP_FAIL{
        @Override
        public String toString() {
            return "Errore durante l'operazione";
        }
    },
    UNKNOWN_PROJECT{
        @Override
        public String toString() {
            return "Progetto inesistente.";
        }
    },
    UNKNOWN_USER{
        @Override
        public String toString() {
            return "Utente non riconosciuto.";
        }
    },
    UNKNOWN_CARD{
        @Override
        public String toString() {
            return "Card insesistente.";
        }
    },
    WRONG_PASSWORD{
        @Override
        public String toString() {
            return "Password errata";
        }
    },
    EXISTING_USER{
        @Override
        public String toString() {
            return "Nickname precedentemente registrato.";
        }
    },
    EXISTING_PROJECT{
        @Override
        public String toString() {
            return "Progetto precedentemente registrato.";
        }
    },
    PERMISSION_DENIED{
        @Override
        public String toString() {
            return "Permesso negato.";
        }
    },
    PROJECT_NOT_FINISHED{
        @Override
        public String toString() {
            return "Progetto non finito.";}
    },
    WRONG_LIST{
        @Override
        public String toString() {
            return "Passaggio tra le liste scelte non permesso.";
        }
    };

}
