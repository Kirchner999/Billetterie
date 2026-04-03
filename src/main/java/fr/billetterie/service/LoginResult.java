package fr.billetterie.service;

import fr.billetterie.model.Client;

public record LoginResult(boolean success, String message, Client client, String targetView) {

    public static LoginResult success(Client client, String targetView) {
        return new LoginResult(true, "", client, targetView);
    }

    public static LoginResult failure(String message) {
        return new LoginResult(false, message, null, null);
    }
}
