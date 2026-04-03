package fr.billetterie.repository;

import fr.billetterie.dao.ClientDAO;
import fr.billetterie.model.Client;

import java.util.List;

public class DaoClientRepository implements ClientRepository {

    @Override
    public Client authenticate(String email, String password) {
        return ClientDAO.authenticate(email, password);
    }

    @Override
    public boolean emailExists(String email) {
        return ClientDAO.emailExists(email);
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
