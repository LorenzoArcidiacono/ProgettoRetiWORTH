public class Message {
    private String senderNickname, senderPassword;
    private OP_CODE operationCode;
    private String projectTitle, cardTitle;
    //stringa da usare per la cardDescription o per i listTitle
    private String extra;

    //------------ Constructors -------------
    public Message(String senderNickname, String senderPassword, OP_CODE operationCode, String projectTitle, String cardTitle, String extra) {
        this.senderNickname = senderNickname;
        this.senderPassword = senderPassword;
        this.operationCode = operationCode;
        this.projectTitle = projectTitle;
        this.cardTitle = cardTitle;
        this.extra = extra;
    }

    // ------ Getters -------

    public String getSenderNickname() {
        return senderNickname;
    }

    public String getSenderPassword() {
        return senderPassword;
    }

    public OP_CODE getOperationCode() {
        return operationCode;
    }

    public String getProjectTitle() {
        return projectTitle;
    }

    public String getCardTitle() {
        return cardTitle;
    }

    public String getExtra() {
        return extra;
    }

    // ------ Methods ------
}
