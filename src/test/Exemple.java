package test;

import java.util.ArrayList;

import requeterrezo.Etat;
import requeterrezo.EtatCache;
import requeterrezo.Filtre;
import requeterrezo.Mot;
import requeterrezo.RequeterRezo;
import requeterrezo.RequeterRezoDump;
import requeterrezo.Resultat;
import requeterrezo.Relation;

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
 * RequeterRezo est une API Java permettant de manipuler les données du réseau lexico-sémantique rezoJDM.
 * Pour en savoir plus, rendez-vous à l'adresse suivante : http://www.jeuxdemots.org/jdm-accueil.php
 * 
 * Cette classe vous permettra de découvrir les fonctionnalités de bases de l'API. 
 * Pour plus d'information (notamment sur RequeterRezoSQL), référez-vous à la documentation.
 * 
 * N'hésitez pas à adresser vos questions & remarques à l'administrateur de JeuxDeMots (http://www.jeuxdemots.org/jdm-about.php) ou au créateur de l'API (benoitsjimmy@gmail.com).
 * 
 * Attention : Pour des raisons de comptatibilité avec les données de rezoJDM, le projet est encodé en LATIN-1. Merci de prendre les dispositions nécessaires.
 *  
 * 
 * @author jimmy.benoits
 */
class Exemple {

	public static void main(String[] args) {
		//Les requêtes sont effectuées via un objet "RequeterRezo".
		//Deux implémentations sont proposées : RequeterRezoDump et RequeterRezoSQL
		//La première effectue des requêtes sur le serveur de JeuxDeMots en passant par le service rezo-dump (http://www.jeuxdemots.org/rezo-dump.php)
		//La seconde effectue des requêtes sur un serveur MySQL hébergeant une base de données contenant rezoJDM (données disponible à l'adresse : http://www.jeuxdemots.org/JDM-LEXICALNET-FR/?C=M;O=D).
		RequeterRezo rezo;
		
		//RequeterRezoDump permet de commencer directement et de tester rapidement si les données de rezoJDM correspondent à votre besoin.
		//Plusieurs constructeurs sont proposés. Il est aussi possible d'utiliser un fichier de configuration (voir le fichier RequeterRezo.ini donné en exemple).
		//Pour plus d'informations sur les paramètres possibles et leurs valeurs par défaut, se référer à la documentation.
		rezo = new RequeterRezoDump();
		
		//RequeterRezo utilise un système de cache afin d'éviter les requêtes redondantes. Si une erreur est survenue dans une exécution précédente, des opérations de vérifications seront peut-être effecutées.
		//En cas de doute, il est toujours possible de supprimer manuellement le dossier de cache
		
		//Pour effectuer une requête sur RequeterRezo, il suffit d'appeler la méthode "requete".
		//Cette méthode retourne un objet de type "Resultat" qui contient le "Mot" demandé ainsi que deux autres informations : Etat et EtatCache 
		Resultat resultatRequete = rezo.requete("toto");
		
		//L'objet qui vous intéresse est certainement le "Mot". Il contient les informations présentes dans rezoJDM.
		Mot mot = resultatRequete.getMot();
		//Un Mot est notamment composé de relations sortantes (pour lesquelles il en est la source) et de relations entrantes (pour lesquelles il en est la destination).
		//Un Voisin est un noeud rezoJDM ainsi que le poids de la relation associée.
		
		ArrayList<Relation> voisins = mot.getRelationsSortantesTypees("r_lieu");
		System.out.println("Un toto peut se trouver dans les lieux suivants : ");		
		for(Relation voisin : voisins) {
			System.out.println("\t"+voisin);
		}
		
		//Attention néanmoins, une requête retourne toujours un objet Resultat mais pas toujours un Mot ! 
		//Le serveur peut-être indisponible, le mot inexistant, ou bien encore trop gros pour être transmis dans les temps. 
		//Pour éviter les NullPointerException, pensez à vérifier l'objet Mot !
		mot = resultatRequete.getMot();
		if(mot != null) {
			//traitement
		}
		//Le champ "Etat" de votre objet "Resultat" permet de donner des informations précieuses sur le resultat de votre requête :
		Etat etat = resultatRequete.getEtat();
		System.out.println("L'état de la requête est : "+etat);
		//Les valeurs possibles sont : 
		// - INEXISTANT : le terme demandé n'existe pas dans rezoJDM
		// - TROP_GROS : le Mot résultat possède plus de 25 000 relations entrantes ou sortantes. 
		//Pour des raisons de performances, le nombre de relations par mot est limité sur RequeterRezoDump. 
		//Pour palier à ces limitations, utilisez RequeterRezoSQL ou utiliser les requêtes filtrées.
		// - RENVOYER : dans le cas d'une requête très importante (ex. "personne"), il est parfois nécessaire de relancer la requête pour avoir un résultat.
		//Le résultat sera alors certainement "TROP_GROS". 
		// - SERVEUR_INACCESSIBLE : Le service rezo-dump n'a pas pu être contacté.
		// - OK : La requête s'est déroulée correctement.
		// - ERREUR_REQUETE : tout autre erreur lors de la requête.
		
		EtatCache etatCache = resultatRequete.getEtatCache();
		System.out.println("L'état du cache de la requête est : "+etatCache);
		//De plus un champ "EtatCache" permet d'obtenir des informations sur le cache :
		// - DEPUIS_CACHE : le résultat a été produit grâce au cache
		// - NOUVELLE_ENTREE : le résultat a été placé dans le cache
		// - EN_ATTENTE : le résultat n'a pas été placé dans le cache mais son nombre d'occurrence est compté afin de faire entrer dans le cache
		//les termes souvent demandés.
		// - ERREUR_REQUETE : une erreur est survenue lors de la requête
		
		
		//Pour éviter les états "TROP_GROS" ou "RENVOYER", il est conseillé de filter vos requêtes du mieux possible. 
		//Ainsi, plutôt que de demander le terme "pomme de terre" et de chercher son voisinage pour la relation "lieu" comme précédemment avec "toto"
		//il est possible de demander directement les relations de types "r_lieu" et de filtrer les relations entrantes.
		resultatRequete = rezo.requete("pomme de terre", "r_lieu", Filtre.RejeterRelationsEntrantes);
		mot = resultatRequete.getMot();
		if(mot != null) {
			voisins = mot.getRelationsSortantesTypees("r_lieu");
			System.out.println("Une pomme de terre peut se trouver dans les lieux suivants : ");		
			for(Relation voisin : voisins) {
				System.out.println("\t"+voisin);
			}
		}
		//filtrer une relation permet d'obtenir des résultats bien plus rapidement et d'être sûr que le type que vous recherchez n'a pas été tronqué 
		//avec un état "TROP_GROS"

		
		//Pour plus de détails, pensez à la javadoc.
		//Bonne chance !		
	}
	
	
}
