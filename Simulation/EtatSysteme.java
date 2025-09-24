package Simulation;

import Modele.Appel;
import Modele.ActiviteAgent;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Classe représentant l’état instantané du système de centre d’appel VANAD.
 * Cette classe joue un double rôle :
 * 1. Gérer dynamiquement les agents libres et la file d'attente durant la simulation.
 * 2. Capturer un vecteur de caractéristiques à chaque appel pour l’entraînement d’un modèle de prédiction du délai d’attente.
 */
public class EtatSysteme
{

    // === État dynamique du système pendant la simulation ===
    private Set<Integer> identifiantsAgentsLibres = new HashSet<>();
    private Queue<Appel> fileAppelsEnAttente = new LinkedList<>();


    // === Variables utilisées pour générer des vecteurs de caractéristiques ===
    private String libelleService;
    private int tailleFilePrincipale;
    private int[] taillesFilesAnnexes;
    private LocalDateTime horodatageAppel;
    private int nombreAgentsLibres;
    private double delaiAttenteObserve;
    private double estimationLES;
    private double estimationLESMoyenne;

    // === Constructeurs ===

    public EtatSysteme() {}

    public EtatSysteme(String libelleService, int tailleFilePrincipale, int[] taillesFilesAnnexes,
                       LocalDateTime horodatageAppel, int nombreAgentsLibres) {
        this.libelleService = libelleService;
        this.tailleFilePrincipale = tailleFilePrincipale;
        this.taillesFilesAnnexes = taillesFilesAnnexes;
        this.horodatageAppel = horodatageAppel;
        this.nombreAgentsLibres = nombreAgentsLibres;
    }

    // === Gestion d’un appel entrant dans la simulation ===

    /**
     * Met à jour l'état du système à la réception d’un nouvel appel :
     * - Affecte un agent libre si disponible
     * - Sinon, ajoute l’appel à la file d’attente
     * - Met à jour la file principale et le nombre d’agents libres
     */
    public void enregistrerAppel(Appel nouvelAppel) {
        this.horodatageAppel = nouvelAppel.getDateReceptionAppel();
        this.libelleService = nouvelAppel.getNomFileAttenteClient();

        if (!identifiantsAgentsLibres.isEmpty()) {
            Integer agentAssigne = identifiantsAgentsLibres.iterator().next();
            identifiantsAgentsLibres.remove(agentAssigne);
            System.out.println("Appel reçu à " + nouvelAppel.getDateReceptionAppel() + " pris en charge par l’agent " + agentAssigne);
        } else {
            fileAppelsEnAttente.add(nouvelAppel);
            System.out.println("Appel reçu à " + nouvelAppel.getDateReceptionAppel() + " mis en attente (file d’attente)");
        }

        this.tailleFilePrincipale = fileAppelsEnAttente.size();
        this.nombreAgentsLibres = identifiantsAgentsLibres.size();
    }

    // === Enregistrement de la disponibilité d’un agent via une nouvelle activité ===

    /**
     * Met à jour l’état de l’agent à partir de son activité :
     * - Ajoute l’agent dans les agents libres
     * - Si des appels sont en attente, en affecte un à cet agent
     */
    public void enregistrerActiviteAgent(ActiviteAgent nouvelleActivite) {
        Integer identifiantAgent = nouvelleActivite.getIdAgent();
        if (identifiantAgent == null) return;

        identifiantsAgentsLibres.add(identifiantAgent);
        System.out.println("Agent " + identifiantAgent + " signalé libre à " + nouvelleActivite.getDebutActivite());

        if (!fileAppelsEnAttente.isEmpty()) {
            Appel appelExtrait = fileAppelsEnAttente.poll();
            System.out.println("Appel en file traité par l’agent " + identifiantAgent + " (appel initial à " + appelExtrait.getDateReceptionAppel() + ")");
            identifiantsAgentsLibres.remove(identifiantAgent);
        }

        this.tailleFilePrincipale = fileAppelsEnAttente.size();
        this.nombreAgentsLibres = identifiantsAgentsLibres.size();
    }

    // === Encodage d’un état sous forme de vecteur de caractéristiques ===

