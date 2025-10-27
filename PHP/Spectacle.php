<?php
require_once 'Database.php';

class Spectacle {
    private $id;
    private $titre;
    private $lieu;
    private $affiche;
    private $tags;
    private $duree;
    private $description_courte;
    private $description_longue;
    private $langue;
    private $age_minimum;
    private $photos;

    public function __construct($titre, $lieu, $affiche, $tags, $duree, $description_courte, $description_longue, $langue, $age_minimum, $photos, $id = null) {
        $this->id = $id;
        $this->titre = $titre;
        $this->lieu = $lieu;
        $this->affiche = $affiche;
        $this->tags = $tags;
        $this->duree = $duree;
        $this->description_courte = $description_courte;
        $this->description_longue = $description_longue;
        $this->langue = $langue;
        $this->age_minimum = $age_minimum;
        $this->photos = $photos;
    }

    public function getId() { return $this->id; }
    public function getTitre() { return $this->titre; }
    public function getLieu() { return $this->lieu; }
    public function getAffiche() { return $this->affiche; }
    public function getTags() { return $this->tags; }
    public function getDuree() { return $this->duree; }
    public function getDescriptionCourte() { return $this->description_courte; }
    public function getLangue() { return $this->langue; }
    public function getAgeMinimum() { return $this->age_minimum; }

    public static function getAll() {
        $pdo = Database::getConnection();
        $stmt = $pdo->query("SELECT * FROM Spectacle ORDER BY titre ASC");
        return $stmt->fetchAll(PDO::FETCH_ASSOC);
    }

    public static function getById($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("SELECT * FROM Spectacle WHERE id = ?");
        $stmt->execute([$id]);
        return $stmt->fetch(PDO::FETCH_ASSOC);
    }

    public function save() {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("
            INSERT INTO Spectacle (titre, lieu, affiche, tags, duree, description_courte, description_longue, langue, age_minimum, photos)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");
        $stmt->execute([
            $this->titre,
            $this->lieu,
            $this->affiche,
            $this->tags,
            $this->duree,
            $this->description_courte,
            $this->description_longue,
            $this->langue,
            $this->age_minimum,
            $this->photos
        ]);
        $this->id = $pdo->lastInsertId();
    }

    public static function delete($id) {
        $pdo = Database::getConnection();
        $stmt = $pdo->prepare("DELETE FROM Spectacle WHERE id = ?");
        $stmt->execute([$id]);
    }

}
?>
