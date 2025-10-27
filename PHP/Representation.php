<?php
require_once 'Database.php';

class Representation {
    private $id;
    private $id_spectacle;
    private $date_heure;
    private $salle;

    public function __construct($id_spectacle, $date_heure, $salle, $id = null) {
        $this->id = $id;
        $this->id_spectacle = $id_spectacle;
        $this->date_heure = $date_heure;
        $this->salle = $salle;
    }

    public function getId() { return $this->id; }
    public function getIdSpectacle() { return $this->id_spectacle; }
    public function getDateHeure() { return $this->date_heure; }
    public function getSalle() { return $this->salle; }

    public static function getAll() {
        $pdo = Database::getConnection();
        $stmt = $pdo->query("
            SELECT r.*, s.titre AS spectacle_titre
            FROM Representation r
            JOIN Spectacle s ON r.id_spectacle = s.id
            ORDER BY r.date_heure ASC
        ");
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public static function getById($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            SELECT r.*, s.titre AS spectacle_titre
            FROM Representation r
            JOIN Spectacle s ON r.id_spectacle = s.id
            WHERE r.id = ?
        ");
        $stmt->execute([$id]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    public static function getBySpectacle($id_spectacle) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            SELECT * FROM Representation
            WHERE id_spectacle = ?
            ORDER BY date_heure ASC
        ");
        $stmt->execute([$id_spectacle]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function save() {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            INSERT INTO Representation (id_spectacle, date_heure, salle)
            VALUES (?, ?, ?)
        ");
        $stmt->execute([
            $this->id_spectacle,
            $this->date_heure,
            $this->salle
        ]);
        $this->id = $pdo->lastInsertId();
    }

    public static function delete($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("DELETE FROM Representation WHERE id = ?");
        $stmt->execute([$id]);
    }
}
?>
