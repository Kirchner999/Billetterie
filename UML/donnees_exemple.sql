USE Gestion_Billetterie;

INSERT INTO Client (pseudo, nom, prenom, numero, email, password, adresse, is_admin) VALUES
('azdine75', 'Achari', 'Maxime-Azdine', '0612345678', 'maxime.azdine@example.com', 'sha256:9b8769a4a742959a2d0298c36fb70623f2dfacda8436237df08d8dfd5b37374c', '34 bis rue du Cotentin, Paris', TRUE),
('marie_du93', 'Dupont', 'Marie', '0678123456', 'marie.dupont@example.com', 'sha256:2f81cba8c3e6f76972a8a3991fd5980eb77515f1fc9d05e5e094e1b82f457776', '12 rue de la République, Saint-Denis', FALSE),
('jordanL', 'Lemaire', 'Jordan', '0654321987', 'jordan.lemaire@example.com', 'sha256:2bb80d537b1da3e38bd30361aa855686bde0eacd7162fef6a25fe97bf527a25b', '45 avenue Victor Hugo, Levallois', FALSE),
('khali_off', 'Benali', 'Khalid', '0644556677', 'khali@example.com', 'sha256:9a00e0857486f5cd5c29914bea7c12bbd261ea240a83f0022cff45179da895f4', '9 rue du Soleil, Nanterre', FALSE),
('mateo_dev', 'Terqui', 'Matéo', '0666778899', 'mateo.terqui@example.com', 'sha256:7c640f94d6e482508327c18778187da3411d990330d4fbcd75cae253c3eff29b', '78 rue du Code, Paris', FALSE),
('sorayaA', 'Abdallah', 'Soraya', '0633345566', 'soraya.abdallah@example.com', 'sha256:69074aa1a18aa1f6658f2cd0b7277ec9471ae8eb061c8fa52a9ecfb3832dcad3', '56 boulevard Haussmann, Paris', FALSE),
('admin2', 'Durand', 'Paul', '0699001122', 'paul.durand@example.com', 'sha256:0242c0436daa4c241ca8a793764b7dfb50c223121bb844cf49be670a3af4dd18', '4 rue Centrale, Lyon', TRUE);

INSERT INTO Spectacle (titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos) VALUES
('Le Roi Lion', 'Théâtre Mogador, Paris', 'affiche_roi_lion.jpg', 'famille, musical', 150, 'Comédie musicale légendaire', 'La célèbre comédie musicale adaptée du film culte Disney.', 'Français', 6, 'photo1.jpg'),
('Notre-Dame de Paris', 'Palais des Congrès, Paris', 'affiche_ndp.jpg', 'drame, musical', 130, 'Spectacle mythique de Luc Plamondon', 'L''histoire tragique de Quasimodo et Esmeralda revisitée en chansons.', 'Français', 10, 'photo2.jpg'),
('Mamma Mia!', 'Casino de Paris', 'affiche_mammamia.jpg', 'musical, comédie', 140, 'Les tubes d''ABBA en spectacle', 'Une mère, une fille, trois pères possibles, et des chansons inoubliables.', 'Anglais', 8, 'photo3.jpg'),
('Les Misérables', 'Opéra Bastille', 'affiche_lesmiserables.jpg', 'drame, historique', 160, 'Classique de Victor Hugo', 'La fresque monumentale de la révolte et de la misère humaine.', 'Français', 12, 'photo4.jpg'),
('Casse-Noisette', 'Opéra Garnier', 'affiche_cassenoisette.jpg', 'ballet, noël', 120, 'Le ballet de Noël par excellence', 'L''histoire magique de Clara et de son casse-noisette enchanté.', 'Français', 5, 'photo5.jpg');

INSERT INTO Representation (id_spectacle, date_heure, salle) VALUES
(1, '2026-12-15 20:00:00', 'Salle A'),
(1, '2026-12-16 14:00:00', 'Salle A'),
(2, '2026-12-20 21:00:00', 'Grande Salle'),
(3, '2026-11-28 20:30:00', 'Casino Hall'),
(3, '2026-12-01 18:00:00', 'Casino Hall'),
(4, '2026-12-22 19:30:00', 'Opéra Bastille'),
(5, '2026-12-24 17:00:00', 'Opéra Garnier'),
(5, '2026-12-25 20:00:00', 'Opéra Garnier');

INSERT INTO Billet (numero, id_representation, id_client) VALUES
('A001', 1, 1),
('A002', 1, 2),
('A003', 1, 3),
('A004', 2, 4),
('A005', 3, 2),
('A006', 3, 5),
('A007', 4, 6),
('A008', 4, 1),
('A009', 5, 3),
('A010', 6, 7),
('A011', 7, 5),
('A012', 8, 6),
('A013', 8, 2),
('A014', 7, 3),
('A015', 3, 4),
('A016', 5, 1),
('A017', 5, 7),
('A018', 6, 6),
('A019', 2, 2),
('A020', 4, 5);

-- Tous les billets d'un client
SELECT b.numero, s.titre, r.date_heure, r.salle
FROM Billet b
JOIN Representation r ON b.id_representation = r.id
JOIN Spectacle s ON r.id_spectacle = s.id
JOIN Client c ON b.id_client = c.id
WHERE c.pseudo = 'marie_du93';

-- Nombre de billets vendus par spectacle
SELECT s.titre, COUNT(b.id) AS nb_billets_vendus
FROM Billet b
JOIN Representation r ON b.id_representation = r.id
JOIN Spectacle s ON r.id_spectacle = s.id
GROUP BY s.titre
ORDER BY nb_billets_vendus DESC;

-- Représentations à venir dans les 30 jours
SELECT s.titre, r.date_heure, r.salle
FROM Representation r
JOIN Spectacle s ON r.id_spectacle = s.id
WHERE r.date_heure BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 30 DAY)
ORDER BY r.date_heure;

-- Liste des clients avec le nombre total de billets
SELECT c.pseudo, c.email, COUNT(b.id) AS total_billets
FROM Client c
LEFT JOIN Billet b ON c.id = b.id_client
GROUP BY c.id
ORDER BY total_billets DESC;

-- Top 3 des spectacles les plus populaires
SELECT s.titre, COUNT(b.id) AS nb_billets
FROM Billet b
JOIN Representation r ON b.id_representation = r.id
JOIN Spectacle s ON r.id_spectacle = s.id
GROUP BY s.id
ORDER BY nb_billets DESC
LIMIT 3;
