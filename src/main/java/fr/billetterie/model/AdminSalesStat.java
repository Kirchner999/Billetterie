package fr.billetterie.model;

import java.math.BigDecimal;

public record AdminSalesStat(
        String eventName,
        int confirmedPurchases,
        int refundedPurchases,
        int cancelledPurchases,
        int ticketsSold,
        BigDecimal revenue
) {
}
