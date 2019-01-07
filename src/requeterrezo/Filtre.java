package requeterrezo;

/**
 * Permet de pauser un filtre sur les requ�tes. Les filtres permettent de ne pas retourner les relations entrantes, les relations sortantes
 * ou les deux. Appliquer un filtre permet d'augmenter la rapidit� d'ex�cution des requ�tes en ne demandant que les informations n�cessaires.
 * @author jimmy.benoits
 */
public enum Filtre {
	/**
	 * Utilis� dans une requ�te, ce filtre permet de ne pas chercher les relations sortantes et de ne conserver que les relations entrantes.
	 */
	RejeterRelationsSortantes,
	//TODO attention aux annotations lors de la pose de filtre !
	
	/**
	 * Utilis� dans une requ�te, ce filtre permet de ne pas chercher les relations entrantes et de ne conserver que les relations sortantes.
	 */
	RejeterRelationsEntrantes,
	
	/**
	 * Utilis� dans une requ�te, ce filtre permet de ne pas chercher les relations sortantes ni les relations entrantes et 
	 * de ne conserver que les informations li�es au noeud comme le poids, le type ({@link Noeud}). 
	 */
	RejeterRelationsEntrantesEtSortantes,
	
	/**
	 * Filtre par d�faut. Ne rejette aucune relation.
	 */
	AucunFiltre;                
}