    /**
     * Transforme l’état courant du système en vecteur numérique pour la prédiction.
     * Utilisé pour entraîner ou tester un modèle supervisé (par ex. ANN).
     */
    public double[] transformerEnVecteurCaracteristiques() {
        double service_30175 = "30175".equals(libelleService) ? 1.0 : 0.0;
        double service_30560 = "30560".equals(libelleService) ? 1.0 : 0.0;
        double service_30172 = "30172".equals(libelleService) ? 1.0 : 0.0;
        double service_30181 = "30181".equals(libelleService) ? 1.0 : 0.0;
        double service_30179 = "30179".equals(libelleService) ? 1.0 : 0.0;

        double heure = horodatageAppel != null ? horodatageAppel.getHour() : 0.0;
        double jour = horodatageAppel != null ? horodatageAppel.getDayOfWeek().getValue() : 0.0;

        double fileAnnexe1 = (taillesFilesAnnexes != null && taillesFilesAnnexes.length > 0) ? taillesFilesAnnexes[0] : 0.0;
        double fileAnnexe2 = (taillesFilesAnnexes != null && taillesFilesAnnexes.length > 1) ? taillesFilesAnnexes[1] : 0.0;

        return new double[]{
                service_30175, service_30560, service_30172, service_30181, service_30179,
                tailleFilePrincipale, fileAnnexe1, fileAnnexe2,
                heure, jour,
                nombreAgentsLibres,
                estimationLES,
                estimationLESMoyenne
        };
    }

    /**
     * Renvoie les noms des colonnes correspondant aux valeurs du vecteur de caractéristiques.
     */
    public static String[] nomsColonnesCSV() {
        return new String[]{
                "service_30175", "service_30560", "service_30172",
                "service_30181", "service_30179",
                "taille_file_principale", "file_1", "file_2",
                "heure_arrivee", "jour_semaine",
                "agents_libres", "PLES", "Pavg_LES"
        };
    }

    // === Getters & Setters ===

    public String getLibelleService()
    {
        return libelleService;
    }

    public void setLibelleService(String libelleService)
    {
        this.libelleService = libelleService;
    }

    public int getTailleFilePrincipale()
    {
        return tailleFilePrincipale;

    }
    public void setTailleFilePrincipale(int tailleFilePrincipale)
    {
        this.tailleFilePrincipale = tailleFilePrincipale;
    }

    public int[] getTaillesFilesAnnexes()
    {
        return taillesFilesAnnexes;
    }

    public void setTaillesFilesAnnexes(int[] taillesFilesAnnexes)
    {
        this.taillesFilesAnnexes = taillesFilesAnnexes;
    }

    public LocalDateTime getHorodatageAppel()
    {
        return horodatageAppel;
    }

    public void setHorodatageAppel(LocalDateTime horodatageAppel)
    {
        this.horodatageAppel = horodatageAppel;
    }

    public int getNombreAgentsLibres()
    {
        return nombreAgentsLibres;
    }

    public void setNombreAgentsLibres(int nombreAgentsLibres)
    {
        this.nombreAgentsLibres = nombreAgentsLibres;
    }

    public double getDelaiAttenteObserve()
    {
        return delaiAttenteObserve;
    }

    public void setDelaiAttenteObserve(double delaiAttenteObserve)
    {
        this.delaiAttenteObserve = delaiAttenteObserve;
    }

    public double getEstimationLES()
    {
        return estimationLES;
    }

    public void setEstimationLES(double estimationLES)
    {
        this.estimationLES = estimationLES;
    }

    public double getEstimationLESMoyenne()
    {
        return estimationLESMoyenne;
    }

    public void setEstimationLESMoyenne(double estimationLESMoyenne)
    {
        this.estimationLESMoyenne = estimationLESMoyenne;
    }

    // === Représentation texte pour le debogage ===
    @Override
    public String toString() {
        return "ContexteSimulation{" +
                "tailleFilePrincipale=" + tailleFilePrincipale +
                ", agentsLibres=" + identifiantsAgentsLibres.size() +
                ", libelleService='" + libelleService + '\'' +
                ", horodatageAppel=" + horodatageAppel +
                '}';
    }
}