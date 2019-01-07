package requeterrezo;

/**
 * Indique l'�tat de la requ�te. Particuli�rement utile pour les requ�tes "live" (effectu�es avec {@link RequeterRezoDump}).
 * 
 * @author jimmy.benoits
 *
 */
public enum Etat {
	/**
	 * La cha�ne demand�e ne correspond � aucun terme dans rezoJDM. Ceci peut provenir d'un probl�me d'encodage (rezoJDM et RequeterRezo sont
	 * encod� en ISO-8859-1. Le mot r�sultat sera null.
	 */
	INEXISTANT,
	/**
	 * Le mot r�sultat a du �tre tronqu� pour des raisons de performance. Lorsque le r�sultat a plus de 25 000 relations, rezo-dump le tronque
	 * afin de lib�rer de la ressource. Pour �viter ceci, utilisez plus de filtre (notamment sur les grandes entr�es) ou si le probl�me persiste,
	 * passez � une version "locale" de RequeterRezo ({@link RequeterRezoSQL}).
	 */
	TROP_GROS,
	/**
	 * Lorsqu'un mot trop gros n'est pas en cache, il arrive que la premi�re requ�te n'aboutisse pas. Le mot r�sultat sera null. Mais r�-it�rer 
	 * la requ�te devrait permettre d'obtenir un r�sultat (probablement {@link Etat#TROP_GROS}). 
	 */
	RENVOYER,
	/**
	 * Etat retourn� lorsque le service rezo-dump (http://www.jeuxdemots.org/rezo-dump.php) est inacessible. Le mot r�sultat sera null.
	 */
	SERVEUR_INACCESSIBLE,
	/**
	 * Etat de base lorsque la requ�te s'est d�roul�e normalement.
	 */
	OK,
	/**
	 * Autre erreur lors de la requ�te. Cela peut provenir notamment d'un filtre sur un type inconnu par RequeterRezo.
	 */
	ERREUR_REQUETE;
}