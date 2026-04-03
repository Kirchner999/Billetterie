package fr.billetterie.repository;

import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;

import java.util.List;

public class DaoClientRepository implements ClientRepository {

    @Override
    public Client authenticate(String username, String password) {
        return ClientDAO.authenticate(username, password);
    }

    @Override
    public boolean usernameExists(String username) {
        return ClientDAO.usernameExists(username);
    }

    @Override
    public boolean register(Client client) {
        return ClientDAO.register(client);
    }

    @Override
    public List<Client> getAll() {
        return ClientDAO.getAll();
    }
}
