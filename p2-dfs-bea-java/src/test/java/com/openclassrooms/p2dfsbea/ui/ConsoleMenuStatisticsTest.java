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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleMenuStatisticsTest {
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

    private static Expense expense(int year, int month, String category, String amount) {
        return new Expense(LocalDate.of(year, month, 15), category, "test", new BigDecimal(amount));
    }

    /** Runs the menu with the given keystrokes (one line each) over an in-memory history. */
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
    void menuListsStatisticsAndQuitOptions() {
        String output = run(List.of(), "5");

        assertTrue(output.contains("3) Statistics"));
        assertTrue(output.contains("5) Quit"));
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void selectingStatisticsReturnsToTheMenuInsteadOfQuitting() {
        String output = run(List.of(), "3", "5");

        assertEquals(2, output.split("Choose an option: ", -1).length - 1);
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void unknownOptionIsRejected() {
        String output = run(List.of(), "9", "5");

        assertTrue(output.contains("Unknown option, please try again."));
    }

    @Test
    void emptyHistoryShowsMessageInsteadOfTable() {
        String output = run(List.of(), "3", "5");

        assertTrue(output.contains("No expenses recorded yet."));
        assertFalse(output.contains("Overall total"));
        assertFalse(output.contains("+-"));
    }

    @Test
    void statisticsShowsTotalsMonthsAndFormattedTable() {
        List<Expense> history = List.of(
                expense(2024, 1, "housing", "500.00"),
                expense(2024, 3, "food", "45.30"));

        String output = run(history, "3", "5");

        assertTrue(output.contains("Overall total: 545.30\n"));
        assertTrue(output.contains("Months covered: 3\n"));
        String expectedTable = String.join("\n",
                "+----------+--------+-----------------+",
                "| Category |  Total | Monthly average |",
                "+----------+--------+-----------------+",
                "| housing  | 500.00 |          166.67 |",
                "| food     |  45.30 |           15.10 |",
                "+----------+--------+-----------------+",
                "");
        assertTrue(output.contains(expectedTable), "table not found in:\n" + output);
    }

    @Test
    void categoryColumnGrowsToFitLongestCategoryName() {
        List<Expense> history = List.of(expense(2024, 1, "", "9.90"));

        String output = run(history, "3", "5");

        String expectedTable = String.join("\n",
                "+-----------------+-------+-----------------+",
                "| Category        | Total | Monthly average |",
                "+-----------------+-------+-----------------+",
                "| (uncategorized) |  9.90 |            9.90 |",
                "+-----------------+-------+-----------------+",
                "");
        assertTrue(output.contains(expectedTable), "table not found in:\n" + output);
    }

    @Test
    void carSimulationShowsFrenchSummary() {
        List<Expense> history = List.of(expense(2024, 1, "housing", "2200.00"));

        String output = run(history, "4", "3000", "12000", "5");

        assertTrue(output.contains("=== Simulation achat d'une voiture ==="));
        assertTrue(output.contains("Revenu mensuel net : 3000.00 €\n"));
        assertTrue(output.contains("Dépenses mensuelles moyennes : 2200.00 €\n"));
        assertTrue(output.contains("Épargne mensuelle disponible : 800.00 €\n"));
        assertTrue(output.contains("Prix de la voiture : 12000.00 €\n"));
        assertTrue(output.contains("Durée estimée : 15 mois\n"));
    }

    @Test
    void carSimulationAcceptsCommaDecimalSeparator() {
        String output = run(List.of(), "4", "3000,50", "1000", "5");

        assertTrue(output.contains("Revenu mensuel net : 3000.50 €"));
    }

    @Test
    void carSimulationWithInvalidNumberDoesNotCrash() {
        String output = run(List.of(), "4", "abc", "5");

        assertTrue(output.contains("Montant invalide"));
        assertTrue(output.contains("Goodbye"));
    }

    @Test
    void carSimulationRejectsZeroAmount() {
        String output = run(List.of(), "4", "3000", "0", "5");

        assertTrue(output.contains("Le prix de la voiture doit être strictement supérieur à zéro."));
        assertFalse(output.contains("Durée estimée"));
    }

    @Test
    void carSimulationExplainsWhenSavingsAreNotPositive() {
        List<Expense> history = List.of(expense(2024, 1, "housing", "2200.00"));

        String output = run(history, "4", "2000", "12000", "5");

        assertTrue(output.contains("Achat impossible par l'épargne"));
        assertFalse(output.contains("Durée estimée"));
    }

    @Test
    void carSimulationMentionsMissingExpenseHistory() {
        String output = run(List.of(), "4", "3000", "12000", "5");

        assertTrue(output.contains("Aucun historique de dépenses"));
        assertTrue(output.contains("Durée estimée : 4 mois"));
    }
}
