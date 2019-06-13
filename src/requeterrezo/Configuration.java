package requeterrezo;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


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
 * Elements de configuration partagée par le système "live" ({@link RequeterRezoDump}) et le système "local" ({@link RequeterRezoSQL}).
 * @author jimmy.benoits
 */
public class Configuration {

	/**
	 * Nombre d'heure à partir duquel un fichier dans le cache est considéré comme périmé.
	 */
	protected int peremption;

	/**
	 * Taille maximale du cache (en Octet).
	 */
	protected long tailleCache;

	/**
	 * Chemin vers dossier contenant le cache.
	 */
	protected String cheminCache;

	/**
	 * Le mode avancé laisse à l'utilisateur le soin de sauvegarder les index de cache et de mise en attente de lui-même, plutôt que de le faire 
	 * après chaque mise à jour. Ce mode est désactivé par défaut et il n'est pas conseillé de l'activer.
	 */
	protected boolean modeAvance;

	/**
	 * Indique (ou non) des messages sur System.err à des fins de debug.
	 */
	protected boolean avertissement;
	
	/**
	 * Utilisation du cache. Obligatoire pour RezoDump, conseillé pour RezoSQL. Modifiable uniquement par le fichier de configuration.
	 */
	protected boolean utiliserCache;


	/**
	 * Constructeur par défaut. Initialise le délais de péremption à 168h (1 semaine), le cache à 100mo, le chemin du cache à "cache",
	 * n'active pas le mode avance mais active les messages.
	 */
	public Configuration() {
		this.peremption = 168;
		this.tailleCache = 100_000_000;
		this.cheminCache = "cache";
		this.modeAvance = false;
		this.avertissement = true;
		this.utiliserCache = true;
	}

	/**
	 * Créé une configuration à partir d'un fichier ".ini". Le fichier doit être rempli sous le format "CLE=VALEUR". Les clés sont : 
	 * - PEREMPTION : la valeur doit être un nombre suivi d'une unité ('j' pour jour et 'h' pour heure). Ex : 24h
	 * - CHEMIN_CACHE : la valeur correspond au chemin où le cache sera stocké.
	 * - TAILLE_MAX_CACHE : la valeur doit être un nombre suivi d'une unité ('ko', 'mo' ou 'go'). Ex : 200mo
	 * - AVERTISSEMENT : OUI si l'on souhaite obtenir des messages en provenance de RequeterRezo, NON sinon.
	 * - MODE : AVANCE si l'on souhaite activer le mode avancé (déconseillé). 
	 * Le caractère dièse (#) est utilisé pour commenter une ligne. 
	 * @param chemin_fichier_configuration Chemin vers le fichier ".ini".
	 * @throws FileNotFoundException Le fichier n'a pas pu être trouvé.
	 * @throws IOException Le fichier n'a pas pu être lu.
	 * @throws ConfigurationException Une erreur est survenue lors de l'interprétation du fichier de configuration.
	 */
	public Configuration(String chemin_fichier_configuration) throws FileNotFoundException, IOException, ConfigurationException {
		try (BufferedReader reader = new BufferedReader(new FileReader(chemin_fichier_configuration))) {
			String ligne;
			int indexOf;
			String cle, valeur;
			String suffixe;
			while ((ligne = reader.readLine()) != null) {
				if (!ligne.isEmpty() && ligne.charAt(0) != '#') {
					if ((indexOf = ligne.indexOf('=')) != -1) {
						cle = ligne.substring(0, indexOf);
						valeur = ligne.substring(indexOf + 1);
						switch (cle.toUpperCase()) {
						case "MODE": {
							this.modeAvance = valeur.toUpperCase().equals("AVANCE");
							break;
						}
						case "AVERTISSEMENT" : {
							this.avertissement = valeur.toUpperCase().equals("OUI") || valeur.toUpperCase().equals("TRUE");
							break;
						}
						case "PEREMPTION": {
							switch (valeur.charAt(valeur.length() - 1)) {
							case 'j': {								
								try {
									this.peremption = Integer.parseInt(valeur.substring(0, valeur.length()-1)) * 24;
								} catch (NumberFormatException e) {
									throw new ConfigurationException("Erreur Configuration RequeterRezo : la valeur de peremption n'est pas correcte. Veuillez utiliser un entier suivi de 'h' pour un nombre d'heures ou 'j' pour un nombre de jours.");
								}
								break;
							}
							case 'h': {
								try {
									this.peremption = Integer.parseInt(valeur.substring(0, valeur.length()-1)) * 24;
								} catch (NumberFormatException e) {
									throw new ConfigurationException("Erreur Configuration RequeterRezo : la valeur de peremption n'est pas correcte. Veuillez utiliser un entier suivi de 'h' pour un nombre d'heures ou 'j' pour un nombre de jours.");
								}
								break;
							}
							default: {
								throw new ConfigurationException("Erreur Configuration RequeterRezo : l'unitÃ© de la pÃ©remption n'est pas reconnue. Veuillez utiliser un entier suivi de 'h' pour un nombre d'heures ou 'j' pour un nombre de jours.");
							}
							}
							break;
						}
						case "CHEMIN_CACHE": {
							this.cheminCache = valeur;
							break;
						}
						case "TAILLE_MAX_CACHE": {
							if (valeur.length() > 2) {
								suffixe = valeur.substring(valeur.length() - 2);
								try {
									switch (suffixe.toLowerCase()) {
									case "ko": {
										this.tailleCache = Long.parseLong(valeur.substring(0, valeur.length() - 2)) * 1_000;
										break;
									}
									case "mo": {
										this.tailleCache = Long.parseLong(valeur.substring(0, valeur.length() - 2)) * 1_000_000;
										break;
									}
									case "go": {
										this.tailleCache = Long.parseLong(valeur.substring(0, valeur.length() - 2)) * 1_000_000_000;
										break;
									}
									default: {
										throw new ConfigurationException("Erreur Configuration RequeterRezo : l'unitÃ© de la taille maximale du cache n'est pas reconnue. Veuillez utiliser un entier suivi de \"ko\", \"mo\" ou \"go\".");
									}
									}
								} catch (NumberFormatException e) {
									throw new ConfigurationException("Erreur Configuration RequeterRezo : la valeur de la taille maximale du cache n'est pas correcte. Veuillez utiliser un entier suivi de 'h' pour un nombre d'heures ou 'j' pour un nombre de jours.");
								}
							} else {
								throw new ConfigurationException("Erreur Configuration RequeterRezo : l'unité de la taille maximale du cache n'est pas reconnue. Veuillez utiliser un entier suivi de \"ko\", \"mo\" ou \"go\".");
							}

							break;
						}
						case "UTILISER_CACHE": {
							this.utiliserCache = valeur.toUpperCase().equals("OUI") || valeur.toUpperCase().equals("TRUE");;
							break;
						}
						default: {
							break;
						}
						}
					} else {
						throw new ConfigurationException("Erreur Configuration RequeterRezo : la ligne \"" + ligne + "\" n'a pas pu être interprÃ©tÃ©e.");
					}
				}
			}
		}
	}

