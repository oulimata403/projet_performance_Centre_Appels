package Simulation;

import Donnees.LecteurCSV;
import Modele.Appel;
import Modele.ActiviteAgent;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TestSimulation {

    public static void main(String[] args) {
        try {
            // Lecture des fichiers CSV
            List<Appel> appels = LecteurCSV.lireAppels("data_csv/donnees_appels_2014_nettoyees.csv");
            List<ActiviteAgent> activites = LecteurCSV.lireActivites("data_csv/donnees_activites_2014_nettoyees.csv");

            // Création et tri des événements
            List<Evenement> evenements = new ArrayList<>();
            for (Appel a : appels) {
                if (a.getDateReceptionAppel() != null)
                    evenements.add(new Evenement(Evenement.Type.APPEL, a.getDateReceptionAppel(), a));
            }
            for (ActiviteAgent act : activites) {
                if (act.getDebutActivite() != null)
                    evenements.add(new Evenement(Evenement.Type.ACTIVITE, act.getDebutActivite(), act));
            }
            Collections.sort(evenements);

            // Initialisation simulation
            EtatSysteme etat = new EtatSysteme();
            etat.setLibelleService("30175");
            etat.setTailleFilePrincipale(0);
            etat.setNombreAgentsLibres(10);
            etat.setTaillesFilesAnnexes(new int[]{0, 0});

            // Paramètre de lissage LES
            final double alpha = 0.2;
            double lesPrecedent = 0.0;
            double sommeLES = 0.0;
            int compteurLES = 0;

            // Écriture du CSV avec gestion d'exception
            try (PrintWriter writer = new PrintWriter(new FileWriter("etat_systeme_sortie.csv"))) {

                // Écriture de l'en-tête
                String[] nomsColonnes = EtatSysteme.nomsColonnesCSV();
                writer.print(String.join(",", nomsColonnes));
                writer.println(",temps_attente_actuel");

                // Boucle de simulation
                for (Evenement e : evenements) {
                    if (e.getType() == Evenement.Type.APPEL) {
                        Appel appel = (Appel) e.getObjet();
                        etat.enregistrerAppel(appel);

                        double tempsAttente = -1.0;
                        if (appel.getDateReceptionAppel() != null && appel.getDateReponseAgent() != null) {
                            Duration diff = Duration.between(appel.getDateReceptionAppel(), appel.getDateReponseAgent());
                            tempsAttente = diff.toSeconds() / 60.0;
                        }
                        etat.setDelaiAttenteObserve(tempsAttente);

                        // LES & Moyenne LES
                        if (tempsAttente >= 0) {
                            double lesActuel = alpha * tempsAttente + (1 - alpha) * lesPrecedent;
                            lesPrecedent = lesActuel;
                            sommeLES += lesActuel;
                            compteurLES++;
                            etat.setEstimationLES(lesActuel);
                            etat.setEstimationLESMoyenne(sommeLES / compteurLES);
                        } else {
                            etat.setEstimationLES(0.0);
                            etat.setEstimationLESMoyenne(0.0);
                        }

                        // Écriture ligne CSV
                        double[] caracteristiques = etat.transformerEnVecteurCaracteristiques();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < caracteristiques.length; i++) {
                            if (i == 11 || i == 12) {
                                sb.append(String.format(Locale.US, "%.3f", caracteristiques[i]));
                            } else {
                                sb.append(caracteristiques[i]);
                            }
                            if (i < caracteristiques.length - 1)
                                sb.append(",");
                        }
                        sb.append(",").append(String.format(Locale.US, "%.3f", tempsAttente));
                        writer.println(sb);

                    } else if (e.getType() == Evenement.Type.ACTIVITE) {
                        ActiviteAgent activite = (ActiviteAgent) e.getObjet();
                        etat.enregistrerActiviteAgent(activite);
                    }
                }

                System.out.println("Simulation terminée : Fichier etat_systeme_sortie.csv généré avec succès.");

            } catch (Exception e) {
                System.err.println("Erreur lors de l'écriture du fichier CSV : " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception ex) {
            System.err.println("Erreur globale de simulation : " + ex.getMessage());
            ex.printStackTrace();
        }
    }
}
