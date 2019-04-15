package requeterrezo;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;


/*
RequeterRezo
Copyright (C) 2019  Jimmy Benoits

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */


/**
 * Classe mère de la classe principale de RequeterRezo. Regroupe les comportements partagés par la version "live" ({@link RequeterRezoDump})
 * qui effectue ses requêtes sur le service rezo-dump (www.jeuxdemots.org/rezo-dump.php) et la version "locale" ({@link RequeterRezoSQL}) qui
 * les effectue sur un serveur MySQL hébergeant les données de rezoJDM.
 * 
 * Les paramètres principaux de RequeterRezo sont : 
 * - La taille maximale du cache : nombre d'octet à partir duquel le système n'acceptera plus de nouvelles entrées dans le cache sans en faire
 * sortir une autre. Cela a pour effet de limiter la taille du cache mais le calcul exact pourra être légèrement plus élevé que le nombre prévu.
 * La valeur par défaut est fixée à 100mo.
 * 
 * - Le délais de péremption : nombre d'heure à partir duquel un élément du cache est jugé obsolète. RezoJDM est une ressource vivante et les
 * éléments changent régulièrement. C'est pour cela qu'il est parfois nécessaire de redemander un mot. La fréquence doit être définie par l'utilisateur
 * en fonction de ses besoins (être toujours à jour avec rezoJDM est-il très important ?).
 * La valeur par défaut est fixé à 168h (7 jours).
 * 
 * De nombreux constructeurs et méthodes pour effectuées des requêtes sont disponibles. Il est également possible de construire votre objet
 * à partir d'un fichier de configuration ".ini" dont un exemplaire est fourni avec cette archive. Passer par un fichier de configuration débloque
 * des paramétrages non-disponible autrement comme le mode avancé (qui laisse le soin à l'utilisateur d'appeler lui-même la méthode 
 * {@link RequeterRezo#sauvegarder()}, ce qui est déconseillé) ou de changer le chemin de stockage du cache.
 * 
 * Un fichier Exemple.java est normalement distribuée avec RequeterRezo. Il explique les premiers pas pour commencer à utiliser
 * RequeterRezo.
 * @author jimmy.benoits
 */
public abstract class RequeterRezo {

	/**
	 * Chemin par défaut du dossier du cache.
	 */
	protected final static String CHEMIN_CACHE_DEFAUT = "cache";

	/**
	 * Chemin par défaut du fichier contenant la sérialisation de l'index d'attente ({@link Attente}).
	 */
	protected final static String FICHIER_ATTENTE_DEFAUT = CHEMIN_CACHE_DEFAUT + File.separator + "indexAttente";

	/**
	 * Chemin par défaut du fichier contenant la sérialisation de l'index du cache ({@link Cache}).
	 */
	protected final static String FICHIER_CACHE_DEFAUT = CHEMIN_CACHE_DEFAUT + File.separator + "indexCache";

	/**
	 * Taille maximale par défaut du cache (en octet).
	 */
	protected final static long TAILLE_CACHE_DEFAUT = 100_000_000;

	/**
	 * Délais de péremption par défaut (en heure).
	 */
	protected final static int PEREMPTION_DEFAUT = 168;

	/**
	 * Etat par défaut du mode avancé.
	 */
	protected final static boolean MODE_AVANCE_DEFAUT = false;

	/**
	 * Etat par défaut de l'affichage des messages.
	 */
	protected final static boolean AVERTISSEMENT_DEFAUT = true;

	/**
	 * Permet la correspondance entre le nom et le type des relations de rezoJDM.
	 */
	protected static CorrespondanceRelation correspondancesRelations = new CorrespondanceRelation();
	
	/**
	 * Mots en attente.
	 */
	protected Attente attente;

	/**
	 * Mots stockés dans le cache.
	 */
	protected Cache cache;

	/**
	 * Taille maximale du cache (en octet).
	 */
	protected final long tailleCache;

	/**
	 * Nombre d'heure à partir duquel un fichier du cache est considéré comme périmé. 
	 */
	protected final int peremption;

	/**
	 * Le mode avancé désactive la sauvegarde automatique des index. L'utilisateur doit lui-même sauvegarder régulièrement grâce à la méthode
	 * {@link RequeterRezo#sauvegarder()}.
	 */
	protected boolean modeAvance;  

	/**
	 * Permet (ou non) à RequeterRezo d'afficher des messages sur System.err.
	 */
	protected boolean avertissement;

