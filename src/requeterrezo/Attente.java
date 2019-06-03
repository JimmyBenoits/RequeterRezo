package requeterrezo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
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
 * Attente défini un index des requêtes déjà effectuées mais qui n'ont pas été jugées assez importantes pour entrer dans le cache. 
 * Cela peut permettre de faire entrer dans le cache une requête demandée de nombreuses fois, même si le cache est plein.
 * 
 * @see AttenteInfo
 * @author jimmy.benoits
 */
class Attente {

	/**
	 * Table d'association entre une requête {@link CleCache} et les informations sur sa fréquences {@link AttenteInfo}.
	 */
	protected HashMap<CleCache, AttenteInfo> index;

	/**
	 * Constructeur par défaut, initialise la table.
	 */
	protected Attente() {
		index = new HashMap<>();
	}

	public Attente(HashMap<CleCache, AttenteInfo> index) {	
		this.index = index;
	}


	/**
	 * Fonction appelée lors du démarrage d'une session RequeterRezo si un cache existe.
	 * Cela permet de garder en mémoire d'une session sur l'autre les requêtes courantes. 
	 * @param chemin Chemin vers le fichier à charger.
	 * @return Une table remplie si le fichier a pu être chargé correctement. En cas d'EOFException (la processus d'écriture 
	 * a été interrompu), l'ancien fichier est supprimé et une nouvelle table est crée. Null sinon (avec affichage de l'erreur).
	 */
	protected static Attente chargerAttente(String chemin) {
//		long timer = System.nanoTime();		
		Attente resultat = null;
		String line;
		String[] tokens;
		HashMap<CleCache, AttenteInfo> index = new HashMap<>();
		CleCache cle;
		AttenteInfo info;
		try(BufferedReader reader = new BufferedReader(new FileReader(chemin))){
			while((line = reader.readLine())!= null) {
				tokens = line.split(";;;");
				cle = CleCache.Construire(tokens[0]);
				info = AttenteInfo.Construire(tokens[1]);
				index.put(cle, info);
			}
			resultat = new Attente(index);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		timer = System.nanoTime() - timer;
//		System.out.println("Cache loaded in: "+ (timer / 1_000_000)+"ms");
		return resultat;
//		FileInputStream fichierFluxEntrant;
//		ObjectInputStream objetFluxEntrant;
//		Attente attente = null;
//		try {
//			fichierFluxEntrant = new FileInputStream(chemin);
//			objetFluxEntrant = new ObjectInputStream(fichierFluxEntrant);
//			attente = (Attente) objetFluxEntrant.readObject();			
//			objetFluxEntrant.close();	
//			fichierFluxEntrant.close();
//		}catch(EOFException e) {
//			File aSupprimer = new File(chemin);
//			aSupprimer.delete();
//			return new Attente();
//		}catch(Exception e) {
//			e.printStackTrace();
//		}	
	}

	/**
	 * Fonction appelée à chaque modification de la table d'attente afin de conserver dans un fichier les informations sur les requêtes courantes.
	 * @param chemin Chemin vers le fichier à sauvegarder.
	 */
	protected void sauvegarderAttente(String chemin) {
//		long timer = System.nanoTime();
		try(BufferedWriter writer = new BufferedWriter(new FileWriter(chemin))){			
			for(Entry<CleCache, AttenteInfo> entree : index.entrySet()) {				
				writer.write(entree.getKey().toString()+";;;"+entree.getValue().toString());
				writer.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		
//		FileOutputStream fichierFluxSortant;
//		ObjectOutputStream objetFluxSortant;
//		try {
//			fichierFluxSortant = new FileOutputStream(chemin);
//			objetFluxSortant = new ObjectOutputStream(fichierFluxSortant);
//			objetFluxSortant.writeObject(this);
//			objetFluxSortant.close();
//			fichierFluxSortant.close();
//		}catch(IOException e) {
//			e.printStackTrace();
//		}
//		timer = System.nanoTime() - timer;
//		System.out.println("Attente saved in: "+ (timer / 1_000_000)+"ms");
	}

	/**
	 * Supprime de l'attente une requête. Cela est notamment utile lors de l'entrée d'une requête en attente dans le cache.
	 * @param cleCache Requête à supprimer.
	 */
	protected void supprimer(CleCache cleCache) {
		if (this.index.containsKey(cleCache)) {
			this.index.remove(cleCache);
		}
	}

	/**
	 * Ajoute une requête à l'attente. Le nombre d'occurence est à fixer à en cas de nouvelle entrée mais le nombre d'occurrence d'une requête passant
	 * du cache à l'attente (pas assez utilisée ou périmée) est conserver. 
	 * @param cleCache Requête à ajouter.
	 * @param occurrences 1 en cas de nouvelle entrée. Sinon le nombre d'occurrence de la requête. 
	 */
	protected void ajouter(CleCache cleCache, int occurrences) {
		AttenteInfo info = new AttenteInfo(occurrences, new Date());
		index.put(cleCache, info);
	}
}
