package com.github.arci0066.worth.server;
import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.OP_CODE;


public class Message{
    private String senderNickname;
    private OP_CODE operationCode;
    private String projectTitle, cardTitle;
    private ANSWER_CODE answerCode;
    //stringa da usare per la cardDescription, per i listTitle e password
    private String extra;

    //------------ Constructors -------------
    public Message(String senderNickname, String extra, OP_CODE operationCode, String projectTitle, String cardTitle, ANSWER_CODE answer) {
        this.senderNickname = senderNickname;
        this.operationCode = operationCode;
        this.projectTitle = projectTitle;
        this.cardTitle = cardTitle;
        this.extra = extra;
        this.answerCode = answer;
    }

    // ------ Getters -------

    public String getSenderNickname() {
        return senderNickname;
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

    public ANSWER_CODE getAnswerCode() {
        return answerCode;
    }



    // ------------ Setters -----------

    //Usato dal server per settare e inviare la risposta
    public void setAnswer(ANSWER_CODE answer_code, String extra) {
        this.answerCode = answer_code;
        this. extra = extra;
    }


    // ------ Methods ------

    @Override
    public String toString() {
        return "Message{" +
                "senderNickname='" + senderNickname + '\'' +
                ", operationCode=" + operationCode +
                ", projectTitle='" + projectTitle + '\'' +
                ", cardTitle='" + cardTitle + '\'' +
                ", answerCode=" + answerCode +
                ", extra='" + extra + '\'' +
                '}';
    }


}
