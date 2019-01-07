package requeterrezo;

/**
 * Permet de pauser un filtre sur les requêtes. Les filtres permettent de ne pas retourner les relations entrantes, les relations sortantes
 * ou les deux. Appliquer un filtre permet d'augmenter la rapidité d'exécution des requêtes en ne demandant que les informations nécessaires.
 * @author jimmy.benoits
 */
public enum Filtre {
	/**
	 * Utilisé dans une requête, ce filtre permet de ne pas chercher les relations sortantes et de ne conserver que les relations entrantes.
	 */
	RejeterRelationsSortantes,
	//TODO attention aux annotations lors de la pose de filtre !
	
	/**
	 * Utilisé dans une requête, ce filtre permet de ne pas chercher les relations entrantes et de ne conserver que les relations sortantes.
	 */
	RejeterRelationsEntrantes,
	
	/**
	 * Utilisé dans une requête, ce filtre permet de ne pas chercher les relations sortantes ni les relations entrantes et 
	 * de ne conserver que les informations liées au noeud comme le poids, le type ({@link Noeud}). 
	 */
	RejeterRelationsEntrantesEtSortantes,
	
	/**
	 * Filtre par défaut. Ne rejette aucune relation.
	 */
	AucunFiltre;                
}
