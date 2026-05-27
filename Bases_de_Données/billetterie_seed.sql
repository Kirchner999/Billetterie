-- Script d'update / seed pour phpMyAdmin
-- Base cible: dispelltacle
-- Import: phpMyAdmin > base dispelltacle > Importer > ce fichier.

CREATE DATABASE IF NOT EXISTS dispelltacle CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dispelltacle;

-- Tables techniques attendues par l'application.
CREATE TABLE IF NOT EXISTS ticket_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    ticket_number VARCHAR(120) NOT NULL,
    pdf_path VARCHAR(500) NOT NULL,
    generated_at DATETIME NOT NULL,
    UNIQUE KEY uk_ticket_files_purchase (purchase_id),
    CONSTRAINT fk_ticket_files_billet_seed
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS purchase_seats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    seat_label VARCHAR(30) NOT NULL,
    CONSTRAINT fk_purchase_seats_billet_seed
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ticket_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ticket_events_billet_seed
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

-- Comptes de test. Les mots de passe restent les mêmes à la connexion:
-- azdine75/pass123, marie_du93/mdp123, jordanL/secret, mateo_dev/slam2025.
INSERT INTO client (pseudo, nom, prenom, numero, email, password, adresse, role, is_admin)
VALUES
('azdine75', 'Achari', 'Maxime-Azdine', '0612345678', 'maxime.azdine@example.com', 'sha256:9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', '34 bis rue du Cotentin, Paris', 'ADMIN', 1),
('marie_du93', 'Dupont', 'Marie', '0678123456', 'marie.dupont@example.com', 'sha256:2f81cba8c3e6f76972a8a3991fd5980eb77515f1fc9d05e5e094e1b82f457776', '12 rue de la Republique, Saint-Denis', 'CLIENT', 0),
('jordanL', 'Lemaire', 'Jordan', '0654321987', 'jordan.lemaire@example.com', 'sha256:2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', '45 avenue Victor Hugo, Levallois', 'CLIENT', 0),
('mateo_dev', 'Terqui', 'Mateo', '0666778899', 'mateo.terqui@example.com', 'sha256:7c640f94d6e482508327c18778187da3411d990330d4fbcd75cae253c3eff29b', '78 rue du Code, Paris', 'EDITEUR', 0)
ON DUPLICATE KEY UPDATE
    nom = VALUES(nom),
    prenom = VALUES(prenom),
    numero = VALUES(numero),
    password = VALUES(password),
    adresse = VALUES(adresse),
    role = VALUES(role),
    is_admin = VALUES(is_admin);

SET @event_1 := 'Les Miserables - Grand Rex';
SET @event_2 := 'Romeo et Juliette - Theatre Mogador';
SET @event_3 := 'Le Lac des Cygnes - Palais Garnier';
SET @event_4 := 'Starmania Live - Zenith Paris';
SET @event_5 := 'Le Roi Lion - Seance Famille';
SET @event_6 := 'Concert Archive - evenement passe';

-- Nettoyage idempotent des anciennes données de ce seed uniquement.
DELETE ps FROM purchase_seats ps
JOIN billet b ON b.id = ps.purchase_id
JOIN representation r ON r.id = b.id_representation
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE tf FROM ticket_files tf
JOIN billet b ON b.id = tf.purchase_id
JOIN representation r ON r.id = b.id_representation
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE te FROM ticket_events te
JOIN billet b ON b.id = te.purchase_id
JOIN representation r ON r.id = b.id_representation
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE b FROM billet b
JOIN representation r ON r.id = b.id_representation
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE st FROM seats st
JOIN representation r ON r.id = st.ticket_id
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE r FROM representation r
JOIN spectacle s ON s.id = r.id_spectacle
WHERE s.titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

DELETE FROM spectacle
WHERE titre IN (@event_1, @event_2, @event_3, @event_4, @event_5, @event_6);

