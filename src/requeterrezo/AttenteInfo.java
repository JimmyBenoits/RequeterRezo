package requeterrezo;

import java.io.Serializable;
import java.util.Date;

/**
 * Contient la date de la dernière demande ainsi que le nombre de demande d'une requête en attente.
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
	 * Nombre d'occurrences de la requête (depuis sa première rencontre, même si elle est passée par le cache en attendant).
	 * Si un nombre d'heure supérieur au délais de péremption s'est écoulée depuis la dernière demande, le compteur est réinitialisé.
	 */
	protected int occurrences;
	
	/**
	 * Date de la dernière occurrence.
	 */
	protected Date dateOccurrences;

	/**
	 * Incrémente le nombre d'occurence. 
	 * Si un nombre d'heure supérieur au délais de péremption s'est écoulée depuis la dernière demande, le compteur est réinitialisé.
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
     * Constructeur paramétré 
     * @param occurrences Nombre d'occurrence de la requête.
     * @param date_occurrences Date de la dernère occurrence.
     */
    protected AttenteInfo(int occurrences, Date date_occurrences) {
        this.occurrences = occurrences;
        this.dateOccurrences = date_occurrences;
    }
    
    /**
     * Constructeur par défaut. Le nombre d'occurrence de base est fixé à 1 et une nouvelle date est créée.
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
