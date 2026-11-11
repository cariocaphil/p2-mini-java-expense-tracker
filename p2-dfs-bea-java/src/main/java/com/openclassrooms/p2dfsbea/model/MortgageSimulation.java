package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;

/**
 * Result of a mortgage borrowing simulation.
 *
 * @param monthlySavings monthly income minus average monthly expenses; may be negative
 * @param monthlyHousingExpense average monthly amount of the "housing" category, 0 if none
 * @param desiredMonthlyPayment max(monthlySavings, 0) + monthlyHousingExpense, before the debt ceiling
 * @param debtCeiling 35 % of the monthly income
 * @param monthlyPayment min(desiredMonthlyPayment, debtCeiling), the payment used for the calculation
 * @param capped true when the desired payment exceeds the debt ceiling
 * @param annualRatePercent annual interest rate in percent (e.g. 3 for 3 %)
 * @param borrowingCapacity maximum borrowable amount, rounded to 2 decimals (HALF_UP)
 * @param hasExpenseHistory false when no expense exists, in which case expenses are taken as 0
 */
public record MortgageSimulation(BigDecimal monthlyIncome,
                                 BigDecimal averageMonthlyExpenses,
                                 BigDecimal monthlySavings,
                                 BigDecimal monthlyHousingExpense,
                                 BigDecimal desiredMonthlyPayment,
                                 BigDecimal debtCeiling,
                                 BigDecimal monthlyPayment,
                                 boolean capped,
                                 BigDecimal annualRatePercent,
                                 int durationYears,
                                 BigDecimal borrowingCapacity,
                                 boolean hasExpenseHistory) {
}
