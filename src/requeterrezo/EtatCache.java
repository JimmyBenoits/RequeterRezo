package requeterrezo;

/**
 * Indique la provenance d'un {@link Resultat}. 
 * 
 * @author jimmy.benoits
 *
 */
public enum EtatCache {
	/**
	 * Le r�sultat provient directement du cache.
	 */
	DEPUIS_CACHE,
	/**
	 * Le r�sultat provient d'une requ�te et a �t� plac� dans le cache.
	 */
	NOUVELLE_ENTREE,
	//        MIS_A_JOUR,
	/**
	 * Le r�sultat provient d'une requ�te mais n'a pas �t� plac� dans le cache.
	 */
	EN_ATTENTE,
	/**
	 * Une erreur est survenue lors de la requ�te.
	 */
	ERREUR_REQUETE;
}