/*
*
* @Author Lorenzo Arcidiacono
* @Mail l.arcidiacono1@studenti.unipi.it
* @Matricola 534235
*
*/
package com.github.arci0066.worth.enumeration;


// Codici delle operazioni che possono essere richieste al server
public enum OP_CODE {
    LOGIN,
    LOGOUT,
    LIST_USER,
    LIST_ONLINE_USER,
    LIST_PROJECTS,
    CREATE_PROJECT,
    ADD_MEMBER,
    SHOW_MEMBERS,
    SHOW_PROJECT_CARDS,
    SHOW_CARD,
    ADD_CARD,
    MOVE_CARD,
    GET_CARD_HISTORY,
    CANCEL_PROJECT,
    CLOSE_CONNECTION,
    GET_CHAT_HST,
    GET_PRJ_CHAT;
}
