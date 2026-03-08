package pharmacie.service;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import pharmacie.dao.FournisseurRepository;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Slf4j
@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;
    private final FournisseurRepository fournisseurRepository;
    private final JavaMailSender mailSender;

    public ApprovisionnementService(MedicamentRepository medicamentRepository,
            FournisseurRepository fournisseurRepository,
            JavaMailSender mailSender) {
        this.medicamentRepository = medicamentRepository;
        this.fournisseurRepository = fournisseurRepository;
        this.mailSender = mailSender;
    }

    /**
     * Lance le processus d'approvisionnement automatique.
     * Identifie les médicaments en rupture de stock et envoie des emails aux
     * fournisseurs.
     *
     * @return Rapport contenant les statistiques de l'opération
     */
    @Transactional(readOnly = true)
    public RapportApprovisionnement lancerApprovisionnement() {
        log.info("Service : Début du processus d'approvisionnement");

        // 1. Trouver tous les médicaments à réapprovisionner
        List<Medicament> medicamentsAReapprovisionner = medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());

        if (medicamentsAReapprovisionner.isEmpty()) {
            log.info("Aucun médicament à réapprovisionner");
            return new RapportApprovisionnement(0, 0, 0, "Aucun médicament à réapprovisionner");
        }

        log.info("Nombre de médicaments à réapprovisionner : {}", medicamentsAReapprovisionner.size());

        // 2. Grouper les médicaments par catégorie
        Map<Categorie, List<Medicament>> medicamentsParCategorie = medicamentsAReapprovisionner.stream()
                .collect(Collectors.groupingBy(Medicament::getCategorie));

        // 3. Trouver tous les fournisseurs concernés
        Set<Categorie> categoriesConcernees = medicamentsParCategorie.keySet();
        Map<Fournisseur, List<Categorie>> categoriesParFournisseur = new HashMap<>();

        for (Fournisseur fournisseur : fournisseurRepository.findAll()) {
            List<Categorie> categoriesFournies = fournisseur.getCategories().stream()
                    .filter(categoriesConcernees::contains)
                    .collect(Collectors.toList());

            if (!categoriesFournies.isEmpty()) {
                categoriesParFournisseur.put(fournisseur, categoriesFournies);
            }
        }

        if (categoriesParFournisseur.isEmpty()) {
            log.warn("Aucun fournisseur trouvé pour les catégories concernées");
            return new RapportApprovisionnement(
                    medicamentsAReapprovisionner.size(),
                    0,
                    0,
                    "Aucun fournisseur disponible pour les catégories concernées");
        }

        // 4. Envoyer un email à chaque fournisseur
        int emailsEnvoyes = 0;
        for (Map.Entry<Fournisseur, List<Categorie>> entry : categoriesParFournisseur.entrySet()) {
            Fournisseur fournisseur = entry.getKey();
            List<Categorie> categories = entry.getValue();

            try {
                envoyerEmailDevis(fournisseur, categories, medicamentsParCategorie);
                emailsEnvoyes++;
                log.info("Email envoyé à {} ({})", fournisseur.getNom(), fournisseur.getEmail());
            } catch (Exception e) {
                log.error("Erreur lors de l'envoi de l'email à {} : {}", fournisseur.getEmail(), e.getMessage());
            }
        }

        log.info("Processus d'approvisionnement terminé : {} emails envoyés sur {} fournisseurs",
                emailsEnvoyes, categoriesParFournisseur.size());

        return new RapportApprovisionnement(
                medicamentsAReapprovisionner.size(),
                categoriesParFournisseur.size(),
                emailsEnvoyes,
                String.format("Approvisionnement lancé avec succès : %d médicaments concernés, %d emails envoyés",
                        medicamentsAReapprovisionner.size(), emailsEnvoyes));
    }

    /**
     * Lancement automatique toutes les 6 heures.
     */
    @Scheduled(fixedDelayString = "${approvisionnement.fixed-delay:21600000}")
    public void lancerApprovisionnementAutomatique() {
        log.info("Déclenchement automatique du service d'approvisionnement");
        lancerApprovisionnement();
    }

    /**
     * Envoie un email de demande de devis à un fournisseur.
     */
    private void envoyerEmailDevis(Fournisseur fournisseur,
            List<Categorie> categories,
            Map<Categorie, List<Medicament>> medicamentsParCategorie) {
        StringBuilder contenu = new StringBuilder();
        contenu.append("Bonjour ").append(fournisseur.getNom()).append(",\n\n");
        contenu.append("Nous avons besoin de réapprovisionner les médicaments suivants.\n");
        contenu.append("Pourriez-vous nous transmettre un devis de réapprovisionnement ?\n\n");

        int totalMedicaments = 0;

        for (Categorie categorie : categories) {
            List<Medicament> medicaments = medicamentsParCategorie.get(categorie);
            if (medicaments != null && !medicaments.isEmpty()) {
                contenu.append("=== ").append(categorie.getLibelle()).append(" ===\n");

                for (Medicament medicament : medicaments) {
                    int quantiteManquante = medicament.getNiveauDeReappro() - medicament.getUnitesEnStock();
                    contenu.append("  - ").append(medicament.getNom())
                            .append(" (").append(medicament.getQuantiteParUnite()).append(")")
                            .append("\n    Stock actuel : ").append(medicament.getUnitesEnStock())
                            .append(" / Seuil : ").append(medicament.getNiveauDeReappro())
                            .append(" / Quantité suggérée : ").append(quantiteManquante)
                            .append("\n");
                    totalMedicaments++;
                }
                contenu.append("\n");
            }
        }

        contenu.append("Nombre total de médicaments à réapprovisionner : ").append(totalMedicaments).append("\n\n");
        contenu.append("Cordialement,\n");
        contenu.append("Service Approvisionnement - Pharmacie");

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(fournisseur.getEmail());
        message.setSubject("Demande de devis - Réapprovisionnement médicaments");
        message.setText(contenu.toString());
        message.setFrom("pharmacie@example.com");

        mailSender.send(message);
    }

    /**
     * DTO pour le rapport d'approvisionnement
     */
    public static class RapportApprovisionnement {
        private final int nombreMedicamentsAReapprovisionner;
        private final int nombreFournisseursConcernes;
        private final int nombreEmailsEnvoyes;
        private final String message;

        public RapportApprovisionnement(int nombreMedicamentsAReapprovisionner,
                int nombreFournisseursConcernes,
                int nombreEmailsEnvoyes,
                String message) {
            this.nombreMedicamentsAReapprovisionner = nombreMedicamentsAReapprovisionner;
            this.nombreFournisseursConcernes = nombreFournisseursConcernes;
            this.nombreEmailsEnvoyes = nombreEmailsEnvoyes;
            this.message = message;
        }

        public int getNombreMedicamentsAReapprovisionner() {
            return nombreMedicamentsAReapprovisionner;
        }

        public int getNombreFournisseursConcernes() {
            return nombreFournisseursConcernes;
        }

        public int getNombreEmailsEnvoyes() {
            return nombreEmailsEnvoyes;
        }

        public String getMessage() {
            return message;
        }
    }
}
