package requeterrezo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * La classe Mot est l'objet principal retourné par une requête dans RequeterRezo. 
 * Un mot contient notamment les informations sur le noeud rezoJDM associé ({@link Noeud}) 
 * ainsi que les relations entrantes, sortantes et les annotations.
 * Si un mot a été construit par une requête "live" ({@link RequeterRezoDump}), alors sa définition (si elle existe) est aussi retournée.
 * 
 * @author jimmy.benoits
 */
public class Mot extends Noeud implements Serializable{

	/**
	 * 01/01/2019 - V1.0
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Définition du mot dans rezoJDM si la requête est effectuée avec {@link RequeterRezoDump}.
	 */
	protected String definition = "";
	
	/**
	 * Table d'association des noeuds du voisinage du terme (id -&gt; {@link Noeud}).
	 */
	protected HashMap<Long, Noeud> voisinage;
	
	/**
	 * Table d'assocation des relations entrantes (dont le Mot est la destination).
	 * La table lie un type de relation et une liste de voisin ({@link Voisin}).
	 */
	protected HashMap<Integer, ArrayList<Voisin>> relationsEntrantes;
	
	/**
	 * Table d'assocation des relations sortantes (dont le Mot est la source).
	 * La table lie un type de relation et une liste de voisin ({@link Voisin}).
	 */
	protected HashMap<Integer, ArrayList<Voisin>> relationsSortantes;
	
	/**
	 * Liste des annotations portant sur les relations du mot.
	 */
	protected ArrayList<Annotation> annotations;
	
	/**
	 * Descriptif interne de la requête.
	 */
	protected final CleCache cleCache;

	
	/**
	 * Constructeur paramétré. 
	 * @param nom Nom du terme dans rezoJDM.
	 * @param id ID du terme dans rezoJDM.
	 * @param type type du terme dans rezoJDM.
	 * @param mot_formate mot formaté du terme dans rezoJDM.
	 * @param poids poids du terme dans rezoJDM.
	 * @param description définition du terme dans rezoJDM.
	 * @param voisinage Table d'association du voisinnage d'un terme (id -> {@link Noeud}).
	 * @param relationsEntrantes Table d'assocation des relations entrantes (dont le Mot est la destination).
	 * @param relationsSortantes Table d'assocation des relations sortantes (dont le Mot est la source).
	 * @param annotations Liste des annotations portant sur les relations du mot.
	 * @param cleCache Descriptif interne de la requête.
	 */
	protected Mot(String nom, long id, int type, String mot_formate, int poids, String description,
			HashMap<Long, Noeud> voisinage,
			HashMap<Integer, ArrayList<Voisin>> relationsEntrantes,
			HashMap<Integer, ArrayList<Voisin>> relationsSortantes,
			ArrayList<Annotation> annotations,
			CleCache cleCache) {
		super(nom, id, type, mot_formate, poids);
		this.voisinage = voisinage;
		this.relationsEntrantes = relationsEntrantes;
		this.relationsSortantes = relationsSortantes;
		this.definition = description;
		this.annotations = annotations;
		this.cleCache = cleCache;
	}


	/**
	 * Définition du mot dans rezoJDM si la requête est effectuée avec {@link RequeterRezoDump}.
	 * @return La définition du mot si cela est possible.
	 */
	public String getDefinition() {
		return definition;
	}

	/**
	 * Modifie la définition (en local) du terme.
	 * @param definition Définition du terme.
	 */
	public void setDefinition(String definition) {
		this.definition = definition;
	}

	/**
	 * Table d'association des noeuds du voisinage du terme (id -&gt; {@link Noeud}).
	 * @return retourne le voisinage du mot.
	 */
	public HashMap<Long, Noeud> getVoisinage() {
		return voisinage;
	}

	/**
	 * Retourne les voisins ({@link Voisin}) du terme pour les relations entrantes dont le nom de la relation est passé en paramètre.
	 * Une relation entrante est une relation dont la destination est le mot.
	 * @param nomType Nom de la relation.
	 * @return Les voisins ({@link Voisin}) du terme pour les relations entrantes dont le nom de la relation est passé en paramètre. S'il n'y en a aucun,
	 * ou si le nom de correspond pas à une relation existante, retourne une liste vide.
	 */
	public ArrayList<Voisin> getRelationsEntrantesTypees(String nomType){
		ArrayList<Voisin> resultats;
		Integer type = RequeterRezo.correspondancesRelations.get(nomType);
		if(type != null) {
			resultats = getRelationsEntrantesTypees(type);
		}else {
			resultats = new ArrayList<>();
		}
		return resultats;
	}

	/**
	 * Retourne les voisins ({@link Voisin}) du terme pour les relations sortantes dont le nom de la relation est passé en paramètre.
	 * Une relation sortante est une relation dont la source est le mot.
	 * @param nomType Nom de la relation.
	 * @return Les voisins ({@link Voisin}) du terme pour les relations sortantes dont le nom de la relation est passé en paramètre. S'il n'y en a aucun,
	 * ou si le nom de correspond pas à une relation existante, retourne une liste vide.
	 */
	public ArrayList<Voisin> getRelationsSortantesTypees(String nomType){
		ArrayList<Voisin> resultats;
		Integer type = RequeterRezo.correspondancesRelations.get(nomType);
		if(type != null) {
			resultats = getRelationsSortantesTypees(type);
		}else {
			resultats = new ArrayList<>();
		}
		return resultats;
	}


