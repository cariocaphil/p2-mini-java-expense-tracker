package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseService {
    public static final String UNCATEGORIZED = "(uncategorized)";

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public List<Expense> loadAllExpenses() {
        return repository.readAll();
    }

    public void addExpense(Expense expense) throws IOException {
        repository.append(expense);
    }

    public ExpenseStatistics computeStatistics() {
        return computeStatistics(loadAllExpenses());
    }

    public ExpenseStatistics computeStatistics(List<Expense> expenses) {
        long months = countMonthsCovered(expenses);
        BigDecimal overallTotal = sumAmounts(expenses);
        List<CategoryStatistics> categories = buildCategoryStatistics(expenses, months);
        return new ExpenseStatistics(overallTotal, months, categories);
    }

    private BigDecimal sumAmounts(List<Expense> expenses) {
        return expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<CategoryStatistics> buildCategoryStatistics(List<Expense> expenses, long months) {
        return totalsByCategory(expenses).entrySet().stream()
                .map(entry -> new CategoryStatistics(entry.getKey(), entry.getValue(),
                        monthlyAverage(entry.getValue(), months)))
                .sorted(Comparator.comparing(CategoryStatistics::total).reversed()
                        .thenComparing(CategoryStatistics::category))
                .toList();
    }

    private Map<String, BigDecimal> totalsByCategory(List<Expense> expenses) {
        return expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> categoryLabel(e.getCategory()),
                        TreeMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)));
    }

    /**
     * Number of calendar months from the earliest to the latest expense, both included.
     * Months without any expense inside that span are counted. Returns 0 for an empty history.
     */
    private long countMonthsCovered(List<Expense> expenses) {
        if (expenses.isEmpty()) return 0;
        YearMonth first = expenses.stream().map(e -> YearMonth.from(e.getDate())).min(Comparator.naturalOrder()).get();
        YearMonth last = expenses.stream().map(e -> YearMonth.from(e.getDate())).max(Comparator.naturalOrder()).get();
        return ChronoUnit.MONTHS.between(first, last) + 1;
    }

    private static String categoryLabel(String category) {
        return category == null || category.isBlank() ? UNCATEGORIZED : category;
    }

    private static BigDecimal monthlyAverage(BigDecimal total, long months) {
        if (months == 0) return BigDecimal.ZERO.setScale(2);
        return total.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
    }
}
