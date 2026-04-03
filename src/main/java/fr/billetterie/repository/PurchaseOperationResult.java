package fr.billetterie.repository;

public record PurchaseOperationResult(boolean success, String message) {

    public static PurchaseOperationResult success(String message) {
        return new PurchaseOperationResult(true, message);
    }

    public static PurchaseOperationResult failure(String message) {
        return new PurchaseOperationResult(false, message);
    }
}
