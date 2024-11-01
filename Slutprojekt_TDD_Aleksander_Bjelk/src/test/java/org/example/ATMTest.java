package org.example;

import org.example.exceptions.CardLockedException;
import org.example.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ATMTest {

    private Bank mockGritBank;
    private ATM atm;
    private Card testCard;

    @BeforeEach
    public void setUp() {
        mockGritBank = mock(Bank.class);
        atm = new ATM(mockGritBank);
        testCard = new Card("12345", "0315");

    }


    @Test
    @DisplayName("Kortintaget fungerar när kortet inte är spärrat")
    public void testCardNotFrozen() throws CardLockedException {

        when(mockGritBank.isCardFrozen("12345")).thenReturn(false);
        atm.insertCard(testCard);

        assertNotNull(atm, "Kortet borde vara insatt i ATM:en");
    }

    @Test
    @DisplayName("kortintaget misslyckas när kortet är spärrat")
    public void testCardFrozenATMNotAllowing() {

        when(mockGritBank.isCardFrozen("12345")).thenReturn(true);

        assertAll("Kortet ska inte funka om om det är spärrat",
                () -> assertThrows(CardLockedException.class, () -> atm.insertCard(testCard),
                        "ett undantag borde kastas när kortet är låst"),
                () -> assertNull(atm.checkBalance(), "borde vara null pga kortet inte är insatt")
        );
    }

    @Test
    @DisplayName("kontrollerar om kortet är fryst")
    public void testIsCardFrozen() {

        when(mockGritBank.isCardFrozen("12345")).thenReturn(true);
        assertTrue(atm.isCardLocked("12345"), "kortet borde vara låst");
    }

    @Test
    @DisplayName("pinkod lyckas")
    public void testCorrectPin() throws CardLockedException {

        atm.insertCard(testCard);
        when(mockGritBank.validatePin("12345", "0315")).thenReturn(true);

        assertTrue(atm.enterPin("0315"), "pinkoden var rätt");
    }


    //som ni kan se ger även "rätt(0315)" pinkod att det är fel pinkod. Det är för testet testar inte om pinkoden är rätt eller fel, utan hanteringen av en pinkod som är fel
    //så när man kör testet kommer det visa fyra gånger att testet misslyckades, därefter kommer det en till från testBlockCardIfThreeFailedAttemptsPincode, resultat:
    //    Fel pinkod. Försök kvar: 2
    //    Fel pinkod. Försök kvar: 2
    //    Fel pinkod. Försök kvar: 2
    //    Fel pinkod. Försök kvar: 2
    // de fyra över står för detta testet, dem två under står för: testBlockCardIfThreeFailedAttemptsPincode
    //    Fel pinkod. Försök kvar: 2
    //    Fel pinkod. Försök kvar: 1
    @ParameterizedTest
    @DisplayName("verifiering (som kommer misslyckas) av en hypotetisk fel pinkod")
    @ValueSource(strings = {"0000", "1111", "2222", "0315"})
    public void testEnterIncorrectPin(String incorrectPin) throws CardLockedException {
        atm.insertCard(testCard);
        when(mockGritBank.validatePin("12345", incorrectPin)).thenReturn(false);

        //kontrollerar att felaktiga pinkoder misslyckas
        assertFalse(atm.enterPin(incorrectPin), "pinkoden var fel");
    }

    @Test
    @DisplayName("saldo hämtas korrekt när kortet är insatt")
    public void testCheckBalance() throws CardLockedException {

        atm.insertCard(testCard);
        when(mockGritBank.getBalance("12345")).thenReturn(1000.0);


        //kontrollerar att saldot är korrekt och att getBalance anropas
        double balance = atm.checkBalance();
        assertAll("kontrollera saldo och anrop",
                () -> assertEquals(1000.0, balance, "saldo borde vara 1000 för insatt kort"),
                () -> verify(mockGritBank, times(1)).getBalance("12345")
        );
    }

    @Test
    @DisplayName("saldo hämtas inte när inget kort är insatt")
    public void testCheckBalanceNoCardInserted() {
        //testar att checkBalance returnerar null när inget kort är insatt.
        Double balance = atm.checkBalance();
        assertNull(balance, "borde returnera null när inget kort är insatt");
    }

    @Test
    @DisplayName("Insättning anropas korrekt")
    public void testDepositMoney() throws CardLockedException {

        atm.insertCard(testCard);
        when(mockGritBank.getBalance("12345")).thenReturn(500.0);


        atm.deposit(500.0);
        assertAll("kollar så att insättning hanteras korrekt",
                () -> verify(mockGritBank, times(1)).deposit("12345", 500.0),
                () -> assertEquals(500.0, atm.checkBalance(), "saldo borde vara 500.0 efter insättning")
        );
    }

    @Test
    @DisplayName("uttag lyckas när saldot räcker till")
    public void testSuccessfulWithdraw() throws CardLockedException, InsufficientFundsException {

        atm.insertCard(testCard);
        when(mockGritBank.getBalance("12345")).thenReturn(1000.0);

        //gör ett uttag och verifierar att både getBalance och withdraw anropas en gång avr
        atm.withdraw(500.0);
        assertAll("Kontrollera uttag och saldo",
                () -> verify(mockGritBank, times(1)).getBalance("12345"),
                () -> verify(mockGritBank, times(1)).withdraw("12345", 500.0)
        );
    }

    //testa med en parameter under 100 för att se att testet kommer misslyckas
    @ParameterizedTest
    @DisplayName("uttag misslyckas när saldot är otillräckligt för olika belopp")
    @ValueSource(doubles = {150.0, 200.0, 500.0})
    public void testWithdrawFailsWhenInsufficientFunds(double withdrawAmount) throws CardLockedException {

        atm.insertCard(testCard);
        when(mockGritBank.getBalance("12345")).thenReturn(100.0);

        //försöker göra uttag och verifierar att InsufficientFundsException kastas vid otillräckligt saldo.
        assertThrows(InsufficientFundsException.class, () -> atm.withdraw(withdrawAmount),
                "ett undantag borde kastas vid otillräckligt saldo för belopp: " + withdrawAmount);
        verify(mockGritBank, never()).withdraw("12345", withdrawAmount);
    }

    @Test
    @DisplayName("visa bankens namn")
    public  void testGetBankName() {
        try (MockedStatic<Bank> bankMockedStatic = Mockito.mockStatic(Bank.class)) {
            bankMockedStatic.when(Bank::getBankName).thenReturn("Mocked GritBank Name");


            String bankName = atm.getBankName();
            assertEquals("Mocked GritBank Name", bankName, "bankens namn borde vara 'Mocked GritBank Name'");
        }
    }

    @Test
    @DisplayName("avsluta sessionen")
    public void testEndSession() throws CardLockedException {
        //sätter in kortet och avslutar sessionen för att kontrollera att kortet tas ut
        atm.insertCard(testCard);
        atm.endSession();

        //kontrollerar att balansen är null efter att sessionen har avslutats
        assertNull(atm.checkBalance(), "kortet borde vara uttaget och sessionen avslutad");
    }

    @Test
    @DisplayName("bankens namn returneras korrekt från Bank.getBankName()")
    public void testStaticGetBankName() {
        assertEquals("Grit Bank", Bank.getBankName(), "bankens namn borde vara 'Grit Bank'");
    }

    @Test
    @DisplayName("Kortet låses efter tre felaktiga pinkoder en med loop")
    public void testBlockCardIfThreeFailedAttemptsPincode() throws CardLockedException {

        atm.insertCard(testCard);
        when(mockGritBank.validatePin("12345", "wrongpin")).thenReturn(false);

        //testar felaktiga Pinkodsförsök genom en loop. Sista försöket ska låsa kortet
        for (int i = 0; i < 2; i++) {
            assertDoesNotThrow(() -> atm.enterPin("wrongpin"));
        }
        assertThrows(CardLockedException.class, () -> atm.enterPin("wrongpin"),
                "Kortet borde vara låst efter tre felaktiga försök");

        //verifierar att lockCard anropas en gång
        verify(mockGritBank, times(1)).lockCard("12345");
    }

}

