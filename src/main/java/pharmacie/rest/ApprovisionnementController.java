package pharmacie.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import pharmacie.service.ApprovisionnementService;
import pharmacie.service.ApprovisionnementService.RapportApprovisionnement;

@Slf4j
@RestController
@RequestMapping(path = "/api/services/approvisionnement")
public class ApprovisionnementController {

    private final ApprovisionnementService approvisionnementService;

    public ApprovisionnementController(ApprovisionnementService approvisionnementService) {
        this.approvisionnementService = approvisionnementService;
    }

    /**
     * Lance le processus d'approvisionnement.
     *
     * @return Rapport d'exécution
     */
    @PostMapping("lancer")
    public ResponseEntity<RapportApprovisionnement> lancerApprovisionnement() {
        log.info("Contrôleur : Lancement du processus d'approvisionnement");
        RapportApprovisionnement rapport = approvisionnementService.lancerApprovisionnement();
        log.info("Contrôleur : Approvisionnement terminé - {} médicaments, {} emails envoyés",
                rapport.getNombreMedicamentsAReapprovisionner(),
                rapport.getNombreEmailsEnvoyes());
        return ResponseEntity.ok(rapport);
    }
}
