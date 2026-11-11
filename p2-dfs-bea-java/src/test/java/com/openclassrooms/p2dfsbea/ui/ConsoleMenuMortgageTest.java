package com.openclassrooms.p2dfsbea.ui;

import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;
import com.openclassrooms.p2dfsbea.service.ExpenseService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuMortgageTest {
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream captured = new ByteArrayOutputStream();

    @BeforeEach
    void captureStdout() {
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private static Expense expense(String category, String amount) {
        return new Expense(LocalDate.of(2024, 1, 15), category, "test", new BigDecimal(amount));
    }

    private String run(List<Expense> history, String... inputLines) {
        ExpenseRepository stub = new ExpenseRepository() {
            @Override
            public List<Expense> readAll() {
                return history;
            }
        };
        String input = String.join("\n", inputLines) + "\n";
        new ConsoleMenu(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new ExpenseService(stub)).start();
        return captured.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    void menuListsMortgageAndQuitOptions() {
        String output = run(List.of(), "6");
        assertTrue(output.contains("5) Simulation prêt immobilier"));
        assertTrue(output.contains("6) Quit"));
    }

    @Test
    void cappedSimulationIsDisplayedInFrench() {
        List<Expense> history = List.of(expense("housing", "500"), expense("food", "500"));
        String output = run(history, "5", "3000", "12", "1", "6");
        assertTrue(output.contains("=== Simulation prêt immobilier ==="));
        assertTrue(output.contains("Dépense logement mensuelle : 500.00 €"));
        assertTrue(output.contains("Plafond d'endettement (35 % du revenu) : 1050.00 €"));
        assertTrue(output.contains("Plafond d'endettement atteint"));
        assertTrue(output.contains("Montant maximum empruntable : 11817.83 €"));
        assertFalse(output.contains("Épargne négative"));
    }

    @Test
    void commaDecimalsAreAccepted() {
        String output = run(List.of(), "5", "3000,00", "0,0", "1", "6");
        assertTrue(output.contains("Montant maximum empruntable : 12600.00 €"));
    }

    @Test
    void negativeSavingsShowAWarning() {
        List<Expense> history = List.of(expense("housing", "1000"), expense("food", "2500"));
        String output = run(history, "5", "3000", "12", "1", "6");
        assertTrue(output.contains("Épargne négative : ramenée à 0"));
        assertTrue(output.contains("Montant maximum empruntable : 11255.08 €"));
    }

    @Test
    void exactlyZeroSavingsShowNoWarning() {
        List<Expense> history = List.of(expense("housing", "1000"), expense("food", "2000"));
        String output = run(history, "5", "3000", "12", "1", "6");
        assertTrue(output.contains("Épargne mensuelle disponible : 0.00 €"));
        assertFalse(output.contains("Épargne négative"));
    }

    @Test
    void noPossiblePaymentIsExplained() {
        String output = run(List.of(expense("food", "1000")), "5", "1000", "12", "1", "6");
        assertTrue(output.contains("Aucune mensualité possible"));
        assertFalse(output.contains("Montant maximum empruntable"));
    }

    @Test
    void invalidIncomeIsRejected() {
        assertTrue(run(List.of(), "5", "abc", "6").contains("Montant invalide"));
    }

    @Test
    void zeroIncomeIsRejected() {
        assertTrue(run(List.of(), "5", "0", "6")
                .contains("Le revenu mensuel doit être strictement supérieur à zéro."));
    }

    @Test
    void negativeRateIsRejected() {
        assertTrue(run(List.of(), "5", "3000", "-1", "6").contains("Le taux d'intérêt ne peut pas être négatif."));
    }

    @Test
    void invalidRateIsRejected() {
        assertTrue(run(List.of(), "5", "3000", "abc", "6").contains("Taux invalide"));
    }

    @Test
    void zeroDurationIsRejected() {
        assertTrue(run(List.of(), "5", "3000", "3", "0", "6")
                .contains("La durée doit être strictement supérieure à zéro."));
    }

    @Test
    void nonIntegerDurationIsRejected() {
        assertTrue(run(List.of(), "5", "3000", "3", "2.5", "6").contains("Durée invalide"));
    }
}
