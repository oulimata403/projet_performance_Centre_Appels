package Simulation;

import Donnees.LecteurCSV;
import Modele.ActiviteAgent;
import Modele.Appel;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;

/**
 * Simulation du centre d'appel VANAD avec moteur de replay temporel fidèle.
 * Cette classe orchestre :
 * 1. La lecture et préparation des fichiers de données (appels, activités),
 * 2. L'exécution de la simulation (replay historique),
 * 3. La génération des données d'entraînement,
 * 4. L’analyse des performances des prédicteurs.
 */
public class SimulationCentreAppels
{

    private String[] servicesPrincipaux;
    private List<Appel> appelsFiltres;
    private List<ActiviteAgent> activitesAgents;
    private final List<EtatSysteme> donneesEntrainement;
    private MoteurReplay moteurReplay;

    public SimulationCentreAppels()
    {
        this.donneesEntrainement = new ArrayList<>();
    }

    /**
     * Lance le replay historique fidèle du centre d'appel.
     *
     * @param cheminFichierAppels   Chemin du fichier CSV des appels
     * @param cheminFichierActivites Chemin du fichier CSV des activités agents
     */
    public void lancerReplayHistorique(String cheminFichierAppels, String cheminFichierActivites) throws Exception {
        System.out.println(">>> Initialisation du replay fidèle des appels historiques <<<");

        chargerEtPreparerDonnees(cheminFichierAppels, cheminFichierActivites);
        initialiserMoteurReplay();
        executerReplayEvenementParEvenement();
        exporterDonneesEntrainement();
        analyserResultatsSimulation();

        System.out.println(">>> Replay terminé avec succès <<<");
    }

    /**
     * Charge, filtre, trie et prépare les données pour la simulation.
     */
    private void chargerEtPreparerDonnees(String cheminAppels, String cheminActivites) throws Exception {
        System.out.println("Chargement des données depuis les fichiers CSV...");

        List<Appel> appelsBruts = LecteurCSV.lireAppels(cheminAppels);
        System.out.println("Nombre total d'appels chargés : " + appelsBruts.size());

        // === Filtrage par jours et heures d'ouverture (lundi–vendredi, 08h–20h) ===
        appelsBruts = appelsBruts.stream()
                .filter(appel -> {
                    LocalDateTime dt = appel.getDateReceptionAppel();
                    if (dt == null) return false;
                    DayOfWeek jour = dt.getDayOfWeek();
                    LocalTime heure = dt.toLocalTime();
                    boolean jourOuvre = (jour != DayOfWeek.SATURDAY && jour != DayOfWeek.SUNDAY);
                    boolean horaireOuvre = !heure.isBefore(LocalTime.of(8, 0)) && !heure.isAfter(LocalTime.of(20, 0));
                    return jourOuvre && horaireOuvre;
                })
                .collect(Collectors.toList());

        System.out.println("Appels après filtrage horaire (jours + heures ouvrées) : " + appelsBruts.size());

        // === Calcul des volumes d'appels par service ===
        Map<String, Long> volumesParService = appelsBruts.stream()
                .filter(appel -> appel.getNomFileAttenteClient() != null)
                .filter(appel -> appel.getDateReceptionAppel() != null
                        && appel.getDateReponseAgent() != null
                        && appel.getDateRaccrochage() != null)
                .collect(Collectors.groupingBy(Appel::getNomFileAttenteClient, Collectors.counting()));

        servicesPrincipaux = volumesParService.entrySet().stream()
                .filter(e -> e.getValue() >= 200)
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(Map.Entry::getKey)
                .toArray(String[]::new);

        System.out.println("Services principaux retenus : " + Arrays.toString(servicesPrincipaux));

        appelsFiltres = appelsBruts.stream()
                .filter(appel -> Arrays.asList(servicesPrincipaux).contains(appel.getNomFileAttenteClient()))
                .filter(appel -> appel.getDateReceptionAppel() != null
                        && appel.getDateReponseAgent() != null
                        && appel.getDateRaccrochage() != null)
                .sorted(Comparator.comparing(Appel::getDateReceptionAppel))
                .collect(Collectors.toList());

        activitesAgents = LecteurCSV.lireActivites(cheminActivites).stream()
                .filter(a -> a.getDebutActivite() != null && a.getFinActivite() != null)
                .sorted(Comparator.comparing(ActiviteAgent::getDebutActivite))
                .collect(Collectors.toList());

        System.out.println("Appels filtrés pour simulation : " + appelsFiltres.size());
        System.out.println("Activités agents chargées : " + activitesAgents.size());
    }

