package requeterrezo;

import java.io.Serializable;

/**
 * Repr�sente une relation "voisine" d'un terme. 
 * Un voisin est associ� � un type et � un mot. Ces trois �l�ments permettent de construire une relation de rezoJDM. 
 * Exemple, la relation ["chat" r_can_eat "souris"] de poids 235 donnera :
 * Dans les relations sortantes du mot "chat", le voisin "souris, 235" sera pr�sent dans la liste des voisins associ�s � la valeur 102. 
 * 
 * @see Mot#getRelationsEntrantes()
 * @see Mot#getRelationsSortantes() 
 * @author jimmy.benoits
 */
public class Voisin implements Serializable{

	/**
	 * 01/01/2019 - V1.0
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Noeud voisin.
	 */
	protected final Noeud mot;
	
	/**
	 * Poids de la relation.
	 */
    protected final int poids;

    /**
     * Constructeur.
     * @param mot Noeud voisin.
     * @param poids Poids de la relation.
     */
    protected Voisin(Noeud mot, int poids) {
        this.mot = mot;
        this.poids = poids;
    }

    /**
     * Retourne le noeud voisin.
     *
     * @return Le noeud voisin.
     */
    public Noeud getNoeud() {
        return mot;
    }

    /**
     * Retourne le nom du noeud voisin.
     *
     * @return Le nom du noeud voisin.
     */
    public String getNom() {
        return mot.getNom();
    }

    /**
     * Retourne le poids de la relation liant le voisin au mot requ�t�.
     *
     * @return Retourne le poids de la relation liant le voisin connexe au mot
     * requ�t�.
     */
    public int getPoids() {
        return this.poids;
    }

    @Override
    public String toString() {
        return this.mot.getNom() + "=" + this.poids;
    }

}
