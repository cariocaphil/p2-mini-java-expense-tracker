package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;

import java.math.BigDecimal;
import java.util.List;

/** Statistics of the history and what remains of the income once average expenses are paid. */
record MonthlyBudget(ExpenseStatistics statistics, BigDecimal averageExpenses, BigDecimal monthlySavings) {

    static MonthlyBudget of(ExpenseService expenseService, BigDecimal monthlyIncome, List<Expense> expenses) {
        ExpenseStatistics stats = expenseService.computeStatistics(expenses);
        BigDecimal averageExpenses = stats.averageMonthlyExpenses();
        return new MonthlyBudget(stats, averageExpenses, monthlyIncome.subtract(averageExpenses));
    }

    static void requirePositiveIncome(BigDecimal monthlyIncome) {
        if (monthlyIncome == null) {
            throw new IllegalArgumentException("Monthly income must not be null.");
        }
        if (monthlyIncome.signum() <= 0) {
            throw new IllegalArgumentException("Monthly income must be greater than zero.");
        }
    }
}
