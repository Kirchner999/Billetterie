package fr.billetterie.repository;

import fr.billetterie.model.Client;

import java.util.List;

public interface ClientRepository {

    Client authenticate(String email, String password);

    boolean emailExists(String email);

    boolean register(Client client);

    List<Client> getAll();
}
