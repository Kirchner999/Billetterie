package fr.billetterie.service;

public record EventFormResult(boolean success, String message) {

    public static EventFormResult success(String message) {
        return new EventFormResult(true, message);
    }

    public static EventFormResult failure(String message) {
        return new EventFormResult(false, message);
    }
}
