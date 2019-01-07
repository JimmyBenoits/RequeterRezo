package requeterrezo;

import java.io.Serializable;
import java.util.Date;

/**
 * Contient la date de la derni�re demande ainsi que le nombre de demande d'une requ�te en attente.
 * 
 *  @see Attente
 * @author jimmy.benoits
 */
class AttenteInfo implements Serializable{

	/**
	 * 01/01/2019 - V1.0
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Nombre d'occurrences de la requ�te (depuis sa premi�re rencontre, m�me si elle est pass�e par le cache en attendant).
	 * Si un nombre d'heure sup�rieur au d�lais de p�remption s'est �coul�e depuis la derni�re demande, le compteur est r�initialis�.
	 */
	protected int occurrences;
	
	/**
	 * Date de la derni�re occurrence.
	 */
	protected Date dateOccurrences;

	/**
	 * Incr�mente le nombre d'occurence. 
	 * Si un nombre d'heure sup�rieur au d�lais de p�remption s'est �coul�e depuis la derni�re demande, le compteur est r�initialis�.
	 * @param peremption
	 */
    protected void incrementeOccurrences(int peremption) {
        if (Utils.perime(dateOccurrences, peremption)) {
            occurrences = 1;
        } else {
            ++occurrences;
        }
        dateOccurrences = new Date();
    }

    /**
     * Constructeur param�tr� 
     * @param occurrences Nombre d'occurrence de la requ�te.
     * @param date_occurrences Date de la dern�re occurrence.
     */
    protected AttenteInfo(int occurrences, Date date_occurrences) {
        this.occurrences = occurrences;
        this.dateOccurrences = date_occurrences;
    }
    
    /**
     * Constructeur par d�faut. Le nombre d'occurrence de base est fix� � 1 et une nouvelle date est cr��e.
     */
    protected AttenteInfo() {
        occurrences = 1;
        dateOccurrences = new Date();
    }
    
    @Override
    public String toString() {       
        return occurrences + ";" + Utils.formatDate.format(dateOccurrences);
    }
}
