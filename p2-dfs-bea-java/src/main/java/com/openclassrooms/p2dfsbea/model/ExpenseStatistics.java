package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Overview of the expense history: overall total, number of months covered
 * (calendar span from the first to the last expense, inclusive) and one row per category.
 */
public record ExpenseStatistics(BigDecimal overallTotal, long monthsCovered, List<CategoryStatistics> categories) {

    /**
     * Average monthly expenses over the months covered (rounded half up to 2 decimals).
     * Returns 0.00 for an empty history.
     */
    public BigDecimal averageMonthlyExpenses() {
        if (monthsCovered == 0) return BigDecimal.ZERO.setScale(2);
        return overallTotal.divide(BigDecimal.valueOf(monthsCovered), 2, RoundingMode.HALF_UP);
    }
}
