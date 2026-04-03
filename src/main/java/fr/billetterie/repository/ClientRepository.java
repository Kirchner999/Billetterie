package fr.billetterie.repository;

import fr.billetterie.model.Client;

import java.util.List;

public interface ClientRepository {

    Client authenticate(String username, String password);

    boolean usernameExists(String username);

    boolean register(Client client);

    List<Client> getAll();
}
