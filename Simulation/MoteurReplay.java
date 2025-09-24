package Simulation;

import Modele.ActiviteAgent;
import Modele.EtatAgent;
import Modele.Appel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Moteur de simulation fidèle du centre d'appels VANAD,
 * reconstituant l'état du système à partir des événements passés (appels, activités d'agents).
 * Utilisé pour générer un jeu d'entraînement réaliste destiné à la prédiction du temps d'attente.
 */
public class MoteurReplay
{

    private final String[] typesServices;
    private final Map<String, Queue<Appel>> filesParService;
    private final Map<Integer, EtatAgent> etatsParAgent;
    private final Map<String, List<Double>> historiquesTempsAttente;
    private final Map<String, List<Double>> historiquesTempsService;
    private final List<ActiviteAgent> activitesChronologiques;
    private int indexActivite = 0;

    private static final Set<Integer> CODES_DISPONIBILITE = Set.of(3, 16);
    private static final Set<Integer> CODES_INDISPONIBILITE = Set.of(2, 7, 8, 35, 39, 40, 41, 42, 43, 44, 61, 71);

    public MoteurReplay(String[] services, List<Appel> appels, List<ActiviteAgent> activites)
    {
        this.typesServices = services;
        this.filesParService = new HashMap<>();
        this.etatsParAgent = new HashMap<>();
        this.historiquesTempsAttente = new HashMap<>();
        this.historiquesTempsService = new HashMap<>();
        this.activitesChronologiques = activites.stream()
                .sorted(Comparator.comparing(ActiviteAgent::getDebutActivite))
                .toList();

        for (String s : services) {
            filesParService.put(s, new LinkedList<>());
            historiquesTempsAttente.put(s, new ArrayList<>());
            historiquesTempsService.put(s, new ArrayList<>());
        }

        initialiserAgentsDepuisAppels(appels);
    }

    // === Initialise les compétences des agents à partir des appels historisés ===
    private void initialiserAgentsDepuisAppels(List<Appel> appels)
    {
        Map<Integer, Set<String>> competencesAgents = appels.stream()
                .filter(appel -> appel.getIdentifiantAgent() != null)
                .collect(Collectors.groupingBy(
                        Appel::getIdentifiantAgent,
                        Collectors.mapping(Appel::getNomFileAttenteClient, Collectors.toSet())
                ));

        for (Map.Entry<Integer, Set<String>> entry : competencesAgents.entrySet()) {
            etatsParAgent.put(entry.getKey(), new EtatAgent(entry.getValue()));
        }

        System.out.println("Agents initialisés : " + etatsParAgent.size());
    }

    // === Capture un snapshot de l'état du système au moment de réception d’un appel ===
    public EtatSysteme capturerEtatSysteme(Appel appel, LocalDateTime horodatage) {
        String file = appel.getNomFileAttenteClient();

        mettreAJourEtatsAgents(horodatage);
        purgerAppelsAnciennementTraites(horodatage);

        int filePrincipale = filesParService.getOrDefault(file, new LinkedList<>()).size();
        int[] autresFiles = Arrays.stream(typesServices)
                .filter(s -> !s.equals(file))
                .mapToInt(s -> filesParService.getOrDefault(s, new LinkedList<>()).size())
                .toArray();
        autresFiles = ajusterTaille(autresFiles, 5);

        int agentsCompatibles = compterAgentsCompatibles(file, horodatage);

        double attenteReelle = -1;
        if (appel.getDateReceptionAppel() != null && appel.getDateReponseAgent() != null) {
            attenteReelle = ChronoUnit.SECONDS.between(appel.getDateReceptionAppel(), appel.getDateReponseAgent());
        }

        EtatSysteme etat = new EtatSysteme(file, filePrincipale, autresFiles, horodatage, Math.max(1, agentsCompatibles));
        etat.setDelaiAttenteObserve(attenteReelle);

        calculerPredicteurs(etat, file);

        return etat;
    }

    // === Réduit ou complète un tableau à une taille fixe ===
    private int[] ajusterTaille(int[] tableau, int tailleFixe)
    {
        int[] resultat = new int[tailleFixe];
        int min = Math.min(tableau.length, tailleFixe);
        System.arraycopy(tableau, 0, resultat, 0, min);
        return resultat;
    }

    // === Retire les appels qui ont déjà été traités à la date courante ===
    private void purgerAppelsAnciennementTraites(LocalDateTime horodatage)
    {
        for (String s : typesServices) {
            Queue<Appel> file = filesParService.get(s);
            file.removeIf(a -> a.getDateReponseAgent() != null && a.getDateReponseAgent().isBefore(horodatage));
        }
    }

