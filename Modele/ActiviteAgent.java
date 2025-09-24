package Modele;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Représente une activité d'un agent dans le système VANAD.
 * Elle contient les informations extraites du fichier CSV d'activités (données temporelles et identifiants).
 * Sert à simuler la disponibilité des agents et à enrichir le contexte de simulation.
 */

public class ActiviteAgent
{

    // === Attributs issus du fichier CSV ===
    private Long idActivite;
    private Integer idUtilisateur;
    private Integer idDnd;
    private Integer idCampagne;
    private Integer extension;
    private Integer idDernierAppel;

    // === Informations temporelles ===
    private LocalDateTime debutActivite;
    private LocalDateTime finActivite;
    private Integer idAgent;

    // === Décompositions temporelles pour analyse ===
    private Integer annee;
    private Integer mois;
    private Integer jour;
    private Integer jourSemaine;
    private Integer heure;
    private Integer minute;
    private Integer seconde;
    private Double tempsJournee;       // Heure + minutes en décimal
    private Double dureeMinutes;      // Durée totale de l’activité (en minutes)

    // Alias (pour compatibilité)
    public LocalDateTime aliasDebut;
    public LocalDateTime aliasFin;
    public Integer aliasAgent;

    public ActiviteAgent() {}

    public ActiviteAgent(Long id, Integer agentId, LocalDateTime debut)
    {
        this.idActivite = id;
        this.setIdAgent(agentId);
        this.setDebutActivite(debut);
    }

    // Getters et setters

    public Long getIdActivite()
    {
        return idActivite;
    }

    public void setIdActivite(Long idActivite)
    {
        this.idActivite = idActivite;
    }

    public Integer getIdUtilisateur()
    {
        return idUtilisateur;
    }

    public void setIdUtilisateur(Integer idUtilisateur)
    {
        this.idUtilisateur = idUtilisateur;
    }

    public Integer getIdDnd()
    {
        return idDnd;
    }

    public void setIdDnd(Integer idDnd)
    {
        this.idDnd = idDnd;
    }

    public Integer getIdCampagne()
    {
        return idCampagne;
    }

    public void setIdCampagne(Integer idCampagne)
    {
        this.idCampagne = idCampagne;
    }

    public Integer getExtension()
    {
        return extension;
    }

    public void setExtension(Integer extension)
    {
        this.extension = extension;
    }

    public Integer getIdDernierAppel()
    {
        return idDernierAppel;
    }

    public void setIdDernierAppel(Integer idDernierAppel)
    {
        this.idDernierAppel = idDernierAppel;
    }

    public LocalDateTime getDebutActivite()
    {
        return debutActivite;
    }

    /**
     * Met à jour le début de l’activité et initialise les composants temporels (heure, jour, etc.)
     */
    public void setDebutActivite(LocalDateTime debutActivite)
    {
        this.debutActivite = debutActivite;
        this.aliasDebut = debutActivite;
        if (debutActivite != null) {
            this.annee = debutActivite.getYear();
            this.mois = debutActivite.getMonthValue();
            this.jour = debutActivite.getDayOfMonth();
            this.jourSemaine = debutActivite.getDayOfWeek().getValue();
            this.heure = debutActivite.getHour();
            this.minute = debutActivite.getMinute();
            this.seconde = debutActivite.getSecond();
            this.tempsJournee = heure + (minute / 60.0);
        } else {
            this.annee = null;
            this.mois = null;
            this.jour = null;
            this.jourSemaine = null;
            this.heure = null;
            this.minute = null;
            this.seconde = null;
            this.tempsJournee = null;
        }
        calculerDuree();
    }

    public LocalDateTime getFinActivite()
    {
        return finActivite;
    }

    /**
     * Met à jour la fin de l’activité et recalcule la durée
     */
    public void setFinActivite(LocalDateTime finActivite)
    {
        this.finActivite = finActivite;
        this.aliasFin = finActivite;
        calculerDuree();
    }

    public Integer getIdAgent()
    {
        return idAgent;
    }

    /**
     * Met à jour l’ID agent et l’alias associé
     */
    public void setIdAgent(Integer idAgent)
    {
        this.idAgent = idAgent;
        this.aliasAgent = idAgent;
    }

    public Integer getAnnee()
    {
        return annee;
    }

    public Integer getMois()
    {
        return mois;
    }

    public Integer getJour()
    {
        return jour;
    }

    public Integer getJourSemaine()
    {
        return jourSemaine;
    }

    public Integer getHeure()
    {
        return heure;
    }

    public Integer getMinute()
    {
        return minute;
    }

    public Integer getSeconde()
    {
        return seconde;
    }

    public Double getTempsJournee()
    {
        return tempsJournee;
    }

    public Double getDureeMinutes()
    {
        return dureeMinutes;
    }

    public void setDureeMinutes(Double dureeMinutes)
    {
        this.dureeMinutes = dureeMinutes;
    }

    /**
     * Calcule la durée de l’activité (en minutes) à partir de débutActivite et finActivite.
     */
    private void calculerDuree()
    {
        if (debutActivite != null && finActivite != null) {
            Duration duree = Duration.between(debutActivite, finActivite);
            this.dureeMinutes = duree.toMinutes() + duree.toSecondsPart() / 60.0;
        } else {
            this.dureeMinutes = null;
        }
    }

    /**
     * Retourne une chaîne texte pour le debug et l'affichage.
     */
    @Override
    public String toString()
    {
        return "ActiviteAgent{" +
                "idActivite=" + idActivite +
                ", idAgent=" + idAgent +
                ", debutActivite=" + debutActivite +
                ", finActivite=" + finActivite +
                ", dureeMinutes=" + dureeMinutes +
                '}';
    }

    /**
     * Retourne l'identifiant d'activité en entier
     */
    public Integer obtenirIdActiviteInt() {
        return idActivite != null ? Math.toIntExact(this.idActivite) : null;
    }
}


