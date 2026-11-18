package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Result of a car purchase simulation, starting from zero savings.
 *
 * @param monthsRequired number of months needed to save the car price; empty when monthly savings are zero or negative,
 *                       i.e. the purchase cannot be financed through savings
 * @param hasExpenseHistory false when no expense exists, in which case average expenses are taken as 0
 */
public record CarPurchaseSimulation(BigDecimal monthlyIncome,
                                    BigDecimal averageMonthlyExpenses,
                                    BigDecimal monthlySavings,
                                    BigDecimal carPrice,
                                    Optional<Long> monthsRequired,
                                    boolean hasExpenseHistory) {
}
