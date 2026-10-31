package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseServiceStatisticsTest {
    private final ExpenseService service = new ExpenseService(null);

    private static Expense expense(int year, int month, int day, String category, String amount) {
        return new Expense(LocalDate.of(year, month, day), category, "test", new BigDecimal(amount));
    }

    private static void assertAmount(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void emptyHistoryHasNoTotalNoMonthsNoCategories() {
        ExpenseStatistics stats = service.computeStatistics(List.of());

        assertAmount("0", stats.overallTotal());
        assertEquals(0, stats.monthsCovered());
        assertTrue(stats.categories().isEmpty());
    }

    @Test
    void singleMonthAverageEqualsTotal() {
        ExpenseStatistics stats = service.computeStatistics(List.of(
                expense(2024, 1, 5, "food", "10.00"),
                expense(2024, 1, 20, "food", "5.50")));

        assertEquals(1, stats.monthsCovered());
        assertAmount("15.50", stats.categories().get(0).total());
        assertAmount("15.50", stats.categories().get(0).monthlyAverage());
    }

    @Test
    void monthWithoutExpensesInsideTheSpanIsCounted() {
        List<Expense> expenses = List.of(
                expense(2024, 1, 10, "food", "10"),
                expense(2024, 3, 10, "food", "10"));

        assertEquals(3, service.countMonthsCovered(expenses));
    }

    @Test
    void spanAcrossYears() {
        List<Expense> expenses = List.of(
                expense(2023, 11, 30, "food", "10"),
                expense(2024, 2, 1, "food", "10"));

        assertEquals(4, service.countMonthsCovered(expenses));
    }

    @Test
    void categoryPresentInOneMonthIsDividedByWholeSpan() {
        ExpenseStatistics stats = service.computeStatistics(List.of(
                expense(2024, 1, 1, "housing", "100.00"),
                expense(2024, 4, 1, "food", "40.00")));

        assertEquals(4, stats.monthsCovered());
        CategoryStatistics housing = stats.categories().stream()
                .filter(c -> c.category().equals("housing")).findFirst().orElseThrow();
        assertAmount("25.00", housing.monthlyAverage());
    }

    @Test
    void totalsAreSummedAndRowsSortedByTotalDescending() {
        ExpenseStatistics stats = service.computeStatistics(List.of(
                expense(2024, 1, 1, "food", "10.00"),
                expense(2024, 1, 2, "housing", "500.00"),
                expense(2024, 1, 3, "food", "15.00"),
                expense(2024, 1, 4, "transport", "2.50")));

        assertAmount("527.50", stats.overallTotal());
        assertEquals(List.of("housing", "food", "transport"),
                stats.categories().stream().map(CategoryStatistics::category).toList());
        assertAmount("25.00", stats.categories().get(1).total());
    }

    @Test
    void averageIsRoundedHalfUpToTwoDecimals() {
        ExpenseStatistics stats = service.computeStatistics(List.of(
                expense(2024, 1, 1, "food", "100.00"),
                expense(2024, 3, 1, "other", "0.00")));

        assertEquals(3, stats.monthsCovered());
        CategoryStatistics food = stats.categories().stream()
                .filter(c -> c.category().equals("food")).findFirst().orElseThrow();
        assertAmount("33.33", food.monthlyAverage());
    }

    @Test
    void blankCategoryIsGroupedAsUncategorized() {
        ExpenseStatistics stats = service.computeStatistics(List.of(
                expense(2024, 1, 1, "", "9.90"),
                expense(2024, 1, 2, "  ", "0.10")));

        assertEquals(1, stats.categories().size());
        assertEquals(ExpenseService.UNCATEGORIZED, stats.categories().get(0).category());
        assertAmount("10.00", stats.categories().get(0).total());
    }
}
