USE dispelltacle;

INSERT INTO client (pseudo, nom, prenom, numero, email, password, adresse, role, is_admin) VALUES
('azdine75', 'Achari', 'Maxime-Azdine', '0612345678', 'maxime.azdine@example.com', 'pass123', '34 bis rue du Cotentin, Paris', 'ADMIN', 1),
('marie_du93', 'Dupont', 'Marie', '0678123456', 'marie.dupont@example.com', 'mdp123', '12 rue de la Republique, Saint-Denis', 'CLIENT', 0),
('jordanL', 'Lemaire', 'Jordan', '0654321987', 'jordan.lemaire@example.com', 'secret', '45 avenue Victor Hugo, Levallois', 'CLIENT', 0),
('khali_off', 'Benali', 'Khalid', '0644556677', 'khali@example.com', 'khali44', '9 rue du Soleil, Nanterre', 'CLIENT', 0),
('mateo_dev', 'Terqui', 'Mateo', '0666778899', 'mateo.terqui@example.com', 'slam2025', '78 rue du Code, Paris', 'EDITEUR', 0),
('sorayaA', 'Abdallah', 'Soraya', '0633345566', 'soraya.abdallah@example.com', 'soso456', '56 boulevard Haussmann, Paris', 'CLIENT', 0),
('admin2', 'Durand', 'Paul', '0699001122', 'paul.durand@example.com', 'rootroot', '4 rue Centrale, Lyon', 'ADMIN', 1);

INSERT INTO spectacle (titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos) VALUES
('Le Roi Lion', 'Theatre Mogador, Paris', 'affiche_roi_lion.jpg', 'famille,musical', 150, 'Comedie musicale legendaire', 'La celebre comedie musicale adaptee du film culte Disney.', 'Francais', 6, 'photo1.jpg'),
('Notre-Dame de Paris', 'Palais des Congres, Paris', 'affiche_ndp.jpg', 'drame,musical', 130, 'Spectacle mythique de Luc Plamondon', 'L''histoire tragique de Quasimodo et Esmeralda revisitee en chansons.', 'Francais', 10, 'photo2.jpg'),
('Mamma Mia!', 'Casino de Paris', 'affiche_mammamia.jpg', 'musical,comedie', 140, 'Les tubes d''ABBA en spectacle', 'Une mere, une fille, trois peres possibles, et des chansons inoubliables.', 'Anglais', 8, 'photo3.jpg'),
('Les Miserables', 'Opera Bastille', 'affiche_lesmiserables.jpg', 'drame,historique', 160, 'Classique de Victor Hugo', 'La fresque monumentale de la revolte et de la misere humaine.', 'Francais', 12, 'photo4.jpg'),
('Casse-Noisette', 'Opera Garnier', 'affiche_cassenoisette.jpg', 'ballet,noel', 120, 'Le ballet de Noel par excellence', 'L''histoire magique de Clara et de son casse-noisette enchante.', 'Francais', 5, 'photo5.jpg');

INSERT INTO representation (id_spectacle, date_heure, salle, prix, stock) VALUES
(1, '2026-12-15 20:00:00', 'Salle A', 79.90, 12),
(1, '2026-12-16 14:00:00', 'Salle A', 69.90, 18),
(2, '2026-12-20 21:00:00', 'Grande Salle', 74.50, 16),
(3, '2026-11-28 20:30:00', 'Casino Hall', 64.90, 20),
(3, '2026-12-01 18:00:00', 'Casino Hall', 59.90, 22),
(4, '2026-12-22 19:30:00', 'Opera Bastille', 89.00, 14),
(5, '2026-12-24 17:00:00', 'Opera Garnier', 55.00, 25),
(5, '2026-12-25 20:00:00', 'Opera Garnier', 62.00, 19);

INSERT INTO billet (numero, id_representation, id_client, statut, quantite, total) VALUES
('A001', 1, 1, 'valide', 1, 79.90),
('A002', 1, 2, 'valide', 1, 79.90),
('A003', 1, 3, 'valide', 1, 79.90),
('A004', 2, 4, 'valide', 1, 69.90),
('A005', 3, 2, 'valide', 1, 74.50),
('A006', 3, 5, 'valide', 1, 74.50),
('A007', 4, 6, 'valide', 1, 64.90),
('A008', 4, 1, 'valide', 1, 64.90),
('A009', 5, 3, 'valide', 1, 59.90),
('A010', 6, 7, 'valide', 1, 89.00),
('A011', 7, 5, 'valide', 1, 55.00),
('A012', 8, 6, 'valide', 1, 62.00),
('A013', 8, 2, 'annule', 1, 62.00),
('A014', 7, 3, 'valide', 1, 55.00),
('A015', 3, 4, 'valide', 1, 74.50),
('A016', 5, 1, 'valide', 1, 59.90),
('A017', 5, 7, 'valide', 1, 59.90),
('A018', 6, 6, 'annule', 1, 89.00),
('A019', 2, 2, 'valide', 1, 69.90),
('A020', 4, 5, 'valide', 1, 64.90);

INSERT INTO seats (ticket_id, seat_row, seat_number, is_taken) VALUES
(1, 'A', 1, 1), (1, 'A', 2, 1), (1, 'A', 3, 1), (1, 'A', 4, 0), (1, 'A', 5, 0), (1, 'A', 6, 0),
(1, 'B', 1, 0), (1, 'B', 2, 0), (1, 'B', 3, 0), (1, 'B', 4, 0), (1, 'B', 5, 0), (1, 'B', 6, 0),
(2, 'A', 1, 1), (2, 'A', 2, 0), (2, 'A', 3, 0), (2, 'A', 4, 0), (2, 'A', 5, 0), (2, 'A', 6, 0),
(3, 'C', 1, 1), (3, 'C', 2, 1), (3, 'C', 3, 0), (3, 'C', 4, 0), (3, 'C', 5, 0), (3, 'C', 6, 0),
(4, 'D', 1, 1), (4, 'D', 2, 1), (4, 'D', 3, 0), (4, 'D', 4, 0), (4, 'D', 5, 0), (4, 'D', 6, 0);

SELECT b.numero, s.titre, r.date_heure, r.salle
FROM billet b
JOIN representation r ON b.id_representation = r.id
JOIN spectacle s ON r.id_spectacle = s.id
JOIN client c ON b.id_client = c.id
WHERE c.pseudo = 'marie_du93';

SELECT s.titre, COUNT(b.id) AS nb_billets_vendus
FROM billet b
JOIN representation r ON b.id_representation = r.id
JOIN spectacle s ON r.id_spectacle = s.id
GROUP BY s.titre
ORDER BY nb_billets_vendus DESC;

SELECT s.titre, r.date_heure, r.salle
FROM representation r
JOIN spectacle s ON r.id_spectacle = s.id
WHERE r.date_heure BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY)
ORDER BY r.date_heure;

SELECT c.pseudo, c.email, COUNT(b.id) AS total_billets
FROM client c
LEFT JOIN billet b ON c.id = b.id_client
GROUP BY c.id, c.pseudo, c.email
ORDER BY total_billets DESC;

SELECT s.titre, COUNT(b.id) AS nb_billets
FROM billet b
JOIN representation r ON b.id_representation = r.id
JOIN spectacle s ON r.id_spectacle = s.id
GROUP BY s.id, s.titre
ORDER BY nb_billets DESC
LIMIT 3;
