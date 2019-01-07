package requeterrezo;

/**
 * Les annotations sont un moyen dans rezoJDM de poser des informations sur les relations.
 * Ces informations peuvent porter sur la fréquence (toujours vrai, rare, toujours faux, etc.), la pertinence, le caractère humouristique, etc.
 * Une relation peut être annotée plusieurs fois (quelque chose peut être à la fois "toujours vrai" et "non pertinent").
 * Et une annotation peut être elle-même annotée.
 * 
 *  Les annotations sont un outils puissants mais parfois difficile à prendre en main. 
 *  Dans rezoJDM, une annotation transforme une relation en un noeud. RequeterRezo regroupe au même endroit les annotations portant sur les relations
 *  du terme demandé. Cela permet de savoir quelles relations sont annotées. Pour obtenir la valeur de (ou des) annotations ("rare", "pertinent", etc.)
 *  il est nécessaire d'effectuer une nouvelle requête (grâce au "nom" de l'annotation).
 *  
 *  Le nom d'une annotation est composé de ":r" suivi de l'ID de la relation annotée. Par exemple : ":r34672520". Le nom formaté permet de lire
 *  l'annotation : "mâchoire --r_has_part#9:33524--&gt; gencive"
 *  
 *  @see Noeud
 * @author jimmy.benoits
 */
public class Annotation extends Noeud {

	/**
	 * 01/01/2019 - V1.0
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Noeud source de la relation annotée. 
	 */
	protected Noeud source;
	
	/**
	 * Type de la relation annotée.
	 */
    protected int typeRelation;
    
    /**
     * Nom du type de la relation annotée. Cela permet un affichage "formaté" lisible de l'annotation.
     */
    protected String nomTypeRelation;
    
    /**
     * Noeud destination de la relation annotée.
     */
    protected Noeud destination;
    
    /**
     * Poids de la relation annotée.
     */
    protected int poidsRelation;

    /**
     * Construit une annotation à partir d'un noeud annotation (la réification de la relation annotée), d'un noeud source, d'un noeud destination, d'un type et d'un poids.
     * @param nom Nom donnée à l'annotation. Par convention, dans rezoJDM, les annotations sont de la forme ":r" suivi de l'ID de la relation annotée.
     * @param idJDM ID du noeud annotation (réification de la relation annotée).
     * @param type type du noeud annotation (réification de la relation annotée).
     * @param poids poids  du noeud annotation (réification de la relation annotée).
     * @param source Noeud source de la relation annotée.
     * @param typeRelation Type de la relation annotée.
     * @param nomTypeRelation Nom du type de la relation annotée.
     * @param destination Noeud destination de la relation annotée.
     * @param poidsRelation Poids de la relation annotée.
     */
    protected Annotation(
            String nom, long idJDM, int type, int poids,
            Noeud source,
            int typeRelation, String nomTypeRelation,
            Noeud destination,
            int poidsRelation) {
        super(nom, idJDM, type, "[" + source.getNom() + " --" + nomTypeRelation + "#" + typeRelation + ":" + destination.getIdRezo() + "--> " + destination.getNom() + "]", poids);
        this.nomTypeRelation = nomTypeRelation;
        this.source = source;
        this.typeRelation = typeRelation;
        this.destination = destination;
        this.poidsRelation = poidsRelation;
    }
    
    
    @Override
    public String toString() {
        return nom+";"+idRezo+";"+type+";"+ poids + ";" + 
                source.getIdRezo() + ";" + 
                typeRelation + ";"+ nomTypeRelation +";"+ 
                destination.getIdRezo();
    }

}
