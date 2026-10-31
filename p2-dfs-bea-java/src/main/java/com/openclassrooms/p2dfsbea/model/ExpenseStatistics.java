package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Overview of the expense history: overall total, number of months covered
 * (calendar span from the first to the last expense, inclusive) and one row per category.
 */
public record ExpenseStatistics(BigDecimal overallTotal, long monthsCovered, List<CategoryStatistics> categories) {
}
