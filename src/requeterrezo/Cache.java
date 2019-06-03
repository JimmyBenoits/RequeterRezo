package requeterrezo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TreeSet;


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
 * Index permettant de garder en m�moire les requ�tes dont le r�sultat est stock� localement. 
 * 
 * Le cache (@{@link Cache#cache}) est une table d'association liant une requ�te ({@link CleCache})
 * a un ensemble d'informations ({@link CacheInfo}). Il permet de savoir si une requ�te a besoin d'�tre envoy�
 * sur le serveur ou si elle peut �tre charg�e directement. Il a une capacit� maximum (en octet) et 
 * une peremption (en nombre d'heure). Une nouvelle requ�te est syst�matiquement plac�e dans le cache s'il y a de la place.
 * Si le cache est plein (sans aucune entr�e p�rim�e), le nombre d'occurrence de la nouvelle requ�te est compar�e avec 
 * la requ�te la plus "petite" du cache afin de d�terminer si elle doit y la remplacer. 
 * 
 * Lors du chargement, une v�rification du cache est effectu�e ({@link Cache#verifierIntegrite(String, boolean)}). Cette op�ration v�rifie
 * que toutes les entr�e du cache poss�dent un fichier et que tous les fichiers du cache ont une entr�e. Cela permet de conserver un cache
 * m�me apr�s une ex�cution ayant rencontr� un probl�me.
 * 
 * Le cache n'autorise plus de nouvelles entr�es sans en supprimer une autre lorsque le cache est "plein". La taille maximale est donc une indication
 * Le cache peut d�passer cette limite : en ajoutant le "dernier" fichier (celui faisant franchir le seuil) et en supprimant un fichier plus petit lors
 * d'un �change.
 * 
 * @author jimmy.benoits
 */
class Cache {	

	/**
	 * Table d'association entre une requ�te et ses informations associ�es 
	 */
	protected HashMap<CleCache, CacheInfo> cache = new HashMap<>();

	/**
	 * Prochain ID a affecter si la file de recyclage est vide.
	 */
	protected int idMax;

	/**
	 * Lorsqu'une entr�e quitte le cache, son ID est conserv� afin d'�tre r�-affecter et de ne pas cro�tre ind�finiment.
	 */
	protected Queue<Integer> recyclesID;

	/**
	 * Nombre d'heures � partir duquel un fichier du cache est consid�r� comme trop vieux pour �tre utilisable.
	 */
	protected final int peremption;

	/**
	 * Taille actuelle (en octet) du cache.
	 */
	protected long tailleCourante;

	/**
	 * Taille maximale (en octet) autoris�e pour le cache. 
	 */
	protected long tailleMax;



	/**
	 * Constructeur param�tr�.
	 * @param peremption Nombre d'heure avant lequelle un fichier du cache est consid�r� comme obsol�te.
	 * @param tailleMax Taille maximale du cache (en octet).
	 */
	protected Cache(int peremption, long tailleMax) {
		this.idMax = 0;
		this.tailleCourante = 0L;
		this.tailleMax = tailleMax;
		this.peremption = peremption;
		this.recyclesID = new LinkedList<>();
	}

	private Cache(int idMax, Queue<Integer> recyclesID, int peremption, long tailleCourante, long tailleMax,
			HashMap<CleCache, CacheInfo> cache) {
		this.idMax = idMax;
		this.tailleCourante = tailleCourante;
		this.tailleMax = tailleMax;
		this.peremption = peremption;
		this.recyclesID = recyclesID;
		this.cache = cache;
	}

	/**
	 * Retourne l'ID du prochain objet � ajouter au cache.
	 * @return idMax ou le premier id de la file de recyclage.
	 */
	protected int prochainID() {
		int res;
		if(!recyclesID.isEmpty()) {
			res = recyclesID.poll();
		}else {
			res = idMax;
			++idMax;
		}
		return res;
	}

	/**
	 * Ajoute une nouvelle requ�te au cache.
	 * @param cleCache Requ�te � ajouter.
	 * @param occurrences Nombre d'occurrence de la requ�te (1 si nouvelle, son nombre dans la table {@link Attente} sinon.
	 * @param resultat R�sultat de la requ�te.
	 */
	protected void ajouter(CleCache cleCache, int occurrences, Resultat resultat) {
		int id = prochainID();
		Date nouvelleDate = new Date();
		String chemin = Utils.construireChemin(id);
		Resultat.ecrireCache(chemin, resultat);
		File fichier =  new File(chemin);		
		long tailleFichier = fichier.length();
		CacheInfo cacheInfo = new CacheInfo(id, nouvelleDate, occurrences, nouvelleDate, tailleFichier, resultat.getEtat());
		cache.put(cleCache, cacheInfo);
		tailleCourante += tailleFichier;
	}

	/**
	 * Supprime une requ�te du cache. Met � jour la taille, ajoute l'ID de la requ�te supprim�e � la file de recyclage. Supprime le fichier dans le cache.
	 * @param cleCache Requ�te � supprimer.
	 */
	protected void supprimer(CleCache cleCache) {
		CacheInfo info = cache.get(cleCache);
		if(info != null) {
			String chemin = Utils.construireChemin(info.ID);
			File aSupprimer = new File(chemin);
			if(aSupprimer.exists()) {
				aSupprimer.delete();
			}
			tailleCourante -= info.tailleFichier;
			recyclesID.add(info.ID);	
			cache.remove(cleCache);
		}
	}	

	/**
	 * D�termine si une entr�e dans le cache est p�rim�e ou non.
	 * @param cacheKey Requ�te � tester.
	 * @return True si le nombre d'heure s�parant la demande et l'entr�e de la requ�te dans le cache est sup�rieur au d�lais de p�remption, false sinon.
	 */
	protected boolean estPerime(CleCache cacheKey) {
		boolean res = true;
		CacheInfo cacheInfo = cache.get(cacheKey);
		if (cacheInfo != null) {
			res = Utils.perime(cacheInfo.dateCache, peremption);
		}
		return res;
	}

	/**
	 * Charge un objet Cache depuis un fichier s�rialis�. 
	 * @param chemin Chemin vers un fichier cr�� par la fonction {@link Cache#sauvegarderCache(String)}.
	 * @return Un cache pr�cedemment rempli. Si le fichier est illisible (l'�criture a �t� interrompu avant sa fin), retourne null.
	 * En cas d'autre erreur, retourne null apr�s avoir affich� la trace d'erreur.
	 */
	protected static Cache chargerCache(String chemin, long nouvelleTailleMax, boolean avertissement) {
		//		long timer = System.nanoTime();		
		Cache resultat = null;
		String line;
		String[] tokens;
		HashMap<CleCache, CacheInfo> cache = new HashMap<>();
		int idMax;
		Queue<Integer> recyclesID;
		int peremption;
		long tailleCourante;	
		long tailleMax;
		CleCache cle;
		CacheInfo info;
		try(BufferedReader reader = new BufferedReader(new FileReader(chemin))){

			idMax = Integer.parseInt(reader.readLine());
			recyclesID = new LinkedList<>();			
			line = reader.readLine();
			if(!line.isEmpty()) {
				tokens = line.split(",");
				for(String token : tokens) {
					recyclesID.add(Integer.parseInt(token));
				}
			}

			peremption = Integer.parseInt(reader.readLine());
			tailleCourante = Long.parseLong(reader.readLine());
			tailleMax = Long.parseLong(reader.readLine());
			if(tailleMax > nouvelleTailleMax && tailleCourante > nouvelleTailleMax && avertissement) {
				System.err.println("Avertissement RequeterRezo : la nouvelle taille du cache est plus petite que l'ancienne.");
			}

			while((line = reader.readLine())!= null) {
				tokens = line.split(";;;");
				cle = CleCache.Construire(tokens[0]);
				info = CacheInfo.Construire(tokens[1]);
				cache.put(cle, info);
			}
			resultat = new Cache(idMax, recyclesID, peremption, tailleCourante, nouvelleTailleMax, cache);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//		timer = System.nanoTime() - timer;
		//		System.out.println("Cache loaded in: "+ (timer / 1_000_000)+"ms");
		return resultat;
	}

	//	protected static Cache chargerCache(String chemin) {
	//		FileInputStream fichierFluxEntrant;
	//		ObjectInputStream objetFluxEntrant;
	//		Cache cache = null;
	//
	//		try {			
	//			fichierFluxEntrant = new FileInputStream(chemin);
	//			objetFluxEntrant = new ObjectInputStream(fichierFluxEntrant);
	//			cache = (Cache) objetFluxEntrant.readObject();			
	//			objetFluxEntrant.close();	
	//			fichierFluxEntrant.close();
	//		}catch(EOFException e) {
	//			return null;		
	//		}catch(Exception e) {
	//			e.printStackTrace();
	//		}
	//		return cache;
	//	}

	/**
	 * Sauvegarde le cache dans un objet s�rialis� pouvant �tre lu par {@link Cache#chargerCache(String)}.
	 * @param chemin Chemin du fichier o� sauvegarder le cache.
	 */
	protected void sauvegarderCache(String chemin) {
		//		long timer = System.nanoTime();		
		String line;
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(chemin))){
			writer.write(String.valueOf(idMax));
			writer.newLine();
			Iterator<Integer> iterateurID;
			iterateurID = recyclesID.iterator();
			line = "";
			if(iterateurID.hasNext()) {
				line = String.valueOf(iterateurID.next());
				while(iterateurID.hasNext()) {
					line += ","+String.valueOf(iterateurID.next());
				}
			}
			writer.write(line);
			writer.newLine();
			writer.write(String.valueOf(peremption));
			writer.newLine();
			writer.write(String.valueOf(tailleCourante));
			writer.newLine();
			writer.write(String.valueOf(tailleMax));
			writer.newLine();

			for(Entry<CleCache, CacheInfo> entree : cache.entrySet()) {			
				writer.write(entree.getKey().toString()+";;;"+entree.getValue().toString());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		//		timer = System.nanoTime() - timer;
		//		System.out.println("Cache saved in: "+ (timer / 1_000_000)+"ms");
	}

	//	protected void sauvegarderCache(String chemin) {
	//		FileOutputStream fichierFluxSortant;
	//		ObjectOutputStream objetFluxSortant;
	//
	//		try {
	//			fichierFluxSortant = new FileOutputStream(chemin);
	//			objetFluxSortant = new ObjectOutputStream(fichierFluxSortant);
	//			objetFluxSortant.writeObject(this);
	//			objetFluxSortant.close();
	//			fichierFluxSortant.close();
	//		}catch(IOException e) {
	//			e.printStackTrace();
	//		}
	//
	//	}

	/**
	 * V�rifie si une requ�te existe dans le cache.
	 * @param mot Requ�te � v�rifier.
	 * @return True si la requ�te existe dans le cache, false sinon.
	 */
	protected boolean contient(CleCache mot) {
		return this.cache.containsKey(mot);
	}


	/**
	 * V�rifie s'il est possible d'ajouter une nouvelle entr�e. 
	 * @return True s'il le cache est plein, false sinon.
	 */
	protected boolean estPlein(){
		return tailleCourante >= tailleMax;
	}

	/**
	 * M�thode � appeler apr�s avoir charger un cache afin de v�rifier son bon fonctionnement. 
	 * Cette m�thode v�rifie que : 
	 * - Toutes les entr�es du cache ont un fichier associ� (sinon, supprime l'entr�e). 
	 * Attention, si le fichier est illisible, cela ne sera pas d�tecter ici.
	 * - Tous les fichiers du cache ont une entr�e. Si un fichier est pr�sent dans le dossier contenant le cache mais pas dans l'index,
	 * le fichier est charg� et ensuite ajouter au cache. 
	 * @param cheminCache chemin vers le dossier contenant le cache.
	 * @param avertissement True si l'utilisateur veut des d�tails sur l'ex�cution, false sinon.
	 */
	protected void verifierIntegrite(String cheminCache, boolean avertissement) {		
		//1er sens : v�rifier que tous les mots du cache poss�dent leur fichier 
		ArrayList<CleCache> aRetirer = new ArrayList<>();
		String chemin;
		File fichierCache;
		for(Entry<CleCache,CacheInfo> entree : cache.entrySet()) {
			chemin = Utils.construireChemin(entree.getValue().ID);
			fichierCache = new File(chemin);
			if(!fichierCache.exists()) {
				aRetirer.add(entree.getKey());
			}
		}

		for(CleCache cleCache : aRetirer) {
			supprimer(cleCache);
		}

		//2nd sens : v�rifier que les fichiers pr�sents dans le dossier sont r�pertorier, sinon, les ajouter		
		File dossierCache = new File(cheminCache);
		//Ne le faire SEULEMENT SI le nombre de fichier en ".cache" dans le cache est plus grand que le nombre d'entr�e		
		ArrayList<File> fichiersCache;
		//R�cup�ration des IDs pr�sents dans le cache (normalement {0,1,...,idMax-1} moins ceux pr�sents dans la file de recyclage		
		TreeSet<Integer> idsCache = new TreeSet<>();
		for(CacheInfo info : cache.values()) {
			idsCache.add(info.ID);
		}		
		//R�cup�ration r�cursive des fichiers dans le dossier du cache.
		fichiersCache = Utils.fichiersCache(dossierCache);
		//nombre de fichier.
		int cpt = fichiersCache.size();
		//nombre d'entr�es � ajouter (0 si aucune entr�e ne manque).
		int aAjouter = cpt - cache.size();
		if(aAjouter > 0) {
			if(avertissement) {
				if(aAjouter == 1) {
					System.err.println("Avertissement RequeterRezo : Probl�me d'int�grit� du cache, "
							+ "recherche et chargement du fichier manquant (cette op�ration peut prendre quelques minutes).");
				}else {
					System.err.println("Avertissement RequeterRezo : Probl�me d'int�grit� du cache, "
							+ "recherche et chargement des "+aAjouter+" fichiers manquants (cette op�ration peut prendre quelques minutes).");	
				}
			}
			ArrayList<File> aSupprimer = new ArrayList<>();
			Resultat resultat;
			Mot mot;
			CleCache cleCache;
			int i = 0;
			File fichier;
			Integer id;
			//Arr�t des recherches si on trouve toutes les entr�es manquantes.	
			while(i < fichiersCache.size() && (aAjouter > 0)) {
				fichier = fichiersCache.get(i);
				id = Utils.recupererID(fichier.getPath());								
				if(id != null && !idsCache.contains(id)) {
					//lecture du fichier seulement en dernier recours (op�ration co�teuse, ~400ms).
					resultat = Resultat.lireCache(fichier.getPath(), null);
					if(resultat == null) {
						//EOFException => fichier illisible, supprimer le fichier.
						if(avertissement) {
							System.err.println("Avertissement RequeterRezo : le fichier \""+fichier.getPath()+"\" est illisible.");
						}
						aSupprimer.add(fichier);
						--aAjouter;
					}else {
						mot = resultat.getMot();
						if(mot != null) {
							cleCache = resultat.getMot().cleCache;							
							if(!cache.containsKey(cleCache)) {
								if(avertissement) {
									System.err.println("Avertissement RequeterRezo : Ajout de \""+cleCache.toString()+"\" gr�ce au fichier.");
								}
								ajouter(cleCache, 1, resultat);
								--aAjouter;
							}
						}
					}					
				}
				++i;
			}

			//Suppression des fichiers illisibles d�tect�s.
			for(File supprimer : aSupprimer) {
				supprimer.delete();
			}
		}
	}

}
