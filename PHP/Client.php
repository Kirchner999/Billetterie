<?php
require_once 'Database.php';

class Client {
    private $id;
    private $pseudo;
    private $nom;
    private $prenom;
    private $numero;
    private $email;
    private $password;
    private $adresse;
    private $is_admin;

    public function __construct($pseudo, $nom, $prenom, $numero, $email, $password, $adresse, $is_admin = false) {
        $this->pseudo = $pseudo;
        $this->nom = $nom;
        $this->prenom = $prenom;
        $this->numero = $numero;
        $this->email = $email;
        $this->password = $password;
        $this->adresse = $adresse;
        $this->is_admin = $is_admin;
    }

    public static function getAll() {
        $pdo = Database::getConnection();
        $stmt = $pdo->query("SELECT * FROM Client");
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public static function getById($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("SELECT * FROM Client WHERE id = ?");
        $stmt->execute([$id]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    public function save() {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            INSERT INTO Client (pseudo, nom, prenom, numero, email, password, adresse, is_admin)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            $this->pseudo,
            $this->nom,
            $this->prenom,
            $this->numero,
            $this->email,
            password_hash($this->password, PASSWORD_BCRYPT),
            $this->adresse,
            $this->is_admin
        ]);
        $this->id = $pdo->lastInsertId();
    }

    public static function delete($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("DELETE FROM Client WHERE id = ?");
        $stmt->execute([$id]);
    }
}
?>
