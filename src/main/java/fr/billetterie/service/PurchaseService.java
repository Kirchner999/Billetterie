package fr.billetterie.service;

import fr.billetterie.model.Client;
import fr.billetterie.model.Seat;
import fr.billetterie.repository.PurchaseOperationResult;
import fr.billetterie.repository.TicketStoreRepository;

import java.util.List;

public class PurchaseService {

    private final TicketStoreRepository ticketStoreRepository;

    public PurchaseService(TicketStoreRepository ticketStoreRepository) {
        this.ticketStoreRepository = ticketStoreRepository;
    }

    public PurchaseOperationResult purchaseWithoutSeats(Client user, int ticketId, String quantityValue) {
        if (user == null) {
            return PurchaseOperationResult.failure("Aucun utilisateur connecte.");
        }

        int quantity;
        try {
            quantity = Integer.parseInt(quantityValue.trim());
        } catch (Exception e) {
            return PurchaseOperationResult.failure("La quantite doit etre un nombre entier.");
        }

        if (quantity <= 0) {
            return PurchaseOperationResult.failure("La quantite doit etre superieure a 0.");
        }

        return ticketStoreRepository.purchaseTicket(user.getId(), ticketId, List.of(), quantity);
    }

    public PurchaseOperationResult purchaseWithSeats(Client user, int ticketId, List<Seat> selectedSeats) {
        if (user == null) {
            return PurchaseOperationResult.failure("Aucun utilisateur connecte.");
        }

        if (selectedSeats == null || selectedSeats.isEmpty()) {
            return PurchaseOperationResult.failure("Selectionne au moins un siege.");
        }

        List<Integer> seatIds = selectedSeats.stream().map(Seat::id).toList();
        return ticketStoreRepository.purchaseTicket(user.getId(), ticketId, seatIds, seatIds.size());
    }
}