	/**
	 * Chemin vers le dossier du cache.
	 */
	protected static String cheminCache= CHEMIN_CACHE_DEFAUT;

	/**
	 * Chemin vers le fichier contenant l'index des mots en attentes.
	 */
	protected static String fichierAttente = FICHIER_ATTENTE_DEFAUT;

	/**
	 * Chemin vers le fichier contenant l'index des mots dans le cache.
	 */
	protected static String fichierCache = FICHIER_CACHE_DEFAUT;

	/**
	 * Constructeur paramétré utilisant la taille maximale du cache, le délais de péremption et la possibilité (ou non) d'afficher
	 * des messages d'avertissement sur System.err.
	 * @param tailleCache Taille maximale du cache. Attent un nombre suivi d'une unité ("ko", "mo", "go", avec "ko" par défaut).
	 * @param peremption Délais de péremption. Attent un nombre suivi d'une unité ('j' pour journée ou 'h' pour heure, 'h' par défaut).
	 * @param avertissement True si le système est autorisé à envoyer des messages sur System.err, false sinon.
	 */
	public RequeterRezo(String tailleCache, String peremption, boolean avertissement) {
		this.avertissement = avertissement;
		String suffixe;
		int multiplieur;
		long tailleCacheLong;
		//Partie taille cache
		if (tailleCache.length() > 2) {
			suffixe = tailleCache.substring(tailleCache.length() - 2);
			tailleCache = tailleCache.substring(0, tailleCache.length() - 2);
			//cas de base = ko           
			switch (suffixe.toLowerCase()) {
			case "mo": {
				multiplieur = 1_000_000;
				break;
			}
			case "go": {
				multiplieur = 1_000_000_000;
				break;
			}
			default: {
				multiplieur = 1_000;
				break;
			}
			}
			try {
				tailleCacheLong = Long.parseLong(tailleCache);
			} catch (NumberFormatException e) {
				tailleCacheLong = TAILLE_CACHE_DEFAUT;
				multiplieur = 1;
			}
			this.tailleCache = tailleCacheLong * multiplieur;
		} else {
			this.tailleCache = TAILLE_CACHE_DEFAUT;
		}

		//partie peremption
		char dernierChar;
		int peremptionInt;
		if (peremption.length() > 1) {
			dernierChar = peremption.charAt(peremption.length() - 1);
			peremption = peremption.substring(0, peremption.length() - 1);
			//cas de base = h
			multiplieur = 1;
			if (dernierChar == 'j') {
				multiplieur = 24;
			}
			try {
				peremptionInt = Integer.parseInt(peremption);
			} catch (NumberFormatException e) {
				peremptionInt = PEREMPTION_DEFAUT;
				multiplieur = 1;
			}
			this.peremption = peremptionInt * multiplieur;
		} else {
			this.peremption = PEREMPTION_DEFAUT;
		}
		initialiserCache();
		initialiserAttente();
	}

	/**
	 * Constructeur paramétré utilisant la taille maximale du cache et le délais de péremption.
	 * @param tailleCache Taille maximale du cache. Attent un nombre suivi d'une unité ("ko", "mo", "go", avec "ko" par défaut).
	 * @param peremption Délais de péremption. Attent un nombre suivi d'une unité ('j' pour journée ou 'h' pour heure, 'h' par défaut).
	 */
	public RequeterRezo(String tailleCache, String peremption) {		
		this(tailleCache, peremption, AVERTISSEMENT_DEFAUT);
	}

	/**
	 * Constructeur paramétré utilisant la taille maximale du cache et le délais de péremption.
	 * @param tailleCache Taille maximale du cache (en octet).
	 * @param peremption Délais de péremption (en heure).
	 */
	public RequeterRezo(long tailleCache, int peremption) {
		this(tailleCache,peremption,AVERTISSEMENT_DEFAUT);
	}

	/**
	 * Constructeur paramétré utilisant la taille maximale du cache, le délais de péremption et la possibilité (ou non) d'afficher
	 * des messages d'avertissement sur System.err.
	 * @param tailleCache Taille maximale du cache (en octet).
	 * @param peremption Délais de péremption (en heure).
	 * @param avertissement True si le système est autorisé à envoyer des messages sur System.err, false sinon.
	 */
	public RequeterRezo(long tailleCache, int peremption, boolean avertissement) {
		this.tailleCache = tailleCache;
		this.peremption = peremption;
		this.avertissement = avertissement;
		this.modeAvance = MODE_AVANCE_DEFAUT;
		initialiserCache();
		initialiserAttente();
	}

