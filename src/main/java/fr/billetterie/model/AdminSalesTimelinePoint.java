package fr.billetterie.model;

import java.math.BigDecimal;

public record AdminSalesTimelinePoint(
        String label,
        BigDecimal confirmedRevenue,
        BigDecimal refundedRevenue
) {
}
