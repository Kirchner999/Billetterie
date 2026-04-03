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

INSERT INTO tickets (event_name, event_date, price, stock) VALUES
('Les Miserables - Grand Rex', '2026-05-14 20:00:00', 79.90, 12),
('Romeo et Juliette - Theatre Mogador', '2026-05-21 20:30:00', 69.00, 16),
('Le Lac des Cygnes - Palais Garnier', '2026-06-03 19:30:00', 95.50, 20),
('Starmania Live - Zenith Paris', '2026-06-15 21:00:00', 59.90, 25),
('Le Roi Lion - Seance Famille', '2026-07-02 15:00:00', 84.00, 18),
('Concert Archive - evenement passe', '2025-01-10 20:00:00', 45.00, 0);

SET @ticket_start := (SELECT MAX(id) - 5 FROM tickets);

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 1, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums_a1;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 1, 'B', n, IF(n IN (2,5), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums_b1;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 2, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_a2;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 2, 'B', n, IF(n IN (1,8), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_b2;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 3, 'C', n, IF(n = 4, 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) nums_c3;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 4, 'D', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums_d4;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_start + 5, 'E', n, IF(n IN (3,4), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) nums_e5;
