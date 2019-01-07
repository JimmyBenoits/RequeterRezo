package requeterrezo;

/**
 * Exception liée à l'interprétation d'un fichier de configuration pour RequeterRezo.
 * @author jimmy.benoits
 */
@SuppressWarnings("serial")
public class ConfigurationException extends Exception{

	/**
	 * Créé une ConfigurationException avec un message d'erreur.
	 * @param messageErreur Le message d'erreur.
	 */
	public ConfigurationException(String messageErreur) {
		super(messageErreur);
	}

}
