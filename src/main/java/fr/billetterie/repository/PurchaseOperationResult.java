package fr.billetterie.repository;

public record PurchaseOperationResult(boolean success, String message, Integer purchaseId) {

    public static PurchaseOperationResult success(String message) {
        return new PurchaseOperationResult(true, message, null);
    }

    public static PurchaseOperationResult success(String message, Integer purchaseId) {
        return new PurchaseOperationResult(true, message, purchaseId);
    }

    public static PurchaseOperationResult failure(String message) {
        return new PurchaseOperationResult(false, message, null);
    }
}
