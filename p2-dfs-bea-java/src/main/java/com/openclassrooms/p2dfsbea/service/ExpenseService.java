package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CarPurchaseSimulation;
import com.openclassrooms.p2dfsbea.model.CategoryStatistics;
import com.openclassrooms.p2dfsbea.model.Expense;
import com.openclassrooms.p2dfsbea.model.ExpenseStatistics;
import com.openclassrooms.p2dfsbea.model.MortgageSimulation;
import com.openclassrooms.p2dfsbea.repository.ExpenseRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class ExpenseService {
    public static final String UNCATEGORIZED = "(uncategorized)";
    static final String HOUSING_CATEGORY = "housing";
    public static final int MAX_LOAN_DURATION_YEARS = 50;
    public static final BigDecimal MAX_ANNUAL_RATE_PERCENT = new BigDecimal("100");
    static final BigDecimal DEBT_RATIO = new BigDecimal("0.35");

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

    public CarPurchaseSimulation simulateCarPurchase(BigDecimal monthlyIncome, BigDecimal carPrice) {
        return simulateCarPurchase(monthlyIncome, carPrice, loadAllExpenses());
    }

    /**
     * Estimates how many months are needed to save the car price, starting from zero savings.
     * Months required are empty when monthly savings are zero or negative.
     * monthlySavings = monthlyIncome - average monthly expenses; monthsRequired = ceil(carPrice / monthlySavings).
     * Without any expense, average expenses are taken as 0.
     *
     * @throws IllegalArgumentException if the income or the car price is not strictly positive
     */
    public CarPurchaseSimulation simulateCarPurchase(BigDecimal monthlyIncome, BigDecimal carPrice,
                                                     List<Expense> expenses) {
        requirePositiveIncome(monthlyIncome);
        if (carPrice.signum() <= 0) {
            throw new IllegalArgumentException("Car price must be greater than zero.");
        }
        MonthlyBudget budget = computeMonthlyBudget(monthlyIncome, expenses);
        BigDecimal averageExpenses = budget.averageExpenses();
        BigDecimal monthlySavings = budget.monthlySavings();
        Optional<Long> monthsRequired = Optional.empty();
        if (monthlySavings.signum() > 0) {
            monthsRequired = Optional.of(carPrice.divide(monthlySavings, 0, RoundingMode.CEILING).longValueExact());
        }
        return new CarPurchaseSimulation(monthlyIncome, averageExpenses, monthlySavings, carPrice,
                monthsRequired, !expenses.isEmpty());
    }

    public MortgageSimulation simulateMortgage(BigDecimal monthlyIncome, BigDecimal annualRatePercent,
                                               int durationYears) {
        return simulateMortgage(monthlyIncome, annualRatePercent, durationYears, loadAllExpenses());
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
        requirePositiveIncome(monthlyIncome);
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
        MonthlyBudget budget = computeMonthlyBudget(monthlyIncome, expenses);
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

    private static void requirePositiveIncome(BigDecimal monthlyIncome) {
        if (monthlyIncome == null) {
            throw new IllegalArgumentException("Monthly income must not be null.");
        }
        if (monthlyIncome.signum() <= 0) {
            throw new IllegalArgumentException("Monthly income must be greater than zero.");
        }
    }

    /** Statistics of the history and what remains of the income once average expenses are paid. */
    private record MonthlyBudget(ExpenseStatistics statistics, BigDecimal averageExpenses, BigDecimal monthlySavings) {
    }

    private MonthlyBudget computeMonthlyBudget(BigDecimal monthlyIncome, List<Expense> expenses) {
        ExpenseStatistics stats = computeStatistics(expenses);
        BigDecimal averageExpenses = stats.averageMonthlyExpenses();
        return new MonthlyBudget(stats, averageExpenses, monthlyIncome.subtract(averageExpenses));
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
