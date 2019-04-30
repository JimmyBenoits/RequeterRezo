package requeterrezo;

import java.io.Serializable;


/*
RequeterRezo
Copyright (C) 2019  Jimmy Benoits

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/


/**
 * Représente une relation "voisine" d'un terme. 
 * Un voisin est associé à un type et à un mot. Ces trois éléments permettent de construire une relation de rezoJDM. 
 * Exemple, la relation ["chat" r_can_eat "souris"] de poids 235 donnera :
 * Dans les relations sortantes du mot "chat", le voisin "souris, 235" sera présent dans la liste des voisins associés à la valeur 102. 
 * 
 * @see Mot#getRelationsEntrantes()
 * @see Mot#getRelationsSortantes() 
 * @author jimmy.benoits
 */
public class Voisin implements Serializable{

	/**
	 * 30/04/2019 - V1.1
	 */
	private static final long serialVersionUID = 11L;
	
	/**
	 * Noeud voisin.
	 */
	protected final Noeud mot;
	
	/**
	 * Poids de la relation.
	 */
    protected final int poids;
    
    /**
     * id de la relation
     */
    protected final long idRelation;

    /**
     * Constructeur.
     * @param mot Noeud voisin.
     * @param poids Poids de la relation.
     * @param idRelation Identifiant rezoJDM de la relation.
     */
    protected Voisin(Noeud mot, int poids, long idRelation) {
        this.mot = mot;
        this.poids = poids;
        this.idRelation = idRelation;
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
     * Retourne le poids de la relation liant le voisin au mot requêté.
     *
     * @return Retourne le poids de la relation liant le voisin connexe au mot
     * requêté.
     */
    public int getPoids() {
        return this.poids;
    }
    
    /**
     * Retourne l'id de la relation rezoJDM liant le voisin au mot requêté.
     * @return L'id de la relation rezoJDM liant le voisin au mot requêté.
     */
    public long getIDRelation() {
    	return this.idRelation;
    }

    @Override
    public String toString() {
        return this.mot.getNom() + "=" + this.poids;
    }

}