-- Nouveaux spectacles.
INSERT INTO spectacle (titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos)
VALUES
(@event_1, 'Grand Rex, Paris', 'affiche_les_miserables.jpg', 'drame,musical,historique', 165, 'Grande fresque musicale de Victor Hugo', 'Une version scénique intense autour de Jean Valjean, Javert et la révolte parisienne.', 'Français', 12, 'les_miserables_1.jpg'),
(@event_2, 'Theatre Mogador, Paris', 'affiche_romeo_juliette.jpg', 'romance,musical', 145, 'La comédie musicale culte revient à Paris', 'Deux familles rivales, une histoire d’amour impossible et une mise en scène moderne.', 'Français', 10, 'romeo_juliette_1.jpg'),
(@event_3, 'Palais Garnier, Paris', 'affiche_lac_cygnes.jpg', 'ballet,classique', 130, 'Le ballet classique incontournable', 'Le chef-d’oeuvre de Tchaïkovski dans un écrin historique.', 'Français', 7, 'lac_cygnes_1.jpg'),
(@event_4, 'Zenith Paris', 'affiche_starmania.jpg', 'rock,musical', 150, 'L’opéra rock en version live', 'Une production énergique avec les grands titres de Starmania.', 'Français', 10, 'starmania_1.jpg'),
(@event_5, 'Theatre Mogador, Paris', 'affiche_roi_lion_famille.jpg', 'famille,musical', 150, 'Séance familiale du Roi Lion', 'Une séance adaptée aux familles avec une grande disponibilité de places.', 'Français', 6, 'roi_lion_1.jpg'),
(@event_6, 'Salle Archive', 'affiche_archive.jpg', 'archive', 90, 'Événement passé pour tester le nettoyage', 'Cet événement est volontairement daté dans le passé.', 'Français', 0, 'archive_1.jpg');

SET @spectacle_1 := (SELECT id FROM spectacle WHERE titre = @event_1 LIMIT 1);
SET @spectacle_2 := (SELECT id FROM spectacle WHERE titre = @event_2 LIMIT 1);
SET @spectacle_3 := (SELECT id FROM spectacle WHERE titre = @event_3 LIMIT 1);
SET @spectacle_4 := (SELECT id FROM spectacle WHERE titre = @event_4 LIMIT 1);
SET @spectacle_5 := (SELECT id FROM spectacle WHERE titre = @event_5 LIMIT 1);
SET @spectacle_6 := (SELECT id FROM spectacle WHERE titre = @event_6 LIMIT 1);

-- Dates futures par rapport au 27/05/2026, pour que l'espace client les affiche.
INSERT INTO representation (id_spectacle, date_heure, salle, prix, stock)
VALUES
(@spectacle_1, '2026-06-14 20:00:00', 'Salle Rouge', 79.90, 0),
(@spectacle_2, '2026-06-21 20:30:00', 'Salle Mogador', 69.00, 0),
(@spectacle_3, '2026-07-03 19:30:00', 'Palais Garnier', 95.50, 0),
(@spectacle_4, '2026-07-15 21:00:00', 'Grande Salle', 59.90, 0),
(@spectacle_5, '2026-08-02 15:00:00', 'Salle Famille', 84.00, 0),
(@spectacle_6, '2025-01-10 20:00:00', 'Salle Archive', 45.00, 0);

SET @ticket_1 := (SELECT r.id FROM representation r JOIN spectacle s ON s.id = r.id_spectacle WHERE s.titre = @event_1 LIMIT 1);
SET @ticket_2 := (SELECT r.id FROM representation r JOIN spectacle s ON s.id = r.id_spectacle WHERE s.titre = @event_2 LIMIT 1);
SET @ticket_3 := (SELECT r.id FROM representation r JOIN spectacle s ON s.id = r.id_spectacle WHERE s.titre = @event_3 LIMIT 1);
SET @ticket_4 := (SELECT r.id FROM representation r JOIN spectacle s ON s.id = r.id_spectacle WHERE s.titre = @event_4 LIMIT 1);
SET @ticket_5 := (SELECT r.id FROM representation r JOIN spectacle s ON s.id = r.id_spectacle WHERE s.titre = @event_5 LIMIT 1);

-- Plan de salle.
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_1, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_1, 'B', n, IF(n IN (2,5), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6
) nums;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_2, 'A', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums;
INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_2, 'B', n, IF(n IN (1,8), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_3, 'C', n, IF(n = 4, 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
) nums;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_4, 'D', n, 0 FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
) nums;

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken)
SELECT @ticket_5, 'E', n, IF(n IN (3,4), 1, 0) FROM (
    SELECT 1 AS n UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9
) nums;

