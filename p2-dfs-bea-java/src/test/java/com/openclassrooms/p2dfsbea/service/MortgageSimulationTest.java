package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.MortgageSimulation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Expected values were computed by hand: with a 12 % annual rate over 1 year (r = 0.01, n = 12),
 * (1 - 1.01^-12) / 0.01 = 11.2550775, so a 1000 payment borrows 11255.08 and a 1050 payment 11817.83.
 */
class MortgageSimulationTest {
    private final ExpenseService expenseService = new ExpenseService(null);
    private final MortgageSimulationService service = new MortgageSimulationService(expenseService);

    private static Expense expense(String category, String amount) {
        return new Expense(LocalDate.of(2024, 1, 10), category, "test", new BigDecimal(amount));
    }

    private MortgageSimulation simulate(String income, String rate, int years, Expense... expenses) {
        return service.simulateMortgage(new BigDecimal(income), new BigDecimal(rate), years, List.of(expenses));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void standardCalculationIsCappedAtThirtyFivePercent() {
        // average expenses 1000, savings 2000, housing 500 -> desired 2500, ceiling 1050
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "500"), expense("food", "500"));
        assertAmount("1000", r.averageMonthlyExpenses());
        assertAmount("2000", r.monthlySavings());
        assertAmount("500", r.monthlyHousingExpense());
        assertAmount("2500", r.desiredMonthlyPayment());
        assertAmount("1050", r.debtCeiling());
        assertAmount("1050", r.monthlyPayment());
        assertTrue(r.capped());
        assertAmount("11817.83", r.borrowingCapacity());
    }

    @Test
    void paymentBelowCeilingIsNotCapped() {
        // average 2500, savings 500, housing 500 -> desired 1000 < 1050
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "500"), expense("food", "2000"));
        assertAmount("1000", r.monthlyPayment());
        assertFalse(r.capped());
        assertAmount("11255.08", r.borrowingCapacity());
    }

    @Test
    void desiredPaymentExactlyAtCeilingIsNotCapped() {
        // average 2450, savings 550, housing 500 -> desired 1050 == ceiling
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "500"), expense("food", "1950"));
        assertAmount("1050", r.desiredMonthlyPayment());
        assertFalse(r.capped());
        assertAmount("11817.83", r.borrowingCapacity());
    }

    @Test
    void desiredPaymentAboveCeilingIsCapped() {
        // average 2400, savings 600, housing 500 -> desired 1100 > 1050
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "500"), expense("food", "1900"));
        assertAmount("1100", r.desiredMonthlyPayment());
        assertAmount("1050", r.monthlyPayment());
        assertTrue(r.capped());
    }

    @Test
    void zeroSavingsLeavesOnlyTheHousingExpense() {
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "1000"), expense("food", "2000"));
        assertAmount("0", r.monthlySavings());
        assertAmount("1000", r.monthlyPayment());
        assertAmount("11255.08", r.borrowingCapacity());
    }

    @Test
    void negativeSavingsAreFlooredAtZero() {
        // average 3500 > income 3000: savings -500 stay visible, but the payment is the rent only
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "1000"), expense("food", "2500"));
        assertAmount("-500", r.monthlySavings());
        assertAmount("1000", r.desiredMonthlyPayment());
        assertAmount("11255.08", r.borrowingCapacity());
    }

    @Test
    void missingHousingExpenseCountsAsZero() {
        MortgageSimulation r = simulate("3000", "12", 1, expense("food", "2000"));
        assertAmount("0", r.monthlyHousingExpense());
        assertAmount("1000", r.monthlyPayment());
        assertAmount("11255.08", r.borrowingCapacity());
    }

    @Test
    void noPaymentPossibleGivesZeroCapacity() {
        // savings 0 and no housing expense
        MortgageSimulation r = simulate("1000", "12", 1, expense("food", "1000"));
        assertAmount("0", r.monthlyPayment());
        assertAmount("0", r.borrowingCapacity());
    }

    @Test
    void emptyHistoryTreatsExpensesAsZero() {
        MortgageSimulation r = simulate("3000", "12", 1);
        assertFalse(r.hasExpenseHistory());
        assertAmount("3000", r.monthlySavings());
        assertAmount("1050", r.monthlyPayment());
        assertAmount("11817.83", r.borrowingCapacity());
    }

    @Test
    void zeroRateMultipliesPaymentByNumberOfMonths() {
        MortgageSimulation r = simulate("3000", "0", 1, expense("housing", "500"), expense("food", "2000"));
        assertAmount("12000", r.borrowingCapacity());
    }

    @Test
    void theSingleIncomeDrivesBothSavingsAndCeiling() {
        Expense[] history = {expense("housing", "500"), expense("food", "500")};
        MortgageSimulation low = simulate("1000", "12", 1, history);
        assertAmount("0", low.monthlySavings());
        assertAmount("350", low.debtCeiling());
        assertAmount("350", low.monthlyPayment());
        MortgageSimulation high = simulate("4000", "12", 1, history);
        assertAmount("3000", high.monthlySavings());
        assertAmount("1400", high.debtCeiling());
        assertAmount("1400", high.monthlyPayment());
    }

    @Test
    void capacityIsRoundedHalfUpToTwoDecimals() {
        // 1000 x 11.2550775... = 11255.0775... -> 11255.08
        MortgageSimulation r = simulate("3000", "12", 1, expense("housing", "1000"), expense("food", "2000"));
        assertEquals(new BigDecimal("11255.08"), r.borrowingCapacity());
    }

    @Test
    void longLoanUsesTheAnnuityFormula() {
        // 3 % over 20 years: factor 180.3109..., payment 1050 -> 189326.45 (hand check: 1050 x 180.31092)
        MortgageSimulation r = simulate("3000", "3", 20, expense("housing", "500"), expense("food", "500"));
        assertTrue(r.borrowingCapacity().subtract(new BigDecimal("189326.45")).abs()
                .compareTo(new BigDecimal("0.50")) < 0, "was " + r.borrowingCapacity());
    }

    @Test
    void housingCategoryMatchIsCaseInsensitive() {
        MortgageSimulation r = simulate("3000", "12", 1, expense("Housing", "500"), expense("food", "2000"));
        assertAmount("500", r.monthlyHousingExpense());
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> simulate("0", "3", 20));
        assertThrows(IllegalArgumentException.class, () -> simulate("-1", "3", 20));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "-0.1", 20));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "3", 0));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "3", -5));
    }

    @Test
    void nullIncomeIsRejectedWithAMeaningfulMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.simulateMortgage(null, new BigDecimal("3"), 20, List.of()));
        assertEquals("Monthly income must not be null.", e.getMessage());
    }

    @Test
    void nullRateIsRejectedWithAMeaningfulMessage() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.simulateMortgage(new BigDecimal("3000"), null, 20, List.of()));
        assertEquals("Interest rate must not be null.", e.getMessage());
    }

    @Test
    void rateAndDurationUpperBoundsAreEnforced() {
        assertDoesNotThrow(() -> simulate("3000", "100", 50));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "100.01", 20));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "1E+9", 20));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "3", 51));
        assertThrows(IllegalArgumentException.class, () -> simulate("3000", "3", Integer.MAX_VALUE));
    }
}