	/**
	 * Retourne les voisins ({@link Voisin}) du terme pour les relations entrantes dont le type de la relation est passé en paramètre.
	 * Une relation entrante est une relation dont la destination est le mot.
	 * @param type Type de la relation.
	 * @return Les voisins ({@link Voisin}) du terme pour les relations entrantes dont le type de la relation est passé en paramètre. S'il n'y en a aucun,
	 * ou si le nom de correspond pas à une relation existante, retourne une liste vide.
	 */
	public ArrayList<Voisin> getRelationsEntrantesTypees(int type){
		ArrayList<Voisin> resultats;
		if((resultats = relationsEntrantes.get(type))==null) {
			resultats = new ArrayList<>();
		}
		return resultats;
	}

	/**
	 * Retourne les voisins ({@link Voisin}) du terme pour les relations sortantes dont le type de la relation est passé en paramètre.
	 * Une relation sortante est une relation dont la destination est le mot.
	 * @param type Type de la relation.
	 * @return Les voisins ({@link Voisin}) du terme pour les relations sortantes dont le type de la relation est passé en paramètre. S'il n'y en a aucun,
	 * ou si le nom de correspond pas à une relation existante, retourne une liste vide.
	 */
	public ArrayList<Voisin> getRelationsSortantesTypees(int type){
		ArrayList<Voisin> resultats;
		if((resultats = relationsSortantes.get(type))==null) {
			resultats = new ArrayList<>();
		}
		return resultats;
	}

	/**
	 * Retourne la table d'association des relations entrantes.
	 * Une relation entrantes est une relation dont le mot est la destination.
	 * La clé correspond au type de la relation et la valeur à la liste des voisins ({@link Voisin}). 
	 * @return La table d'association des relations entrantes.
	 */
	public HashMap<Integer, ArrayList<Voisin>> getRelationsEntrantes() {
		return relationsEntrantes;
	}

	/**
	 * Retourne la table d'association des relations sortantes.
	 * Une relation sortantes est une relation dont le mot est la source.
	 * La clé correspond au type de la relation et la valeur à la liste des voisins ({@link Voisin}). 
	 * @return La table d'association des relations sortantes.
	 */
	public HashMap<Integer, ArrayList<Voisin>> getRelationsSortantes() {
		return relationsSortantes;
	}

	/**
	 * Retourne la liste des annotations ({@link Annotation}) portant sur les relations du mot.
	 * @return la liste des annotations ({@link Annotation}) portant sur les relations du mot.
	 */
	public ArrayList<Annotation> getAnnotations() {
		return annotations;
	}

	
	/**
	 * Fusionne deux mots dans un seul. Cette fonction peut être utile lorsque l'on a effectué deux requêtes filtrées sur un même terme.
	 * Cela permet de regrouper les deux résultats dans un même objet.
	 * @param mot1 Premier mot à fusionner. 
	 * @param mot2 Second mot à fusionner.
	 * @return Si les deux mots sont bien le même terme, retourne l'union de leur voisinage. Sinon, retourne le premier mot.
	 */
	public static Mot fusion(Mot mot1, Mot mot2) {
		ArrayList<Mot> mots = new ArrayList<>(2);
		mots.add(mot1);
		mots.add(mot2);
		return fusion(mots);
	}

	/**
	 * Fusionne une liste de mot dans un seul. Cette fonction peut être utile lorsque l'on a effectué plusieurs requêtes filtrées sur un même terme.
	 * Cela permet de regrouper les résultats dans un même objet.
	 * @param mots Liste de mots à fusionner.
	 * @return Retourne la fusion (l'union des voisinages) des termes identiques au premier mot de la liste.
	 */
	public static Mot fusion(ArrayList<Mot> mots) {
		Mot res = null;
		if(!mots.isEmpty()) {
			res = mots.get(0);
			HashMap<Integer, ArrayList<Voisin>> entrantes = res.getRelationsEntrantes();
			HashMap<Integer, ArrayList<Voisin>> sortantes = res.getRelationsSortantes();
			ArrayList<Annotation> annotations = res.getAnnotations();
			Mot intermediaire;
			int id_relation;
			ArrayList<Voisin> voisins, voisins_intermediaire;
			for(int i = 1; i < mots.size(); ++i) {
				intermediaire = mots.get(i);
				if(intermediaire.getNom().equals(res.getNom())) {
					for(Entry<Integer, ArrayList<Voisin>> entry : intermediaire.getRelationsEntrantes().entrySet()) {
						id_relation = entry.getKey();
						voisins_intermediaire = entry.getValue();
						if((voisins=entrantes.get(id_relation)) == null) {
							entrantes.put(id_relation, voisins_intermediaire);
						}else {
							for(Voisin voisin : voisins_intermediaire) {
								if(!voisins.contains(voisin)) {
									voisins.add(voisin);
								}
							}
						}
					}
					for(Entry<Integer, ArrayList<Voisin>> entry : intermediaire.getRelationsSortantes().entrySet()) {
						id_relation = entry.getKey();
						voisins_intermediaire = entry.getValue();
						if((voisins=sortantes.get(id_relation)) == null) {
							sortantes.put(id_relation, voisins_intermediaire);
						}else {
							for(Voisin voisin : voisins_intermediaire) {
								if(!voisins.contains(voisin)) {
									voisins.add(voisin);
								}
							}
						}
					}
					for(Annotation annotation : intermediaire.getAnnotations()) {
						if(!annotations.contains(annotation)) {
							annotations.add(annotation);
						}
					}
				}
			}
		}		
		return res;
	}

}