	/**
	 * Constructeur par défaut - utilise les valeurs par défaut.
	 */
	public RequeterRezo() {
		this(TAILLE_CACHE_DEFAUT,PEREMPTION_DEFAUT,AVERTISSEMENT_DEFAUT);
	}

	/**
	 * Constructeur paramétré utilisant la taille maximale du cache, le délais de péremption, la possibilité (ou non) d'afficher
	 * des messages d'avertissement sur System.err, l'activation (ou non) du mode avancé et le chemin vers le dossier de cache. 
	 * Ce constructeur n'est utilisable qu'en passant par un fichier de configuration. 
	 * @param tailleCache Taille maximale du cache (en octet).
	 * @param peremption Délais de péremption (en heure).
	 * @param avertissement True si le système est autorisé à envoyer des messages sur System.err, false sinon.
	 */
	private RequeterRezo(long tailleCache, int peremption, boolean avertissement, boolean modeAvance, String cheminCache) {
		this.tailleCache = tailleCache;
		this.peremption = peremption;
		this.avertissement = avertissement;
		this.modeAvance = modeAvance;
		RequeterRezo.cheminCache = cheminCache;
		RequeterRezo.fichierAttente = cheminCache + File.separator + "indexAttente";
		RequeterRezo.fichierCache = cheminCache + File.separator + "indexCache";
		initialiserCache();
		initialiserAttente();
	}

	/**
	 * Construit un objet RequeterRezo à partir d'une {@link Configuration}.
	 * @param configuration Configuration a utiliser.
	 */
	public RequeterRezo(Configuration configuration) {
		this(configuration.getTailleCache(), 
				configuration.getPeremption(), 
				configuration.getAvertissement(),
				configuration.getModeAvance(),
				configuration.getCheminCache());
	}

	/**
	 * Construit les équivalences entre nom et type de relation. La façon de construire est différente suivant que l'on travaille en "live" ({@link RequeterRezoDump}) 
	 * ou en "local" ({@link RequeterRezoSQL}).
	 */
	protected abstract void construireRelations();

	/**
	 * Effectue la requête lorsque le cache ne possède pas l'élément demande. La façon de construire le mot est différente suivant 
	 * que l'on travaille en "live" ({@link RequeterRezoDump}) ou en "local" ({@link RequeterRezoSQL}).
	 * @param cacheKey Requête à effectuer.
	 * @return Le resultat de la requête.
	 */
	protected abstract Resultat construireMot(CleCache cacheKey);

	/**
	 * Sauvegarde les index permettant le bon fonctionnement du cache d'une exécution sur l'autre. 
	 * Cette méthode n'est utile seulement si le mode avancé a été activé (ce qui est fortement déconseillé).	
	 */
	public void sauvegarder() {
		cache.sauvegarderCache(fichierCache);
		attente.sauvegarderAttente(fichierAttente);
	}

	/**
	 * Initialise l'index des mots en attente soit à partir d'une exécution précédente soit en recommençant de 0.
	 */
	protected final void initialiserAttente() {
		File dossierCache = new File(cheminCache);
		if(!dossierCache.exists()) {
			dossierCache.mkdir();
			attente = new Attente();
		}else {
			File fichier = new File(fichierAttente);
			if(!fichier.exists()) {
				attente = new Attente();
			}else {
				attente = Attente.chargerAttente(fichierAttente);
				if(attente == null) {
					if(avertissement) {
						System.err.println("Avertissement RequeterRezo : l'index des mots en attentes est illisible");
					}
					attente = new Attente();
				}
			}			
		}
	}

	/**
	 * Initialise l'index des mots du cache soit à partir d'une exécution précédente soit en recommençant de 0.
	 * Une vérification est effectuée ({@link Cache#verifierIntegrite(String, boolean)} si le chargement se fait depuis un cache existant. 
	 */
	protected final void initialiserCache() {
		File dossierCache = new File(cheminCache);
		if(!dossierCache.exists()) {
			dossierCache.mkdir();
			cache = new Cache(peremption, tailleCache);
		}else {
			File fichier = new File(fichierCache);
			if(!fichier.exists()) {
				cache = new Cache(peremption, tailleCache);
			}else {
				cache = Cache.chargerCache(fichierCache);
				if(cache == null) {
					if(avertissement) {
						System.err.println("Avertissement RequeterRezo : l'index du cache est illisible");
					}
					cache = new Cache(peremption, tailleCache);
				}
				cache.verifierIntegrite(cheminCache, avertissement);
			}	
		}

	}


