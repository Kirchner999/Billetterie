USE billetterie;

ALTER TABLE purchases
ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE purchases
ADD COLUMN IF NOT EXISTS refunded_at DATETIME NULL;

CREATE TABLE IF NOT EXISTS ticket_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    ticket_number VARCHAR(120) NOT NULL,
    pdf_path VARCHAR(500) NOT NULL,
    generated_at DATETIME NOT NULL,
    UNIQUE KEY uk_ticket_files_purchase (purchase_id),
    CONSTRAINT fk_ticket_files_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS purchase_seats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    seat_label VARCHAR(30) NOT NULL,
    CONSTRAINT fk_purchase_seats_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ticket_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ticket_events_purchase
        FOREIGN KEY (purchase_id) REFERENCES purchases(id)
        ON DELETE CASCADE
);

SET @event_1 := 'Les Miserables - Grand Rex';
SET @event_2 := 'Romeo et Juliette - Theatre Mogador';
SET @event_3 := 'Le Lac des Cygnes - Palais Garnier';
SET @event_4 := 'Starmania Live - Zenith Paris';
SET @event_5 := 'Le Roi Lion - Seance Famille';
SET @event_6 := 'Concert Archive - evenement passe';

DELETE ps FROM purchase_seats ps
JOIN purchases p ON p.id = ps.purchase_id
JOIN tickets t ON t.id = p.ticket_id
WHERE t.event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE tf FROM ticket_files tf
JOIN purchases p ON p.id = tf.purchase_id
JOIN tickets t ON t.id = p.ticket_id
WHERE t.event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE te FROM ticket_events te
JOIN purchases p ON p.id = te.purchase_id
JOIN tickets t ON t.id = p.ticket_id
WHERE t.event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE p FROM purchases p
JOIN tickets t ON t.id = p.ticket_id
WHERE t.event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE FROM seats
WHERE ticket_id IN (
    SELECT id FROM (
        SELECT id FROM tickets WHERE event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6)
    ) seeded_tickets
);

DELETE FROM tickets
WHERE event_name IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

INSERT INTO tickets (event_name, event_date, price, stock) VALUES
(@event_1, '2026-05-14 20:00:00', 79.90, 10),
(@event_2, '2026-05-21 20:30:00', 69.00, 14),
(@event_3, '2026-06-03 19:30:00', 95.50, 9),
(@event_4, '2026-06-15 21:00:00', 59.90, 8),
(@event_5, '2026-07-02 15:00:00', 84.00, 7),
(@event_6, '2025-01-10 20:00:00', 45.00, 0);

SET @ticket_1 := (SELECT id FROM tickets WHERE event_name = @event_1 LIMIT 1);
SET @ticket_2 := (SELECT id FROM tickets WHERE event_name = @event_2 LIMIT 1);
SET @ticket_3 := (SELECT id FROM tickets WHERE event_name = @event_3 LIMIT 1);
SET @ticket_4 := (SELECT id FROM tickets WHERE event_name = @event_4 LIMIT 1);
SET @ticket_5 := (SELECT id FROM tickets WHERE event_name = @event_5 LIMIT 1);

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_1, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums_a1;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_1, 'B', n, IF(n IN (2,5), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums_b1;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_2, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_a2;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_2, 'B', n, IF(n IN (1,8), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_b2;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_3, 'C', n, IF(n = 4, 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) nums_c3;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_4, 'D', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_d4;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_5, 'E', n, IF(n IN (3,4), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) nums_e5;
