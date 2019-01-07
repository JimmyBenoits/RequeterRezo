package requeterrezo;

/**
 * Indique la provenance d'un {@link Resultat}. 
 * 
 * @author jimmy.benoits
 *
 */
public enum EtatCache {
	/**
	 * Le résultat provient directement du cache.
	 */
	DEPUIS_CACHE,
	/**
	 * Le résultat provient d'une requête et a été placé dans le cache.
	 */
	NOUVELLE_ENTREE,
	//        MIS_A_JOUR,
	/**
	 * Le résultat provient d'une requête mais n'a pas été placé dans le cache.
	 */
	EN_ATTENTE,
	/**
	 * Une erreur est survenue lors de la requête.
	 */
	ERREUR_REQUETE;
}