    /**
     * Initialise le moteur de replay avec les données chargées.
     */
    private void initialiserMoteurReplay() {
        moteurReplay = new MoteurReplay(servicesPrincipaux, appelsFiltres, activitesAgents);
    }

    /**
     * Exécute la simulation appel par appel.
     */
    private void executerReplayEvenementParEvenement()
    {
        int totalAppels = appelsFiltres.size();
        int compteur = 0;

        for (Appel appel : appelsFiltres) {
            EtatSysteme etat = moteurReplay.capturerEtatSysteme(appel, appel.getDateReceptionAppel());

            if (echantillonValide(etat)) {
                donneesEntrainement.add(etat);
            }

            moteurReplay.enregistrerEvenementAppel(appel);
            compteur++;

            // Affichage allégé pour suivi de la progression
            if (compteur % 200000 == 0 || compteur == totalAppels) {
                System.out.printf("Progression : %d/%d appels traités (%.1f%%)%n",
                        compteur, totalAppels, 100.0 * compteur / totalAppels);
            }
        }
    }

    /**
     * Vérifie si un échantillon est valide pour l'entraînement.
     */
    private boolean echantillonValide(EtatSysteme etat) {
        return etat.getDelaiAttenteObserve() >= 0 &&
                etat.getDelaiAttenteObserve() < 7200 &&
                etat.getTailleFilePrincipale() < 500 &&
                etat.getNombreAgentsLibres() > 0;
    }

    /**
     * Exporte les données d'entraînement dans un fichier CSV.
     */
    private void exporterDonneesEntrainement() throws IOException {
        String fichierSortie = "jeu_donnees_ann_vanad.csv";
        System.out.println("Export des données d'entraînement vers : " + fichierSortie);

        try (FileWriter fw = new FileWriter(fichierSortie)) {
            fw.append(String.join(",", EtatSysteme.nomsColonnesCSV())).append(",attente_reelle\n");

            for (EtatSysteme etat : donneesEntrainement) {
                double[] vecteur = etat.transformerEnVecteurCaracteristiques();
                for (double valeur : vecteur) {
                    fw.append(String.format(Locale.US, "%.2f", valeur)).append(",");
                }
                fw.append(String.format(Locale.US, "%.2f", etat.getDelaiAttenteObserve())).append("\n");
            }
        }
    }

    /**
     * Analyse les résultats générés par la simulation.
     */
    private void analyserResultatsSimulation() {
        System.out.println("=== ANALYSE DES DONNÉES SIMULÉES ===");

        if (donneesEntrainement.isEmpty()) {
            System.out.println("Aucun échantillon généré.");
            return;
        }

        EtatSysteme exemple = donneesEntrainement.get(0);
        System.out.println("Exemple d’échantillon simulé :");
        System.out.println(exemple);

        double moyenneAttente = donneesEntrainement.stream()
                .mapToDouble(EtatSysteme::getDelaiAttenteObserve).average().orElse(0);
        double moyenneFile = donneesEntrainement.stream()
                .mapToDouble(EtatSysteme::getTailleFilePrincipale).average().orElse(0);

        System.out.printf("Nombre total d’échantillons : %d%n", donneesEntrainement.size());
        System.out.printf("Temps d’attente moyen : %.1f secondes%n", moyenneAttente);
        System.out.printf("Longueur moyenne de la file d’attente : %.1f%n", moyenneFile);

        validerPredicteurs();
    }

    /**
     * Calcule et affiche les RMSE des prédicteurs LES et Avg-LES.
     */
    private void validerPredicteurs() {
        System.out.println("=== VALIDATION DES PRÉDICTEURS LES ET AVG-LES ===");

        double mseLes = donneesEntrainement.stream()
                .mapToDouble(e -> Math.pow(e.getDelaiAttenteObserve() - e.getEstimationLES(), 2))
                .average().orElse(0);

        double mseAvg = donneesEntrainement.stream()
                .mapToDouble(e -> Math.pow(e.getDelaiAttenteObserve() - e.getEstimationLESMoyenne(), 2))
                .average().orElse(0);

        double moyenneAttente = donneesEntrainement.stream()
                .mapToDouble(EtatSysteme::getDelaiAttenteObserve).average().orElse(1);

        double rmseLes = Math.sqrt(mseLes);
        double rmseAvg = Math.sqrt(mseAvg);

        System.out.printf("Prédicteur LES : RMSE = %.2f secondes, RRMSE = %.3f%n", rmseLes, rmseLes / moyenneAttente);
        System.out.printf("Prédicteur Avg-LES : RMSE = %.2f secondes, RRMSE = %.3f%n", rmseAvg, rmseAvg / moyenneAttente);
    }
}