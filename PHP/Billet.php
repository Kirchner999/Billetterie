<?php
require_once 'Database.php';

class Billet {
    private $id;
    private $numero;
    private $id_representation;
    private $id_client;
    private $statut;
    private $date_achat;

    public function __construct($numero, $id_representation, $id_client, $statut = 'valide', $id = null) {
        $this->id = $id;
        $this->numero = $numero;
        $this->id_representation = $id_representation;
        $this->id_client = $id_client;
        $this->statut = $statut;
    }

    public function getId() { return $this->id; }
    public function getNumero() { return $this->numero; }
    public function getStatut() { return $this->statut; }
    public function getIdRepresentation() { return $this->id_representation; }
    public function getIdClient() { return $this->id_client; }
    public function setStatut($statut) { $this->statut = $statut; }

    public static function getAll() {
        $pdo = Database::getConnection();
        $stmt = $pdo->query("SELECT * FROM Billet ORDER BY date_achat DESC");
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public static function getById($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("SELECT * FROM Billet WHERE id = ?");
        $stmt->execute([$id]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    public static function getByClient($id_client) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            SELECT b.*, r.date_heure, s.titre, s.lieu
            FROM Billet b
            JOIN Representation r ON b.id_representation = r.id
            JOIN Spectacle s ON r.id_spectacle = s.id
            WHERE b.id_client = ?
            ORDER BY r.date_heure DESC
        ");
        $stmt->execute([$id_client]);
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public function save() {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            INSERT INTO Billet (numero, id_representation, id_client, statut)
            VALUES (?, ?, ?, ?)
        ");
        $stmt->execute([
            $this->numero,
            $this->id_representation,
            $this->id_client,
            $this->statut
        ]);
        $this->id = $pdo->lastInsertId();
    }

    public function updateStatut() {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("UPDATE Billet SET statut = ? WHERE id = ?");
        $stmt->execute([$this->statut, $this->id]);
    }

    public static function delete($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("DELETE FROM Billet WHERE id = ?");
        $stmt->execute([$id]);
    }
}
?>
