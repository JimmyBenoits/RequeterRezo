package requeterrezo;

/**
 * Indique l'état de la requête. Particulièrement utile pour les requêtes "live" (effectuées avec {@link RequeterRezoDump}).
 * 
 * @author jimmy.benoits
 *
 */
public enum Etat {
	/**
	 * La chaîne demandée ne correspond à aucun terme dans rezoJDM. Ceci peut provenir d'un problème d'encodage (rezoJDM et RequeterRezo sont
	 * encodé en ISO-8859-1. Le mot résultat sera null.
	 */
	INEXISTANT,
	/**
	 * Le mot résultat a du être tronqué pour des raisons de performance. Lorsque le résultat a plus de 25 000 relations, rezo-dump le tronque
	 * afin de libérer de la ressource. Pour éviter ceci, utilisez plus de filtre (notamment sur les grandes entrées) ou si le problème persiste,
	 * passez à une version "locale" de RequeterRezo ({@link RequeterRezoSQL}).
	 */
	TROP_GROS,
	/**
	 * Lorsqu'un mot trop gros n'est pas en cache, il arrive que la première requête n'aboutisse pas. Le mot résultat sera null. Mais ré-itérer 
	 * la requête devrait permettre d'obtenir un résultat (probablement {@link Etat#TROP_GROS}). 
	 */
	RENVOYER,
	/**
	 * Etat retourné lorsque le service rezo-dump (http://www.jeuxdemots.org/rezo-dump.php) est inacessible. Le mot résultat sera null.
	 */
	SERVEUR_INACCESSIBLE,
	/**
	 * Etat de base lorsque la requête s'est déroulée normalement.
	 */
	OK,
	/**
	 * Autre erreur lors de la requête. Cela peut provenir notamment d'un filtre sur un type inconnu par RequeterRezo.
	 */
	ERREUR_REQUETE;
}