package org.example;

public class Card {
    private String cardId;
    private String pinCode;
    private boolean locked;
    private int failedAttempts;

    public Card(String cardId, String pinCode) {
        this.cardId = cardId;
        this.pinCode = pinCode;
        this.locked = false;
        this.failedAttempts = 0;
    }

    public String getCardId() {
        return cardId;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }


    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }
}