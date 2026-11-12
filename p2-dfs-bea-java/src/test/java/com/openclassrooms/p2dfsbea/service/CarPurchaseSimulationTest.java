package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CarPurchaseSimulation;
import com.openclassrooms.p2dfsbea.model.Expense;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarPurchaseSimulationTest {
    private final ExpenseService expenseService = new ExpenseService(null);
    private final CarSimulationService service = new CarSimulationService(expenseService);

    /** One month of history with 2200.00 of expenses: average monthly expenses = 2200.00. */
    private final List<Expense> history = List.of(expense(1, "2200.00"));

    private static Expense expense(int month, String amount) {
        return new Expense(LocalDate.of(2024, month, 10), "housing", "test", new BigDecimal(amount));
    }

    private CarPurchaseSimulation simulate(String income, String price) {
        return service.simulateCarPurchase(new BigDecimal(income), new BigDecimal(price), history);
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void standardCalculation() {
        CarPurchaseSimulation result = simulate("3000", "12000");

        assertAmount("2200.00", result.averageMonthlyExpenses());
        assertAmount("800.00", result.monthlySavings());
        assertEquals(Optional.of(15L), result.monthsRequired());
        assertTrue(result.hasExpenseHistory());
    }

    @Test
    void partialMonthIsRoundedUp() {
        assertEquals(Optional.of(16L), simulate("3000", "12100").monthsRequired()); // 15.125 -> 16
    }

    @Test
    void averageUsesTheWholeCalendarSpanIncludingEmptyMonths() {
        // January 2000 + March 1000 over 3 months covered = 1000.00 per month
        List<Expense> sparse = List.of(expense(1, "2000.00"), expense(3, "1000.00"));

        CarPurchaseSimulation result = service.simulateCarPurchase(
                new BigDecimal("1500"), new BigDecimal("2500"), sparse);

        assertAmount("1000.00", result.averageMonthlyExpenses());
        assertAmount("500.00", result.monthlySavings());
        assertEquals(Optional.of(5L), result.monthsRequired());
    }

    @Test
    void zeroMonthlySavingsCannotBeFinanced() {
        CarPurchaseSimulation result = simulate("2200", "12000");

        assertAmount("0.00", result.monthlySavings());
        assertEquals(Optional.empty(), result.monthsRequired());
    }

    @Test
    void negativeMonthlySavingsCannotBeFinanced() {
        CarPurchaseSimulation result = simulate("2000", "12000");

        assertAmount("-200.00", result.monthlySavings());
        assertEquals(Optional.empty(), result.monthsRequired());
    }

    @Test
    void carPriceSmallerThanOneMonthOfSavingsTakesOneMonth() {
        assertEquals(Optional.of(1L), simulate("3000", "500").monthsRequired());
    }

    @Test
    void emptyHistoryTreatsExpensesAsZero() {
        CarPurchaseSimulation result = service.simulateCarPurchase(
                new BigDecimal("3000"), new BigDecimal("12000"), List.of());

        assertFalse(result.hasExpenseHistory());
        assertAmount("0.00", result.averageMonthlyExpenses());
        assertAmount("3000", result.monthlySavings());
        assertEquals(Optional.of(4L), result.monthsRequired());
    }

    @Test
    void monthsRequiredBeyondLongRangeThrowsArithmeticException() {
        // 1e30 / 0.01 = 1e32 months, far above Long.MAX_VALUE (about 9.2e18): longValueExact() overflows.
        // Documents the current behavior; no upper bound is enforced.
        assertThrows(ArithmeticException.class, () -> service.simulateCarPurchase(
                new BigDecimal("0.01"), new BigDecimal("1E+30"), List.of()));
    }

    @Test
    void incomeMustBeStrictlyPositive() {
        assertThrows(IllegalArgumentException.class, () -> simulate("0", "12000"));
        assertThrows(IllegalArgumentException.class, () -> simulate("-1", "12000"));
    }

    @Test
    void carPriceMustBeStrictlyPositive() {
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "0"));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "-5"));
    }
}
