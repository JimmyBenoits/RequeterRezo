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
 * Attente d�fini un index des requ�tes d�j� effectu�es mais qui n'ont pas �t� jug�es assez importantes pour entrer dans le cache. 
 * Cela peut permettre de faire entrer dans le cache une requ�te demand�e de nombreuses fois, m�me si le cache est plein.
 * 
 * @see AttenteInfo
 * @author jimmy.benoits
 */
class Attente {

	/**
	 * Table d'association entre une requ�te {@link CleCache} et les informations sur sa fr�quences {@link AttenteInfo}.
	 */
	protected HashMap<CleCache, AttenteInfo> index;

	/**
	 * Constructeur par d�faut, initialise la table.
	 */
	protected Attente() {
		index = new HashMap<>();
	}

	public Attente(HashMap<CleCache, AttenteInfo> index) {	
		this.index = index;
	}


	/**
	 * Fonction appel�e lors du d�marrage d'une session RequeterRezo si un cache existe.
	 * Cela permet de garder en m�moire d'une session sur l'autre les requ�tes courantes. 
	 * @param chemin Chemin vers le fichier � charger.
	 * @return Une table remplie si le fichier a pu �tre charg� correctement. En cas d'EOFException (la processus d'�criture 
	 * a �t� interrompu), l'ancien fichier est supprim� et une nouvelle table est cr�e. Null sinon (avec affichage de l'erreur).
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
	 * Fonction appel�e � chaque modification de la table d'attente afin de conserver dans un fichier les informations sur les requ�tes courantes.
	 * @param chemin Chemin vers le fichier � sauvegarder.
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
	 * Supprime de l'attente une requ�te. Cela est notamment utile lors de l'entr�e d'une requ�te en attente dans le cache.
	 * @param cleCache Requ�te � supprimer.
	 */
	protected void supprimer(CleCache cleCache) {
		if (this.index.containsKey(cleCache)) {
			this.index.remove(cleCache);
		}
	}

	/**
	 * Ajoute une requ�te � l'attente. Le nombre d'occurence est � fixer � en cas de nouvelle entr�e mais le nombre d'occurrence d'une requ�te passant
	 * du cache � l'attente (pas assez utilis�e ou p�rim�e) est conserver. 
	 * @param cleCache Requ�te � ajouter.
	 * @param occurrences 1 en cas de nouvelle entr�e. Sinon le nombre d'occurrence de la requ�te. 
	 */
	protected void ajouter(CleCache cleCache, int occurrences) {
		AttenteInfo info = new AttenteInfo(occurrences, new Date());
		index.put(cleCache, info);
	}
}
