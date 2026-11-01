package com.openclassrooms.p2dfsbea.model;

import java.math.BigDecimal;

/**
 * Total and monthly average of the expenses of one category.
 */
public record CategoryStatistics(String category, BigDecimal total, BigDecimal monthlyAverage) {
}
