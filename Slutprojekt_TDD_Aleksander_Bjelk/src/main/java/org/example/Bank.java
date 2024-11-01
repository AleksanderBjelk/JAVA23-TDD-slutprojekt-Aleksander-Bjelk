package org.example;

public interface Bank {

    boolean validatePin(String cardId, String pinCode);

    void incrementFailedAttempts(String cardId);

    void lockCard(String cardId);

    boolean isCardFrozen(String cardId);

    double getBalance(String cardId);

    void deposit(String cardId, double amount);

    void withdraw(String cardId, double amount);

    static String getBankName() {
        return "Grit Bank";
    }
}