    // === Applique les changements d’état des agents jusqu’au moment courant ===
    private void mettreAJourEtatsAgents(LocalDateTime maintenant)
    {
        while (indexActivite < activitesChronologiques.size() &&
                !activitesChronologiques.get(indexActivite).getDebutActivite().isAfter(maintenant)) {

            ActiviteAgent act = activitesChronologiques.get(indexActivite);
            Integer agentId = act.getIdAgent();
            if (agentId != null && etatsParAgent.containsKey(agentId)) {
                EtatAgent etat = etatsParAgent.get(agentId);
                if (CODES_DISPONIBILITE.contains(act.obtenirIdActiviteInt())) {
                    etat.setDispoApres(act.getFinActivite());
                } else if (CODES_INDISPONIBILITE.contains(act.obtenirIdActiviteInt())) {
                    etat.setIndispoAvant(act.getFinActivite());
                }
            }
            indexActivite++;
        }
    }

    // === Nombre d’agents pouvant prendre un appel d’un service donné à une date donnée ===
    private int compterAgentsCompatibles(String file, LocalDateTime temps)
    {
        return (int) etatsParAgent.values().stream()
                .filter(agent -> agent.accepteService(file))
                .filter(agent -> agent.estDisponible(temps))
                .count();
    }

    // === Calcule les prédicteurs LES et Avg-LES pour l’état courant ===
    private void calculerPredicteurs(EtatSysteme etat, String file)
    {
        double tempsMoyenService = calculerMoyenne(historiquesTempsService.get(file), 180.0);
        int nbAgents = Math.max(1, etat.getNombreAgentsLibres());

        double predLes;
        if (etat.getTailleFilePrincipale() == 0) {
            predLes = tempsMoyenService / nbAgents;
        } else {
            double positionMoy = (etat.getTailleFilePrincipale() + 1.0) / 2.0;
            predLes = (positionMoy * tempsMoyenService) / nbAgents;
        }

        double moyenneAttenteRecente = calculerMoyenne(historiquesTempsAttente.get(file), 60.0);
        double facteurCharge = (double) etat.getTailleFilePrincipale() / nbAgents;
        double predAvgLes = moyenneAttenteRecente * (1 + facteurCharge * 0.1);

        etat.setEstimationLES(predLes);
        etat.setEstimationLESMoyenne(predAvgLes);
    }

    // === Enregistre un appel dans la file de son service et met à jour l'état de l’agent ===
    public void enregistrerEvenementAppel(Appel appel)
    {
        String file = appel.getNomFileAttenteClient();
        LocalDateTime reponse = appel.getDateReponseAgent();

        filesParService.get(file).offer(appel);

        if (appel.getIdentifiantAgent() != null && reponse != null) {
            EtatAgent agent = etatsParAgent.get(appel.getIdentifiantAgent());
            if (agent != null) {
                agent.setOccupeJusquA(appel.getDateRaccrochage());
            }

            collecterStatistiques(appel, file);
        }
    }

    // === Stocke les durées d’attente et de service dans l’historique ===
    private void collecterStatistiques(Appel appel, String file)
    {
        if (appel.getDateReceptionAppel() != null && appel.getDateReponseAgent() != null)
        {
            double attente = ChronoUnit.SECONDS.between(appel.getDateReceptionAppel(), appel.getDateReponseAgent());
            enregistrerDansHistorique(historiquesTempsAttente.get(file), attente, 200);
        }

        if (appel.getDateReponseAgent() != null && appel.getDateRaccrochage() != null)
        {
            double service = ChronoUnit.SECONDS.between(appel.getDateReponseAgent(), appel.getDateRaccrochage());
            enregistrerDansHistorique(historiquesTempsService.get(file), service, 200);
        }
    }

    // === Ajoute une valeur à l'historique, avec limite de taille ===
    private void enregistrerDansHistorique(List<Double> liste, double valeur, int limite)
    {
        liste.add(valeur);
        if (liste.size() > limite) liste.remove(0);
    }

    // === Moyenne d’une liste de valeurs, ou valeur par défaut si vide ===
    private double calculerMoyenne(List<Double> liste, double valeurParDefaut)
    {
        return (liste == null || liste.isEmpty())
                ? valeurParDefaut
                : liste.stream().mapToDouble(Double::doubleValue).average().orElse(valeurParDefaut);
    }
}