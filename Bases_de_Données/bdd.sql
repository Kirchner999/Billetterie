DROP DATABASE IF EXISTS dispelltacle;
CREATE DATABASE dispelltacle CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE dispelltacle;

CREATE TABLE client (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pseudo VARCHAR(100) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    numero VARCHAR(20),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    adresse VARCHAR(255),
    role ENUM('ADMIN', 'EDITEUR', 'CLIENT') NOT NULL DEFAULT 'CLIENT',
    is_admin TINYINT(1) NOT NULL DEFAULT 0,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE spectacle (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    affiche VARCHAR(255),
    tags VARCHAR(255),
    duree INT,
    description_courte TEXT,
    description_longue TEXT,
    langue VARCHAR(50),
    age_minimum INT,
    photos VARCHAR(255)
);

CREATE TABLE representation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_spectacle INT NOT NULL,
    date_heure DATETIME NOT NULL,
    salle VARCHAR(255),
    prix DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    stock INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_representation_spectacle
        FOREIGN KEY (id_spectacle) REFERENCES spectacle(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE billet (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) NOT NULL UNIQUE,
    id_representation INT NOT NULL,
    id_client INT NOT NULL,
    statut ENUM('valide', 'annule', 'rembourse') NOT NULL DEFAULT 'valide',
    quantite INT NOT NULL DEFAULT 1,
    total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    date_achat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    refunded_at DATETIME NULL,
    CONSTRAINT fk_billet_representation
        FOREIGN KEY (id_representation) REFERENCES representation(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_billet_client
        FOREIGN KEY (id_client) REFERENCES client(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE seats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id INT NOT NULL,
    seat_row VARCHAR(5) NOT NULL,
    seat_number INT NOT NULL,
    is_taken TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_seats_representation
        FOREIGN KEY (ticket_id) REFERENCES representation(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

CREATE TABLE ticket_files (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    ticket_number VARCHAR(120) NOT NULL,
    pdf_path VARCHAR(500) NOT NULL,
    generated_at DATETIME NOT NULL,
    UNIQUE KEY uk_ticket_files_purchase (purchase_id),
    CONSTRAINT fk_ticket_files_billet
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

CREATE TABLE purchase_seats (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    seat_label VARCHAR(30) NOT NULL,
    CONSTRAINT fk_purchase_seats_billet
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

CREATE TABLE ticket_events (
    id INT AUTO_INCREMENT PRIMARY KEY,
    purchase_id INT NOT NULL,
    event_type VARCHAR(40) NOT NULL,
    details VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_ticket_events_billet
        FOREIGN KEY (purchase_id) REFERENCES billet(id)
        ON DELETE CASCADE
);

CREATE OR REPLACE VIEW users AS
SELECT
    id,
    pseudo AS username,
    password,
    LOWER(
        CASE
            WHEN role = 'CLIENT' THEN 'user'
            ELSE role
        END
    ) AS role
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
