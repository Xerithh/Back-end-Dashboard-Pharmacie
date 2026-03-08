package pharmacie.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pharmacie.dao.CategorieRepository;
import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

/**
 * Initialise des données de démonstration au démarrage si la base est vide.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategorieRepository categorieRepository;
    private final FournisseurRepository fournisseurRepository;
    private final MedicamentRepository medicamentRepository;

    @Override
    public void run(String... args) {
        if (categorieRepository.count() > 0 || medicamentRepository.count() > 0 || fournisseurRepository.count() > 0) {
            log.info("Initialisation ignorée : la base contient déjà des données.");
            return;
        }

        log.info("Initialisation des données de démonstration de la pharmacie...");

        List<Categorie> categories = creerCategories();
        List<Fournisseur> fournisseurs = creerFournisseurs(categories);
        creerMedicaments(categories);

        fournisseurRepository.saveAll(fournisseurs);

        log.info("Initialisation terminée : {} catégories, {} fournisseurs, {} médicaments.",
                categorieRepository.count(), fournisseurRepository.count(), medicamentRepository.count());
    }

    private List<Categorie> creerCategories() {
        List<Categorie> categories = List.of(
                creerCategorie("Antalgiques", "Médicaments contre la douleur"),
                creerCategorie("Antibiotiques", "Médicaments contre les infections"),
                creerCategorie("Vitamines", "Compléments vitaminiques"),
                creerCategorie("Digestif", "Médicaments pour le système digestif"),
                creerCategorie("Dermatologie", "Soins et traitements de la peau"));
        return categorieRepository.saveAll(categories);
    }

    private Categorie creerCategorie(String libelle, String description) {
        Categorie categorie = new Categorie(libelle);
        categorie.setDescription(description);
        return categorie;
    }

    private List<Fournisseur> creerFournisseurs(List<Categorie> categories) {
        Fournisseur f1 = new Fournisseur("PharmaDistrib", "pharmacie+pharmadistrib@gmail.com");
        Fournisseur f2 = new Fournisseur("MediSupply", "pharmacie+medisupply@gmail.com");
        Fournisseur f3 = new Fournisseur("BioMed", "pharmacie+biomed@gmail.com");
        Fournisseur f4 = new Fournisseur("HealthPlus", "pharmacie+healthplus@gmail.com");

        // Chaque catégorie doit être liée à au moins deux fournisseurs.
        lier(f1, categories.get(0), categories.get(1), categories.get(2));
        lier(f2, categories.get(0), categories.get(3), categories.get(4));
        lier(f3, categories.get(1), categories.get(2), categories.get(4));
        lier(f4, categories.get(2), categories.get(3), categories.get(4));

        return List.of(f1, f2, f3, f4);
    }

    private void lier(Fournisseur fournisseur, Categorie... categories) {
        for (Categorie categorie : categories) {
            fournisseur.ajouterCategorie(categorie);
        }
    }

    private void creerMedicaments(List<Categorie> categories) {
        List<Medicament> medicaments = new ArrayList<>();

        medicaments.add(creerMedicament("Paracétamol 500", "Boîte de 16 comprimés", categories.get(0), 12, 20,
                "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=400"));
        medicaments.add(creerMedicament("Ibuprofène 400", "Boîte de 20 comprimés", categories.get(0), 4, 12,
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=400"));
        medicaments.add(creerMedicament("Amoxicilline 1g", "Boîte de 12 gélules", categories.get(1), 3, 10,
                "https://images.unsplash.com/photo-1512069772995-ec65ed45afd6?w=400"));
        medicaments.add(creerMedicament("Azithromycine", "Boîte de 6 comprimés", categories.get(1), 9, 15,
                "https://images.unsplash.com/photo-1585435557343-3b092031a831?w=400"));
        medicaments.add(creerMedicament("Vitamine C", "Tube de 20 comprimés", categories.get(2), 5, 18,
                "https://images.unsplash.com/photo-1607619056574-7b8d3ee536b2?w=400"));
        medicaments.add(creerMedicament("Magnésium B6", "Boîte de 30 comprimés", categories.get(2), 22, 15,
                "https://images.unsplash.com/photo-1628771065518-0d82f1938462?w=400"));
        medicaments.add(creerMedicament("Oméprazole", "Boîte de 14 gélules", categories.get(3), 6, 12,
                "https://images.unsplash.com/photo-1471864190281-a93a3070b6de?w=400"));
        medicaments.add(creerMedicament("Smecta", "Boîte de 18 sachets", categories.get(3), 11, 10,
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=400"));
        medicaments.add(creerMedicament("Biafine", "Tube 93 g", categories.get(4), 2, 8,
                "https://images.unsplash.com/photo-1587854692152-cbe660dbde88?w=400"));
        medicaments.add(creerMedicament("Crème hydratante", "Tube 50 ml", categories.get(4), 14, 10,
                "https://images.unsplash.com/photo-1556228578-dd6a486868d1?w=400"));

        medicamentRepository.saveAll(medicaments);
    }

    private Medicament creerMedicament(String nom, String quantiteParUnite, Categorie categorie,
            int unitesEnStock, int niveauDeReappro, String imageUrl) {
        Medicament medicament = new Medicament(nom, categorie);
        medicament.setQuantiteParUnite(quantiteParUnite);
        medicament.setPrixUnitaire(BigDecimal.valueOf(1500));
        medicament.setUnitesEnStock(unitesEnStock);
        medicament.setNiveauDeReappro(niveauDeReappro);
        medicament.setImageURL(imageUrl);
        medicament.setIndisponible(false);
        return medicament;
    }
}
