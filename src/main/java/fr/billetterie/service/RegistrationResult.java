package fr.billetterie.service;

public record RegistrationResult(boolean success, String message) {

    public static RegistrationResult success(String message) {
        return new RegistrationResult(true, message);
    }

    public static RegistrationResult failure(String message) {
        return new RegistrationResult(false, message);
    }
}