	/**
	 * Nombre d'heure à partir duquel un fichier dans le cache est considéré comme périmé.
	 * @return Le délais de peremption.
	 */
	public int getPeremption() {
		return peremption;
	}

	/**
	 * Taille maximale du cache (en Octet).
	 * Le cache n'autorise plus de nouvelles entrées sans en supprimer une autre lorsque le cache est "plein". La taille maximale est donc une indication
	 * Le cache peut dépasser cette limite : en ajoutant le "dernier" fichier (celui faisant franchir le seuil) et en supprimant un fichier plus petit lors
	 * d'un échange.
	 * @return La taille maximale du cache.
	 */
	public long getTailleCache() {
		return tailleCache;
	}

	/**
	 * Chemin vers dossier contenant le cache.
	 * @return Le chemin vers le dossier contenant le cache.
	 */
	public String getCheminCache() {
		return cheminCache;
	}

	/**
	 * Le mode avancé laisse à l'utilisateur le soin de sauvegarder les index de cache et de mise en attente de lui-même, plutôt que de le faire
	 * après chaque mise à jour. Ce mode est désactivé par défaut et il n'est pas conseillé de l'activer.
	 * @return True si le mode Avancé a été activé, false sinon.
	 */
	public boolean getModeAvance(){
		return modeAvance;
	}
	
	/**
	 * Indique (ou non) des messages sur System.err à des fins de debug.
	 * @return True si RequeterRezo doit transmettre des messages, false sinon.
	 */
	public boolean getAvertissement() {
		return avertissement;
	}
	
	/**
	 * Utilisation du cache. Obligatoire pour RezoDump, conseillé pour RezoSQL. Modifiable uniquement par le fichier de configuration.
	 * @return True si le cache doit être utiliser, false sinon.
	 */
	public boolean getUtiliserCache() {
		return utiliserCache;
	}
}
