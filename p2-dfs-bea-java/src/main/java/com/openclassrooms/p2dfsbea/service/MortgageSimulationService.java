package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.MortgageSimulation;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

/** Mortgage financing simulation, based on the expense statistics. */
public class MortgageSimulationService {
    static final String HOUSING_CATEGORY = "housing";
    public static final int MAX_LOAN_DURATION_YEARS = 50;
    public static final BigDecimal MAX_ANNUAL_RATE_PERCENT = new BigDecimal("100");
    static final BigDecimal DEBT_RATIO = new BigDecimal("0.35");

    private final ExpenseService expenseService;

    public MortgageSimulationService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public MortgageSimulation simulateMortgage(BigDecimal monthlyIncome, BigDecimal annualRatePercent,
                                               int durationYears) {
        return simulateMortgage(monthlyIncome, annualRatePercent, durationYears, expenseService.loadAllExpenses());
    }

    /**
     * Estimates the maximum borrowing amount.
     * <pre>
     * monthlySavings   = monthlyIncome - average monthly expenses
     * desiredPayment   = max(monthlySavings, 0) + average monthly "housing" expense
     * debtCeiling      = 35 % of monthlyIncome
     * monthlyPayment   = min(desiredPayment, debtCeiling)
     * capacity         = monthlyPayment * (1 - (1 + r)^-n) / r   (r = annual rate / 12, n = years * 12)
     *                  = monthlyPayment * n                      when the rate is 0
     * </pre>
     * Business rule: negative savings are floored at 0, so the payment never drops below the current housing
     * expense (the loan replaces the rent). Simplification: when expenses exceed income, this can overestimate
     * the real financial capacity.
     *
     * @throws IllegalArgumentException if the income or the rate is null, the income is not strictly positive, the rate is outside
     *         [0, {@link #MAX_ANNUAL_RATE_PERCENT} (100) %] or the duration is outside [1, {@value #MAX_LOAN_DURATION_YEARS}] years
     */
    public MortgageSimulation simulateMortgage(BigDecimal monthlyIncome, BigDecimal annualRatePercent,
                                               int durationYears, List<Expense> expenses) {
        MonthlyBudget.requirePositiveIncome(monthlyIncome);
        if (annualRatePercent == null) {
            throw new IllegalArgumentException("Interest rate must not be null.");
        }
        if (annualRatePercent.signum() < 0) {
            throw new IllegalArgumentException("Interest rate must not be negative.");
        }
        if (annualRatePercent.compareTo(MAX_ANNUAL_RATE_PERCENT) > 0) {
            throw new IllegalArgumentException("Interest rate must not exceed " + MAX_ANNUAL_RATE_PERCENT + " %.");
        }
        if (durationYears <= 0) {
            throw new IllegalArgumentException("Loan duration must be greater than zero.");
        }
        if (durationYears > MAX_LOAN_DURATION_YEARS) {
            throw new IllegalArgumentException("Loan duration must not exceed " + MAX_LOAN_DURATION_YEARS + " years.");
        }
        MonthlyBudget budget = MonthlyBudget.of(expenseService, monthlyIncome, expenses);
        BigDecimal averageExpenses = budget.averageExpenses();
        BigDecimal monthlySavings = budget.monthlySavings();
        BigDecimal housing = budget.statistics().categories().stream()
                .filter(c -> HOUSING_CATEGORY.equalsIgnoreCase(c.category()))
                .map(CategoryStatistics::monthlyAverage)
                .findFirst()
                .orElse(BigDecimal.ZERO);
        BigDecimal desiredPayment = monthlySavings.max(BigDecimal.ZERO).add(housing);
        BigDecimal debtCeiling = monthlyIncome.multiply(DEBT_RATIO);
        BigDecimal monthlyPayment = desiredPayment.min(debtCeiling);
        boolean capped = desiredPayment.compareTo(debtCeiling) > 0;

        int months = durationYears * 12;
        BigDecimal capacity = monthlyPayment.multiply(annuityFactor(annualRatePercent, months))
                .setScale(2, RoundingMode.HALF_UP);
        return new MortgageSimulation(monthlyIncome, averageExpenses, monthlySavings, housing, desiredPayment,
                debtCeiling, monthlyPayment, capped, annualRatePercent, durationYears, capacity,
                !expenses.isEmpty());
    }

    /** (1 - (1 + r)^-n) / r with r the monthly rate; n when r is 0. */
    private static BigDecimal annuityFactor(BigDecimal annualRatePercent, int months) {
        if (annualRatePercent.signum() == 0) {
            return BigDecimal.valueOf(months);
        }
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MathContext.DECIMAL128);
        BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(months, MathContext.DECIMAL128);
        BigDecimal discount = BigDecimal.ONE.divide(growth, MathContext.DECIMAL128);
        return BigDecimal.ONE.subtract(discount).divide(monthlyRate, MathContext.DECIMAL128);
    }
}