	/**
	 * Requête complète à partir d'un terme, d'un type de relation de rezoJDM et d'un filtre.
	 * @param mot Terme de rezoJDM.
	 * @param typeRelation Type de la relation dont on cherche le voisinage pour "mot".
	 * @param filtre Filtre à appliquer.
	 * @return Le résultat de la requête.
	 */
	public Resultat requete(String mot, int typeRelation, Filtre filtre) {
		CleCache cleCache = new CleCache(mot, typeRelation, filtre);
		Resultat resultat = requeteInterne(cleCache);
		if(!modeAvance) {
			sauvegarder();
		}
		return resultat;
	}	

	/**
	 * Requête à partir du terme. Retourne toutes les informations présentes dans le voisinage de ce terme. 
	 * Pour les mots de taille importante, il est vivement déconseillé d'utiliser une requête de la sorte mais plutôt de passer 
	 * par des filtres (type des relations ou entrants / sortants).
	 * @param mot Terme à rechercher dans rezoJDM.
	 * @return Le résultat de la requête sur le terme demandé, sans aucun filtre.
	 */
	public Resultat requete(String mot) {
		return requete(mot, -1, Filtre.AucunFiltre);
	}

	/**
	 * Requête à partir du terme et d'un filtre sur les relations entrantes, sortantes ou les deux.
	 * Pour les mots de taille importante, il est conseillé d'appliquer aussi un filtre sur le type de relation.
	 * @param mot Terme à rechercher dans rezoJDM.
	 * @param filtre Filtre à appliquer (entrants / sortants).
	 * @return Le résultat de la requête sur le terme demandé, avec un filtre sur la direction des relations. 
	 */
	public Resultat requete(String mot, Filtre filtre) {
		return requete(mot, -1, filtre);
	}

	/**
	 * Requête à partir du terme et d'un filtre sur le nom de relation.
	 * @param mot Terme à rechercher dans rezoJDM.
	 * @param typeRelation Type de la relation dont il faut extraire le voisinage.
	 * @return Le résultat de la requête sur le terme demandé, avec un filtre sur le types des relations. 
	 */
	public Resultat requete(String mot, int typeRelation) {
		return requete(mot, typeRelation, Filtre.AucunFiltre);
	}

	/**
	 * Requête à partir du terme et d'un filtre sur le type de relation.
	 * @param mot Terme à rechercher dans rezoJDM.
	 * @param nomTypeRelation nom du type de la relation dont il faut extraire le voisinage.
	 * @return Le résultat de la requête sur le terme demandé, avec un filtre sur le types des relations. 
	 */
	public Resultat requete(String mot, String nomTypeRelation) {
		Resultat res = null;
		Integer typeRelation = RequeterRezo.correspondancesRelations.get(nomTypeRelation);
		if (typeRelation != null) {
			res = requete(mot, typeRelation, Filtre.AucunFiltre);
		}
		else if(avertissement){
			System.err.println("Erreur RequeterRezo : "
					+ "le type de relation \""+nomTypeRelation+"\" n'a pas été reconnu dans la requête : "
					+ "\""+mot+","+nomTypeRelation+","+Filtre.AucunFiltre.toString()+"\"");
		}
		return res;
	}

	/**
	 * Requête complète à partir d'un terme, d'un nom de relation de rezoJDM et d'un filtre.
	 * @param mot Terme de rezoJDM.
	 * @param nomTypeRelation nom du type de la relation dont on cherche le voisinage pour "mot".
	 * @param filtre Filtre à appliquer.
	 * @return Le résultat de la requête.
	 */
	public Resultat requete(String mot, String nomTypeRelation, Filtre filtre) {		
		Resultat res = null;
		Integer typeRelation = RequeterRezo.correspondancesRelations.get(nomTypeRelation);
		if (typeRelation != null) {
			res = requete(mot, typeRelation, filtre);
		}else if(avertissement){
			System.err.println("Erreur RequeterRezo : "
					+ "le type de relation \""+nomTypeRelation+"\" n'a pas été reconnu dans la requête : "
					+ "\""+mot+","+nomTypeRelation+","+filtre.toString()+"\"");
		}
		return res;
	}