-- Achats de démonstration correspondant aux sièges déjà pris.
SET @client_admin := (SELECT id FROM client WHERE pseudo = 'azdine75' LIMIT 1);
SET @client_marie := (SELECT id FROM client WHERE pseudo = 'marie_du93' LIMIT 1);
SET @client_jordan := (SELECT id FROM client WHERE pseudo = 'jordanL' LIMIT 1);
SET @client_mateo := (SELECT id FROM client WHERE pseudo = 'mateo_dev' LIMIT 1);

INSERT INTO billet (numero, id_representation, id_client, statut, quantite, total, date_achat)
VALUES
('SEED-R1-001', @ticket_1, @client_marie, 'valide', 2, 159.80, '2026-05-28 10:15:00'),
('SEED-R2-001', @ticket_2, @client_admin, 'valide', 2, 138.00, '2026-05-29 11:00:00'),
('SEED-R3-001', @ticket_3, @client_jordan, 'valide', 1, 95.50, '2026-05-30 12:30:00'),
('SEED-R5-001', @ticket_5, @client_mateo, 'rembourse', 2, 168.00, '2026-05-30 15:45:00');

SET @purchase_1 := (SELECT id FROM billet WHERE numero = 'SEED-R1-001' LIMIT 1);
SET @purchase_2 := (SELECT id FROM billet WHERE numero = 'SEED-R2-001' LIMIT 1);
SET @purchase_3 := (SELECT id FROM billet WHERE numero = 'SEED-R3-001' LIMIT 1);
SET @purchase_5 := (SELECT id FROM billet WHERE numero = 'SEED-R5-001' LIMIT 1);

UPDATE billet SET refunded_at = '2026-05-31 09:20:00' WHERE id = @purchase_5;

INSERT INTO purchase_seats (purchase_id, seat_label)
VALUES
(@purchase_1, 'B2'), (@purchase_1, 'B5'),
(@purchase_2, 'B1'), (@purchase_2, 'B8'),
(@purchase_3, 'C4'),
(@purchase_5, 'E3'), (@purchase_5, 'E4');

INSERT INTO ticket_events (purchase_id, event_type, details, created_at)
VALUES
(@purchase_1, 'GENERATED', 'Billet de démonstration généré par seed phpMyAdmin', NOW()),
(@purchase_2, 'GENERATED', 'Billet de démonstration généré par seed phpMyAdmin', NOW()),
(@purchase_3, 'GENERATED', 'Billet de démonstration généré par seed phpMyAdmin', NOW()),
(@purchase_5, 'REFUNDED', 'Achat remboursé pour tester les statistiques', NOW());

-- Synchronisation du stock avec les sièges libres.
UPDATE representation r
JOIN (
    SELECT
        ticket_id,
        COALESCE(SUM(CASE WHEN is_taken = 0 THEN 1 ELSE 0 END), 0) AS free_seats
    FROM seats
    GROUP BY ticket_id
) seat_stats ON seat_stats.ticket_id = r.id
SET r.stock = seat_stats.free_seats;

-- Vues de compatibilité utilisées par le code Java.
CREATE OR REPLACE VIEW users AS
SELECT
    id,
    pseudo AS username,
    password,
    LOWER(CASE WHEN role = 'CLIENT' THEN 'user' ELSE role END) AS role
FROM client;

CREATE OR REPLACE VIEW tickets AS
SELECT
    r.id,
    s.titre AS event_name,
    r.date_heure AS event_date,
    r.prix AS price,
    r.stock AS stock
FROM representation r
JOIN spectacle s ON s.id = r.id_spectacle;

CREATE OR REPLACE VIEW purchases AS
SELECT
    b.id,
    b.id_client AS user_id,
    b.id_representation AS ticket_id,
    b.quantite AS quantity,
    b.total,
    b.date_achat AS purchase_date,
    CASE
        WHEN b.statut = 'valide' THEN 'CONFIRMED'
        WHEN b.statut = 'annule' THEN 'CANCELLED'
        WHEN b.statut = 'rembourse' THEN 'REFUNDED'
        ELSE 'CONFIRMED'
    END AS status,
    b.refunded_at
FROM billet b;

SELECT 'Update dispelltacle terminé' AS message;
