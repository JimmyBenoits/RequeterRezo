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
 * RequeterRezo est une API Java permettant de manipuler les donn�es du r�seau lexico-s�mantique rezoJDM.
 * Pour en savoir plus, rendez-vous � l'adresse suivante : http://www.jeuxdemots.org/jdm-accueil.php
 * 
 * Cette classe vous permettra de d�couvrir les fonctionnalit�s de bases de l'API. 
 * Pour plus d'information (notamment sur RequeterRezoSQL), r�f�rez-vous � la documentation.
 * 
 * N'h�sitez pas � adresser vos questions & remarques � l'administrateur de JeuxDeMots (http://www.jeuxdemots.org/jdm-about.php) ou au cr�ateur de l'API (benoitsjimmy@gmail.com).
 * 
 * Attention : Pour des raisons de comptatibilit� avec les donn�es de rezoJDM, le projet est encod� en LATIN-1. Merci de prendre les dispositions n�cessaires.
 *  
 * 
 * @author jimmy.benoits
 */
class Exemple {

	public static void main(String[] args) {
		//Les requ�tes sont effectu�es via un objet "RequeterRezo".
		//Deux impl�mentations sont propos�es : RequeterRezoDump et RequeterRezoSQL
		//La premi�re effectue des requ�tes sur le serveur de JeuxDeMots en passant par le service rezo-dump (http://www.jeuxdemots.org/rezo-dump.php)
		//La seconde effectue des requ�tes sur un serveur MySQL h�bergeant une base de donn�es contenant rezoJDM (donn�es disponible � l'adresse : http://www.jeuxdemots.org/JDM-LEXICALNET-FR/?C=M;O=D).
		RequeterRezo rezo;
		
		//RequeterRezoDump permet de commencer directement et de tester rapidement si les donn�es de rezoJDM correspondent � votre besoin.
		//Plusieurs constructeurs sont propos�s. Il est aussi possible d'utiliser un fichier de configuration (voir le fichier RequeterRezo.ini donn� en exemple).
		//Pour plus d'informations sur les param�tres possibles et leurs valeurs par d�faut, se r�f�rer � la documentation.
		rezo = new RequeterRezoDump();
		
		//RequeterRezo utilise un syst�me de cache afin d'�viter les requ�tes redondantes. Si une erreur est survenue dans une ex�cution pr�c�dente, des op�rations de v�rifications seront peut-�tre effecut�es.
		//En cas de doute, il est toujours possible de supprimer manuellement le dossier de cache
		
		//Pour effectuer une requ�te sur RequeterRezo, il suffit d'appeler la m�thode "requete".
		//Cette m�thode retourne un objet de type "Resultat" qui contient le "Mot" demand� ainsi que deux autres informations : Etat et EtatCache 
		Resultat resultatRequete = rezo.requete("toto");
		
		//L'objet qui vous int�resse est certainement le "Mot". Il contient les informations pr�sentes dans rezoJDM.
		Mot mot = resultatRequete.getMot();
		//Un Mot est notamment compos� de relations sortantes (pour lesquelles il en est la source) et de relations entrantes (pour lesquelles il en est la destination).
		//Un Voisin est un noeud rezoJDM ainsi que le poids de la relation associ�e.
		
		ArrayList<Relation> voisins = mot.getRelationsSortantesTypees("r_lieu");
		System.out.println("Un toto peut se trouver dans les lieux suivants : ");		
		for(Relation voisin : voisins) {
			System.out.println("\t"+voisin);
		}
		
		//Attention n�anmoins, une requ�te retourne toujours un objet Resultat mais pas toujours un Mot ! 
		//Le serveur peut-�tre indisponible, le mot inexistant, ou bien encore trop gros pour �tre transmis dans les temps. 
		//Pour �viter les NullPointerException, pensez � v�rifier l'objet Mot !
		mot = resultatRequete.getMot();
		if(mot != null) {
			//traitement
		}
		//Le champ "Etat" de votre objet "Resultat" permet de donner des informations pr�cieuses sur le resultat de votre requ�te :
		Etat etat = resultatRequete.getEtat();
		System.out.println("L'�tat de la requ�te est : "+etat);
		//Les valeurs possibles sont : 
		// - INEXISTANT : le terme demand� n'existe pas dans rezoJDM
		// - TROP_GROS : le Mot r�sultat poss�de plus de 25 000 relations entrantes ou sortantes. 
		//Pour des raisons de performances, le nombre de relations par mot est limit� sur RequeterRezoDump. 
		//Pour palier � ces limitations, utilisez RequeterRezoSQL ou utiliser les requ�tes filtr�es.
		// - RENVOYER : dans le cas d'une requ�te tr�s importante (ex. "personne"), il est parfois n�cessaire de relancer la requ�te pour avoir un r�sultat.
		//Le r�sultat sera alors certainement "TROP_GROS". 
		// - SERVEUR_INACCESSIBLE : Le service rezo-dump n'a pas pu �tre contact�.
		// - OK : La requ�te s'est d�roul�e correctement.
		// - ERREUR_REQUETE : tout autre erreur lors de la requ�te.
		
		EtatCache etatCache = resultatRequete.getEtatCache();
		System.out.println("L'�tat du cache de la requ�te est : "+etatCache);
		//De plus un champ "EtatCache" permet d'obtenir des informations sur le cache :
		// - DEPUIS_CACHE : le r�sultat a �t� produit gr�ce au cache
		// - NOUVELLE_ENTREE : le r�sultat a �t� plac� dans le cache
		// - EN_ATTENTE : le r�sultat n'a pas �t� plac� dans le cache mais son nombre d'occurrence est compt� afin de faire entrer dans le cache
		//les termes souvent demand�s.
		// - ERREUR_REQUETE : une erreur est survenue lors de la requ�te
		
		
		//Pour �viter les �tats "TROP_GROS" ou "RENVOYER", il est conseill� de filter vos requ�tes du mieux possible. 
		//Ainsi, plut�t que de demander le terme "pomme de terre" et de chercher son voisinage pour la relation "lieu" comme pr�c�demment avec "toto"
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
		//filtrer une relation permet d'obtenir des r�sultats bien plus rapidement et d'�tre s�r que le type que vous recherchez n'a pas �t� tronqu� 
		//avec un �tat "TROP_GROS"

		
		// Pour savoir si il y a une relation entre 2 termes , il faut utiliser la méthode "verifierVoisinage" (qui renvoie le poids le + fort)
		// Cette méthode renvoie le poids de la relation (0 si inexistante)
		// Ensuite on va pouvoir utiliser "relationsVoisinage" pour récupérer toutes les relations 
		int poidsRelMax= rezo.verifierVoisinage("toto", "enfant");
		System.out.println("Poids le plus fort des relations entre toto et enfant "+ poidsRelMax);
		ArrayList<Relation> relationsVoisins= rezo.relationsCommunes("toto", "enfant");
		System.out.println("Relations de voisinage entre les termes toto et enfants: ");
		for(Relation voisin: relationsVoisins) {
			System.out.println("\t"+voisin);
		}
		//Pour plus de d�tails, pensez � la javadoc.
		//Bonne chance !		
	}
	
	
}