	/**
	 * Permet d'effectuer une requête filtrée sur plusieurs types.
	 * @param mot Terme sur lequel effectuer la requête.
	 * @param nomsTypesRelations Noms ou types des relations, séparés par un point-virgule (';').
	 * @return Une liste de résultat, chacun contenant le mot avec son voisinage pour le type associé.
	 */
	public ArrayList<Resultat> requeteMultiple(String mot, String nomsTypesRelations) {
		return requeteMultiple(mot, nomsTypesRelations, Filtre.AucunFiltre);
	}

	/**
	 * Permet d'effectuer une requête filtrée sur plusieurs types. 
	 * @param mot Terme sur lequel effectuer la requête.
	 * @param filtre Filtre sur la direction des relations. Le même filtre est appliqué à toutes les requêtes.
	 * @param nomsTypesRelations Noms ou types des relations, séparés par un point-virgule (';').
	 * @return Une liste de résultat, chacun contenant le mot avec son voisinage pour le type associé.
	 */
	public ArrayList<Resultat> requeteMultiple(String mot, String nomsTypesRelations, Filtre filtre) {
		String[] types = nomsTypesRelations.split(";");
		Resultat resultat;
		Integer typeRelation;
		ArrayList<Resultat> listeMots = new ArrayList<>();
		for (String type : types) {
			if (type.startsWith("r_")) {
				resultat = requete(mot, type, filtre);
				listeMots.add(resultat);
			} else {
				try {
					typeRelation = Integer.parseInt(type);
					resultat = requete(mot, typeRelation, filtre);
					listeMots.add(resultat);
				} catch (NumberFormatException numberFormatException) {
					if(avertissement) {
						System.err.println("Erreur requeteMultiple : impossible de lire le type \"" + type + "\". Ignoré.");
					}
				}
			}
		}
		return listeMots;
	}

	/**
	 * Permet de récupérer le poids d'une relation du RezoJDM
	 * @param motSource Terme JDM de départ de la relation
	 * @param nomTypeRelation Nom du type de relation devant lier les deux termes.
	 * @param motDestination Terme JDM d'arriver de la relation
	 * @return Le poids de la relation si elle existe, 0 sinon.
	 */
	public abstract int verifierExistenceRelation(String motSource, String nomTypeRelation, String motDestination);
	
	/**
	 * Centralisation des requêtes vers un point unique qui vérifie si la requête existe en cache, s'il faut faire entrer la requête dans le cache
	 * ou simplement retourner le résultat.
	 * @param cleCache Requête à effectuer.
	 * @return Le résultat de la requête. Soit en provenance du cache, soit en effectuant réellement la requête.
	 */
	protected Resultat requeteInterne(CleCache cleCache) {
		String avisCache = rencontrerMot(cleCache);
		boolean demande = false;
		Resultat resultat;
		switch (avisCache) {
		case "$DEMANDE$": {
			demande = true;
			break;
		}
		case "$OSEF$": {
			demande = false;
			break;
		}
		default: {                
			resultat = Resultat.lireCache(avisCache, cleCache);
			if(resultat == null) {
				cache.supprimer(cleCache);
				demande = true;
			}else {
				return resultat;
			}
		}
		}
		resultat = construireMot(cleCache);
		if (resultat.mot != null && demande) {
			int occ = 1;
			AttenteInfo info = attente.index.get(cleCache);
			if(info != null) {
				occ = info.occurrences;
			}
			cache.ajouter(cleCache, occ,resultat);
			attente.supprimer(cleCache);
			resultat.etatCache = EtatCache.NOUVELLE_ENTREE;
		}
		return resultat;
	}

	/**
	 * Analyse un requête pour déterminer si : 
	 * - Elle existe dans le cache
	 * - Il faut la faire entrer dans le cache
	 * - Il faut simplement la retourner.
	 * @param mot Requête à analyser.
	 * @return La chaîne "$DEMANDE$" si la requête doit être effectuée puis placer dans le cache, "$OSEF$" si elle ne doit pas l'être et le chemin
	 * vers le fichier contenant la requête si elle existe déjà en cache.
	 */
	protected String rencontrerMot(CleCache mot) {
		//Soit la requête existe dans le cache
		if (this.cache.contient(mot)) {
			return nouvelleOccurrence(mot);
		}
		//Si le cache ne contient pas le mot, on cherche dans les requêtes en attente  
		AttenteInfo attenteInfo = attente.index.get(mot);
		if(attenteInfo != null) {
			attenteInfo.incrementeOccurrences(peremption);
		}else {
			attente.ajouter(mot,1);
		}
		//Dans tous les cas, le mot est présent maintenant dans index
		//Regarde si on doit le faire entrer dans le cache
		if (demande(mot)) {
			return "$DEMANDE$";
		} else {
			return "$OSEF$";
		}
	}

