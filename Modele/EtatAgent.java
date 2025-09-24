package Modele;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Représente l’état de disponibilité d’un agent dans le centre d’appel.
 * Cet état tient compte :
 * - des services que l’agent est autorisé à traiter,
 * - de sa disponibilité après un certain moment,
 * - d’éventuelles périodes d’indisponibilité ou d’occupation.
 * Utilisé par le moteur de simulation pour déterminer quel agent peut répondre à un appel.
 */
public class EtatAgent
{

    private Set<String> servicesAutorises;
    private LocalDateTime dispoApres;
    private LocalDateTime indispoAvant;
    private LocalDateTime occupeJusquA;

    /**
     * Constructeur principal du statut de l’agent.
     *
     * @param servicesAutorises Ensemble des services que l’agent peut gérer
     */
    public EtatAgent(Set<String> servicesAutorises)
    {
        this.servicesAutorises = servicesAutorises;
        this.dispoApres = LocalDateTime.MIN;
        this.indispoAvant = LocalDateTime.MIN;
        this.occupeJusquA = LocalDateTime.MIN;
    }

    /**
     * Vérifie si l’agent est autorisé à gérer un service donné.
     * @param nomService Nom ou identifiant du service à vérifier
     */
    public boolean accepteService(String nomService)
    {
        return servicesAutorises.contains(nomService);
    }

    /**
     * Détermine si l’agent est disponible à un instant donné,
     * en tenant compte de ses périodes d’indisponibilité et d’occupation.
     * @param moment Instant de vérification
     */
    public boolean estDisponible(LocalDateTime moment)
    {
        return (moment.isAfter(dispoApres) || moment.isEqual(dispoApres))
                && (moment.isAfter(indispoAvant) || moment.isEqual(indispoAvant))
                && (moment.isAfter(occupeJusquA) || moment.isEqual(occupeJusquA));
    }

    // === Getters ===

    public Set<String> getServicesAutorises()
    {
        return servicesAutorises;
    }

    public LocalDateTime getDispoApres()
    {
        return dispoApres;
    }

    public LocalDateTime getIndispoAvant()
    {
        return indispoAvant;
    }

    public LocalDateTime getOccupeJusquA()
    {
        return occupeJusquA;
    }

    // === Setters ===

    public void setServicesAutorises(Set<String> servicesAutorises)
    {
        this.servicesAutorises = servicesAutorises;
    }

    public void setDispoApres(LocalDateTime dispoApres)
    {
        this.dispoApres = dispoApres;
    }

    public void setIndispoAvant(LocalDateTime indispoAvant)
    {
        this.indispoAvant = indispoAvant;
    }

    public void setOccupeJusquA(LocalDateTime occupeJusquA)
    {
        this.occupeJusquA = occupeJusquA;
    }

    // === Affichage lisible pour le débogage ===

    @Override
    public String toString() {
        return "StatutAgent{" +
                "servicesAutorises=" + servicesAutorises +
                ", dispoApres=" + dispoApres +
                ", indispoAvant=" + indispoAvant +
                ", occupeJusquA=" + occupeJusquA +
                '}';
    }
}