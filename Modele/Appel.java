package Modele;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Représente un appel téléphonique dans le système VANAD.
 * Cette classe contient toutes les informations importantes concernant un appel :
 * - Date et heure de réception
 * - Identifiant de l’agent ayant répondu (si existant)
 * - File d’attente concernée
 * - Dates des différentes étapes (réponse, consultation, transfert, fin)
 * Elle fournit aussi des champs dérivés utiles pour les analyses temporelles.
 */
public class Appel
{

    // === Données principales de l’appel ===
    private LocalDateTime dateReceptionAppel;
    private String nomFileAttenteClient;
    private Integer identifiantAgent;

    // === Événements associés à l’appel ===
    private LocalDateTime dateReponseAgent;
    private LocalDateTime dateConsultation;
    private LocalDateTime dateTransfert;
    private LocalDateTime dateRaccrochage;

    private Integer anneeAppel;
    private Integer moisAppel;
    private Integer jourAppel;
    private Integer numeroJourSemaine;
    private Integer heureAppel;
    private Integer minuteAppel;
    private Double tempsDansJournee;

    // Format utilisé uniquement pour le constructeur CSV brut
    private static final DateTimeFormatter formatDate = DateTimeFormatter.ofPattern("d/M/yyyy H:mm:ss");

    // === Constructeurs ===
    public Appel() {}

    public Appel(LocalDateTime dateReception, String fileAttente, Integer agent)
    {
        this.setDateReceptionAppel(dateReception);
        this.nomFileAttenteClient = fileAttente;
        this.identifiantAgent = agent;
    }

    /**
     * Constructeur à partir d’une ligne de fichier CSV brute.
     * Cette méthode tente de parser toutes les colonnes utiles de manière sécurisée.
     */
    public Appel(String[] ligneCSV)
    {
        try {
            if (ligneCSV.length > 0 && !ligneCSV[0].isEmpty())
                this.setDateReceptionAppel(LocalDateTime.parse(ligneCSV[0], formatDate));
            if (ligneCSV.length > 1) this.nomFileAttenteClient = ligneCSV[1];
            if (ligneCSV.length > 2 && !ligneCSV[2].isEmpty())
                this.identifiantAgent = Integer.parseInt(ligneCSV[2]);
            if (ligneCSV.length > 3 && !ligneCSV[3].isEmpty())
                this.dateReponseAgent = LocalDateTime.parse(ligneCSV[3], formatDate);
            if (ligneCSV.length > 4 && !ligneCSV[4].isEmpty())
                this.dateConsultation = LocalDateTime.parse(ligneCSV[4], formatDate);
            if (ligneCSV.length > 5 && !ligneCSV[5].isEmpty())
                this.dateTransfert = LocalDateTime.parse(ligneCSV[5], formatDate);
            if (ligneCSV.length > 6 && !ligneCSV[6].isEmpty())
                this.dateRaccrochage = LocalDateTime.parse(ligneCSV[6], formatDate);
        } catch (DateTimeParseException | NumberFormatException e) {
            System.err.println("Erreur lors de l’analyse d’un appel : " + e.getMessage());
        }
    }


    public LocalDateTime getDateReceptionAppel()
    {
        return dateReceptionAppel;
    }

    /**
     * Définit la date de réception de l’appel et met à jour tous les champs temporels dérivés.
     */
    public void setDateReceptionAppel(LocalDateTime dateReceptionAppel)
    {
        this.dateReceptionAppel = dateReceptionAppel;
        if (dateReceptionAppel != null) {
            this.anneeAppel = dateReceptionAppel.getYear();
            this.moisAppel = dateReceptionAppel.getMonthValue();
            this.jourAppel = dateReceptionAppel.getDayOfMonth();
            this.numeroJourSemaine = dateReceptionAppel.getDayOfWeek().getValue();
            this.heureAppel = dateReceptionAppel.getHour();
            this.minuteAppel = dateReceptionAppel.getMinute();
            this.tempsDansJournee = heureAppel + (minuteAppel / 60.0);
        } else {
            this.anneeAppel = null;
            this.moisAppel = null;
            this.jourAppel = null;
            this.numeroJourSemaine = null;
            this.heureAppel = null;
            this.minuteAppel = null;
            this.tempsDansJournee = null;
        }
    }

    public String getNomFileAttenteClient()
    {
        return nomFileAttenteClient;
    }

    public void setNomFileAttenteClient(String nomFileAttenteClient)
    {
        this.nomFileAttenteClient = nomFileAttenteClient;
    }

    public Integer getIdentifiantAgent()
    {
        return identifiantAgent;
    }

    public void setIdentifiantAgent(Integer identifiantAgent)
    {
        this.identifiantAgent = identifiantAgent;
    }

    public LocalDateTime getDateReponseAgent()
    {
        return dateReponseAgent;
    }

    public void setDateReponseAgent(LocalDateTime dateReponseAgent)
    {
        this.dateReponseAgent = dateReponseAgent;
    }

    public LocalDateTime getDateConsultation()
    {
        return dateConsultation;
    }

    public void setDateConsultation(LocalDateTime dateConsultation)
    {
        this.dateConsultation = dateConsultation;
    }

    public LocalDateTime getDateTransfert()
    {
        return dateTransfert;
    }

    public void setDateTransfert(LocalDateTime dateTransfert)
    {
        this.dateTransfert = dateTransfert;
    }

    public LocalDateTime getDateRaccrochage()
    {
        return dateRaccrochage;
    }

    public void setDateRaccrochage(LocalDateTime dateRaccrochage)
    {
        this.dateRaccrochage = dateRaccrochage;
    }

    public Integer getAnneeAppel()
    {
        return anneeAppel;
    }

    public Integer getMoisAppel()
    {
        return moisAppel;
    }

    public Integer getJourAppel()
    {
        return jourAppel;
    }

    public Integer getNumeroJourSemaine()
    {
        return numeroJourSemaine;
    }

    public Integer getHeureAppel()
    {
        return heureAppel;
    }

    public Integer getMinuteAppel()
    {
        return minuteAppel;
    }

    public Double getTempsDansJournee()
    {
        return tempsDansJournee;
    }

    // === Représentation texte pour l'export ===
    @Override
    public String toString() {
        return " Appel reçu à " + dateReceptionAppel +
                " | File : " + nomFileAttenteClient +
                " | Agent : " + identifiantAgent +
                " | Réponse : " + dateReponseAgent +
                " | Consultation : " + dateConsultation +
                " | Transfert : " + dateTransfert +
                " | Fin : " + dateRaccrochage;
    }
}