	/**
	 * Traite le cas d'une requête existant dans le cache et détermine s'il faut : 
	 * - Utiliser le cache retourner le chemin vers le fichier dans le cache (le fichier n'est pas périmé)
	 * - Ne pas utiliser le cache et effectuer la requête puis : 
	 *     - Ajouter le résultat au cache
	 *     - Ne pas ajouter le résultat au cache
	 *     
	 * @param mot Requête à effectuer.
	 * @return La chaîne "$DEMANDE$" si la requête doit être effectuée puis placer dans le cache, "$OSEF$" si elle ne doit pas l'être et le chemin
	 * vers le fichier contenant la requête si elle existe déjà en cache.
	 */
	protected String nouvelleOccurrence(CleCache mot) {
		String res = null;
		CacheInfo info = cache.cache.get(mot);
		if (info != null) {                        
			if (!Utils.perime(info.dateCache, peremption)) {
				info.incrementeOccurrences();
				//On retourne la valeur du cache   
				res = Utils.construireChemin(info.ID); 
			} else {
				//L'entrée est périmée : on la supprime
				cache.supprimer(mot);
				if (demande(mot)) {			
					//Si le mot est intéressant (il y a de la place ou il est récurrent), 
					//on le demande pour le stocker en cache
					res = "$DEMANDE$";				
				} else {
					//Sinon on effectue une requête sur le serveur, sans stocker le résultat
					res = "$OSEF$";
				}
			}
		}
		return res;
	}

	/**
	 * Détermine s'il est intéressant de faire entrer la requête dans le cache.
	 * C'est le cas si : 
	 * - Le cache n'est pas plein
	 * - Il existe un élément périmé dans le cache
	 * - L'élément du cache le plus proche de la péremption est moins courant que le candidat
	 *  
	 * @param mot Une requête
	 * @return true si la requête doit être ajouter au cache, false sinon.
	 */
	protected boolean demande(CleCache mot) { 
		boolean res = !cache.estPlein();
		if(!res) {
			//Parcours du cache afin de trouver un voisin périmé
			//On en profite pour garder en mémoire le voisin le plus proche de la péremption
			//(au cas où il n'y ait aucun périmé)
			boolean existePerime = false;
			Iterator<Entry<CleCache, CacheInfo>> iter = this.cache.cache.entrySet().iterator();
			Entry<CleCache, CacheInfo> element;
			Entry<CleCache, CacheInfo> plusVieux;
			Date datePlusVieux;
			//initialisation
			if (iter.hasNext()) {
				element = iter.next();
				plusVieux = element;
				datePlusVieux = plusVieux.getValue().dateCache;				
				if (Utils.perime(datePlusVieux, peremption)) {
					existePerime = true;
				}
				while (!existePerime && iter.hasNext()) {
					element = iter.next();				
					if (Utils.perime(element.getValue().dateCache, peremption)) {
						existePerime = true;						
					} else if (element.getValue().dateCache.after(datePlusVieux)) {
						plusVieux = element;
						datePlusVieux = element.getValue().dateCache;
					}
				}
				if (existePerime) {
					cache.supprimer(element.getKey());
					res = true;
				} 
				//On compare avec les occurrences du plus vieux
				else {
					int occurrence = 1;
					AttenteInfo attenteInfo = this.attente.index.get(mot);
					if(attenteInfo != null) {
						occurrence += attenteInfo.occurrences;
					}
					if(occurrence > plusVieux.getValue().occurrences) {
						//mise en attente du plusVieux
						attente.ajouter(plusVieux.getKey(), plusVieux.getValue().occurrences);
						//suppression dans le cache du plus vieux
						cache.supprimer(plusVieux.getKey());
						//demande d'entrer dans le cache
						res = true;
					}
				}
			}
		}
		return res;
	}


	/**
	 * Indique si RequeterRezo est autorisé à indiquer des messages d'avertissement concernant l'exécution des requêtes.
	 * @return True si RequeterRezo est autorisé à écrire sur System.err, false sinon.
	 */
	public boolean getAvertissement() {
		return avertissement;
	}

	/**
	 * Autorise (ou interdit) RequeterRezo à indiquer des messages d'avertissement concernant l'exécution des requêtes.
	 * @param avertissement True si RequeterRezo est autorisé à écrire sur System.err, false sinon.
	 */
	public void setAvertissement(boolean avertissement) {
		this.avertissement = avertissement;
	}
}
