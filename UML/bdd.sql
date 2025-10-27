
CREATE DATABASE IF NOT EXISTS Gestion_Billetterie;
USE Gestion_Billetterie;


CREATE TABLE Client (
    id INT AUTO_INCREMENT PRIMARY KEY,
    pseudo VARCHAR(100) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    prenom VARCHAR(100) NOT NULL,
    numero VARCHAR(20),
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    adresse VARCHAR(255),
    is_admin BOOLEAN DEFAULT FALSE
);

CREATE TABLE Spectacle (
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


CREATE TABLE Representation (
    id INT AUTO_INCREMENT PRIMARY KEY,
    id_spectacle INT NOT NULL,
    date_heure DATETIME NOT NULL,
    salle VARCHAR(255),
    CONSTRAINT fk_representation_spectacle
        FOREIGN KEY (id_spectacle)
        REFERENCES Spectacle(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);


CREATE TABLE Billet (
    id INT AUTO_INCREMENT PRIMARY KEY,
    numero VARCHAR(50) UNIQUE NOT NULL,
    id_representation INT NOT NULL,
    id_client INT NOT NULL,
    CONSTRAINT fk_billet_representation
        FOREIGN KEY (id_representation)
        REFERENCES Representation(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    CONSTRAINT fk_billet_client
        FOREIGN KEY (id_client)
        REFERENCES Client(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- SUGGESTION CHATGPT: INDEX POUR RECHERCHE PLUS RAPIDE
CREATE INDEX idx_client_email ON Client(email);
CREATE INDEX idx_representation_date ON Representation(date_heure);
CREATE INDEX idx_billet_numero ON Billet(numero);
