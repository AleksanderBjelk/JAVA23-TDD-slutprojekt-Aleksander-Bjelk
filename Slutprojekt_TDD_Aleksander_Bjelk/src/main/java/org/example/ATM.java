package org.example;


import org.example.exceptions.CardLockedException;
import org.example.exceptions.InsufficientFundsException;


public class ATM {
    private Bank bank;
    private Card currentCard;

    public ATM(Bank bank) {
        this.bank = bank;
        this.currentCard = null;
    }

    //kontrollera om ett kort är insatt, och visar ett meddelande om det inte är det
    private boolean isCardInserted() {
        if (currentCard == null) {
            System.out.println("Ditt kort är inte insatt eller så är det spärrat.");
            return false;
        }
        return true;
    }

    //kontrollera om ett kort är spärrat
    private boolean isCardFrozen() throws CardLockedException {
        if (bank.isCardFrozen(currentCard.getCardId())) {
            System.out.println("kortet är spärrat och kan inte användas");
            throw new CardLockedException("kortet är spärrat.");
        }
        return false;
    }

    //sätter in ett kort i ATM:en, kontrollerar om kortet är spärrat innan det sätts in
    public void insertCard(Card card) throws CardLockedException {
        if (bank.isCardFrozen(card.getCardId())) {
            throw new CardLockedException("Kortet är låst och kan inte användas.");
        }
        this.currentCard = card;
    }

    //kontrollerar pinkoden. Spärrar kortet efter tre felaktiga pinkodssförsök
    public boolean enterPin(String pin) throws CardLockedException {
        if (!isCardInserted()) return false;
        isCardFrozen();

        if (bank.validatePin(currentCard.getCardId(), pin)) {
            currentCard.resetFailedAttempts();
            return true;
        } else {
            currentCard.incrementFailedAttempts();
            bank.incrementFailedAttempts(currentCard.getCardId());

            int attemptsLeft = 3 - currentCard.getFailedAttempts();
            if (attemptsLeft <= 0) {
                bank.lockCard(currentCard.getCardId());
                System.out.println("Kortet är nu låst efter tre felaktiga försök.");
                throw new CardLockedException("Kortet är nu låst och kan inte användas.");
            } else {
                System.out.println("Fel pinkod. Försök kvar: " + attemptsLeft);
            }
            return false;
        }
    }

    //hämtar och returnerar saldo, eller null om inget kort är insatt
    public Double checkBalance() throws CardLockedException {
        if (!isCardInserted()) return null;
        isCardFrozen();
        return bank.getBalance(currentCard.getCardId());
    }

    //gör en insättning på kontot kopplat till det insatta kortet
    public void deposit(double amount) throws CardLockedException {
        if (!isCardInserted()) return;
        isCardFrozen();
        bank.deposit(currentCard.getCardId(), amount);
    }

    //gör ett uttag om saldot räcker. Annars kastas ett undantag
    public void withdraw(double amount) throws InsufficientFundsException, CardLockedException, IllegalArgumentException {
        if (!isCardInserted()) return;
        isCardFrozen();

        if (amount < 20) {
            throw new IllegalArgumentException("Minsta uttagsbelopp är 20 kronor.");
        }

        double balance = bank.getBalance(currentCard.getCardId());
        if (balance >= amount) {
            bank.withdraw(currentCard.getCardId(), amount);
        } else {
            throw new InsufficientFundsException("otillräckligt saldo för uttag.");
        }
    }

    //returnerar namnet på banken
    public String getBankName() {
        return Bank.getBankName();
    }

    //avslutar sessionen genom att ta bort det insatta kortet
    public void endSession() {
        this.currentCard = null;
        System.out.println("session avslutad. Vänligen ta ditt kort.");
    }

    //kontrollerar om ett specifikt kort är spärrat
    public boolean isCardLocked(String cardId) {
        return bank.isCardFrozen(cardId);
    }
}