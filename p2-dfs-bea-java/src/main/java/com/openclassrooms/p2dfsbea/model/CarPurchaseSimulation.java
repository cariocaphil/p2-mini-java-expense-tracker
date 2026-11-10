package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;

/**
 * Result of a car purchase simulation, starting from zero savings.
 *
 * @param monthsRequired number of months needed to save the car price; 0 when the purchase cannot be financed
 * @param hasExpenseHistory false when no expense exists, in which case average expenses are taken as 0
 */
public record CarPurchaseSimulation(BigDecimal monthlyIncome,
                                    BigDecimal averageMonthlyExpenses,
                                    BigDecimal monthlySavings,
                                    BigDecimal carPrice,
                                    long monthsRequired,
                                    boolean hasExpenseHistory) {

    /** The car can only be financed through savings if something is saved every month. */
    public boolean canBeFinanced() {
        return monthlySavings.signum() > 0;
    }
}
