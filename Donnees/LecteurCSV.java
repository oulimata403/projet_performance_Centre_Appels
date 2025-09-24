package Donnees;

import com.opencsv.CSVReader;
import Modele.Appel;
import Modele.ActiviteAgent;

import java.io.FileReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Classe utilitaire pour lire les fichiers CSV contenant les appels clients
 * et les activités des agents au format VANAD, et les convertir en objets Java.
 */

public class LecteurCSV
{

    // Format standard utilisé pour parser les dates présentes dans les CSV
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Lit un fichier CSV contenant les appels clients et retourne une liste d'objets Appel.
     */
    public static List<Appel> lireAppels(String cheminFichier) throws Exception
    {
        List<Appel> appels = new ArrayList<>();
        try (CSVReader lecteur = new CSVReader(new FileReader(cheminFichier))) {
            String[] ligne;
            int numeroLigne = 1;
            lecteur.readNext();

            while ((ligne = lecteur.readNext()) != null) {
                numeroLigne++;
                try {
                    Appel appel = new Appel();

                    appel.setDateReceptionAppel(parseDateSecurisee(ligne, 0, "date_received", numeroLigne, false));
                    appel.setNomFileAttenteClient(parseChaineSecurisee(ligne, 1));
                    appel.setIdentifiantAgent(parseNombreEntierFlexible(ligne, 2, "agent_number", numeroLigne));

                    appel.setDateReponseAgent(parseDateSecurisee(ligne, 3, "answered", numeroLigne, true));
                    appel.setDateConsultation(parseDateSecurisee(ligne, 4, "consult", numeroLigne, true));
                    appel.setDateTransfert(parseDateSecurisee(ligne, 5, "transfer", numeroLigne, true));
                    appel.setDateRaccrochage(parseDateSecurisee(ligne, 6, "hangup", numeroLigne, true));

                    appels.add(appel);
                } catch (Exception e) {
                    System.err.printf("[APPEL] Erreur de parsing ligne %d : %s%n", numeroLigne, Arrays.toString(ligne));
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[APPEL] Nombre total d'appels lus : " + appels.size());
        return appels;
    }

    /**
     * Lit un fichier CSV contenant les activités des agents
     * et retourne une liste d'objets ActiviteAgent.
     */
    public static List<ActiviteAgent> lireActivites(String cheminFichier) throws Exception
    {
        List<ActiviteAgent> activites = new ArrayList<>();
        int numeroLigne = 1;
        try (CSVReader lecteur = new CSVReader(new FileReader(cheminFichier))) {
            String[] ligne;
            lecteur.readNext();

            while ((ligne = lecteur.readNext()) != null) {
                numeroLigne++;
                try {
                    ActiviteAgent activite = new ActiviteAgent();

                    activite.setIdActivite(parseLongSecurise(ligne, 0, "id", numeroLigne));
                    activite.setIdUtilisateur(parseEntierSecurise(ligne, 1, "user_id", numeroLigne));
                    activite.setIdDnd(parseEntierSecurise(ligne, 2, "dnd_id", numeroLigne));
                    activite.setIdCampagne(parseEntierSecurise(ligne, 3, "campaign_id", numeroLigne));
                    activite.setExtension(parseEntierSecurise(ligne, 4, "extension", numeroLigne));
                    activite.setIdDernierAppel(parseEntierSecurise(ligne, 5, "last_call_id", numeroLigne));

                    activite.setDebutActivite(parseDateSecurisee(ligne, 6, "startdatetime", numeroLigne, false));
                    activite.setFinActivite(parseDateSecurisee(ligne, 7, "enddatetime", numeroLigne, true));
                    activite.setIdAgent(parseEntierSecurise(ligne, 8, "agent_id", numeroLigne));

                    activites.add(activite);
                } catch (Exception e) {
                    System.err.printf("[ACTIVITÉ] Erreur de parsing ligne %d : %s%n", numeroLigne, Arrays.toString(ligne));
                    e.printStackTrace();
                }
            }
        }
        System.out.println("[ACTIVITÉ] Nombre total d'activités lues : " + activites.size());
        return activites;
    }



    // ==================== Méthodes utilitaires internes ====================

    /** Parse une date en toute sécurité, avec gestion des erreurs et valeurs nullables */
    private static LocalDateTime parseDateSecurisee(String[] champs, int index, String nomChamp, int ligne, boolean peutEtreNull) {
        try {
            if (champVide(champs, index)) {
                if (peutEtreNull) return null;
                throw new IllegalArgumentException("Date obligatoire manquante");
            }
            return LocalDateTime.parse(champs[index].trim(), FORMAT_DATE);
        } catch (Exception e) {
            if (peutEtreNull) return null;
            System.err.printf("[ERREUR] Parsing date '%s' ligne %d : '%s'%n", nomChamp, ligne, champSecurise(champs, index));
            throw e;
        }
    }

    /** Lit un champ texte et retourne null si vide */
    private static String parseChaineSecurisee(String[] champs, int index)
    {
        return champVide(champs, index) ? null : champs[index].trim();
    }

    /** Parse un entier simple avec message d'erreur clair si échec */
    private static Integer parseEntierSecurise(String[] champs, int index, String nomChamp, int ligne) {
        try {
            if (champVide(champs, index)) return null;
            return Integer.parseInt(champs[index].trim());
        } catch (NumberFormatException e) {
            System.err.printf("[WARN] Erreur entier '%s' ligne %d : '%s'%n", nomChamp, ligne, champSecurise(champs, index));
            return null;
        }
    }


    /** Parse des entiers qui peuvent être écrits sous forme décimale */
    private static Integer parseNombreEntierFlexible(String[] champs, int index, String nomChamp, int ligne) {
        try {
            if (champVide(champs, index)) return null;
            String valeur = champs[index].trim();
            if (valeur.contains(".")) {
                double v = Double.parseDouble(valeur);
                return (int) Math.round(v);
            } else {
                return Integer.parseInt(valeur);
            }
        } catch (Exception e) {
            System.err.printf("[WARN] Erreur nombre-entier '%s' ligne %d : '%s'%n", nomChamp, ligne, champSecurise(champs, index));
            return null;
        }
    }

    /** Parse un identifiant long avec gestion d'erreur */
    private static Long parseLongSecurise(String[] champs, int index, String nomChamp, int ligne)
    {
        try {
            if (champVide(champs, index)) return null;
            return Long.parseLong(champs[index].trim());
        } catch (NumberFormatException e) {
            System.err.printf("[WARN] Erreur long '%s' ligne %d : '%s'%n", nomChamp, ligne, champSecurise(champs, index));
            return null;
        }
    }

    /** Vérifie si un champ CSV est vide ou absent */
    private static boolean champVide(String[] champs, int index)
    {
        return champs.length <= index || champs[index] == null || champs[index].trim().isEmpty();
    }

    /** Récupère une valeur "sûre" pour l'affichage (évite NullPointer) */
    private static String champSecurise(String[] champs, int index)
    {
        return (champs.length > index && champs[index] != null) ? champs[index] : "null";
    }
}