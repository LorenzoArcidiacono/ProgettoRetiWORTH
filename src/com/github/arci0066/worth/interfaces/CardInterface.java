package com.github.arci0066.worth.interfaces;

import com.github.arci0066.worth.enumeration.ANSWER_CODE;
import com.github.arci0066.worth.enumeration.CARD_STATUS;

public interface CardInterface {
    //    ---------- Getters -------------
    /* SYNCHRONIZE:
     *   READ: title
     *   WRITE:
     */
    String getCardTitle();

    /* SYNCHRONIZE:
     *   READ: description
     *   WRITE:
     */
    String getCardDescription();

    /* SYNCHRONIZE:
     *   READ: status
     *   WRITE:
     */
    CARD_STATUS getCardStatus();

    /* SYNCHRONIZE:
     *   READ: history
     *   WRITE:
     */
    String getCardHistory();

    //    ----------- Methods ------------

    /* SYNCHRONIZE:
     *   READ:
     *   WRITE: status, history
     */
    ANSWER_CODE moveAndAdjournHistory(String userNickname, CARD_STATUS newCardStatus);

    /* SYNCHRONIZE:
     *   READ:
     *   WRITE: this
     */
    void empty();

    @Override
        /* SYNCHRONIZE:
         *   READ: this
         *   WRITE:
         */
    String toString();
}
