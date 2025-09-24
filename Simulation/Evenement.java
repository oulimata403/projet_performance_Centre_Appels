package Simulation;

import java.time.LocalDateTime;

/**
 * Représente un événement dans la simulation du centre d’appel VANAD.
 * Un événement peut être soit un appel reçu, soit une activité d’agent.
 * Ces objets sont insérés dans une file de priorité (ordonnée par date) pour simuler le temps de manière fidèle.
 */
public class Evenement implements Comparable<Evenement>
{
    /**
     * Enumération des types d’événements possibles :
     * - APPEL : représente l’arrivée d’un nouvel appel
     * - ACTIVITE : représente le début d’une activité d’agent
     */
    public enum Type { APPEL, ACTIVITE }

    private Type type;
    private LocalDateTime date;
    private Object objet; // Appel ou ActiviteAgent


    /**
     * Constructeur de la classe Evenement.
     *
     * @param type  Type d’événement (APPEL ou ACTIVITE)
     * @param date  Date à laquelle se produit l’événement
     * @param objet L’objet associé à l’événement (Appel ou ActiviteAgent)
     */
    public Evenement(Type type, LocalDateTime date, Object objet)
    {
        this.type = type;
        this.date = date;
        this.objet = objet;
    }


    public Type getType()
    {
        return type;
    }

    public LocalDateTime getDate()
    {
        return date;
    }

    public Object getObjet()
    {
        return objet;
    }

    // === Ordonnancement des événements ===

    /**
     * Permet de comparer deux événements selon leur date.
     * Utile pour insérer les événements dans une PriorityQueue triée chronologiquement.
     *
     * @param autre L’autre événement à comparer
     * @return négatif si this est avant autre, positif si après, 0 si égal
     */
    @Override
    public int compareTo(Evenement autre) {
        return this.date.compareTo(autre.date);
    }
}