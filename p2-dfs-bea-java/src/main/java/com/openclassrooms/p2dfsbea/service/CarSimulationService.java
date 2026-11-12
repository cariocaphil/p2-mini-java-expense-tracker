package com.openclassrooms.p2dfsbea.service;

import com.openclassrooms.p2dfsbea.model.CarPurchaseSimulation;
import com.openclassrooms.p2dfsbea.model.Expense;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/** Car purchase financing simulation, based on the expense statistics. */
public class CarSimulationService {
    private final ExpenseService expenseService;

    public CarSimulationService(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    public CarPurchaseSimulation simulateCarPurchase(BigDecimal monthlyIncome, BigDecimal carPrice) {
        return simulateCarPurchase(monthlyIncome, carPrice, expenseService.loadAllExpenses());
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
        MonthlyBudget.requirePositiveIncome(monthlyIncome);
        if (carPrice.signum() <= 0) {
            throw new IllegalArgumentException("Car price must be greater than zero.");
        }
        MonthlyBudget budget = MonthlyBudget.of(expenseService, monthlyIncome, expenses);
        BigDecimal averageExpenses = budget.averageExpenses();
        BigDecimal monthlySavings = budget.monthlySavings();
        Optional<Long> monthsRequired = Optional.empty();
        if (monthlySavings.signum() > 0) {
            monthsRequired = Optional.of(carPrice.divide(monthlySavings, 0, RoundingMode.CEILING).longValueExact());
        }
        return new CarPurchaseSimulation(monthlyIncome, averageExpenses, monthlySavings, carPrice,
                monthsRequired, !expenses.isEmpty());
    }
}
