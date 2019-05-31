package requeterrezo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
 * Version "locale" de RequeterRezo. Les requ�tes qui ne sont pas directement r�cup�r�es depuis le cache sont effectu�es sur un serveur MySQL
 * que l'utilisateur doit mettre en place.
 * L'int�r�t de RequeterRezoSQL est sa performance par rapport � {@link RequeterRezoDump} et l'absence de limitation. 
 * En contrepartie, l'utilisateur doit importer les donn�es de rezoJDM (disponible sous licence "Domaine Publique" � l'adresse :
 * http://www.jeuxdemots.org/JDM-LEXICALNET-FR/?C=M;O=D
 * 
 * L'importation est laiss�e � l'utilisateur mais il doit respecter certaines r�gles. 
 * 
 * I] Les noeuds
 * Les noeuds doivent �tre stock�s dans une table "nodes" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *   - "name" (varchar)
 *   - "type" (int, qui vient de node_types)
 *   - "weight" (int)
 *   
 * II] Les relations
 * Les relations doivent �tre stock�es dans une tables "edges" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *   - "source" (int, id de "nodes")
 *   - "destination" (int, id de "nodes")
 *   - "type" (int, qui vient de edge_types)
 *   - "weight" (int)
 *   
 * III] Type de noeuds 
 * Les types de noeuds doivent �tre stock�s dans une table "node_types" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *	 - "name" (varchar)
 *
 * IV] Type de relations 
 * Les types de relations doivent �tre stock�s dans une table "edge_types" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *	 - "name" (varchar)
 * 
 * 
 * De plus, pour faire fonctionner RequeterRezoSQL, il est n�cessaire d'ajouter � votre projet un mysql-connector.
 * 
 * Enfin, si vous souhaitez contribuer au projet JeuxDeMots en envoyant les donn�es r�colt�es, vous pouvez utiliser des identifiants n�gatifs 
 * pour vos noeuds et vos relations. Ces valeurs ne sont pas utilis�es et permettent une fusion simplifi�e !

 * @author jimmy.benoits
 */
public class RequeterRezoSQL extends RequeterRezo {

	/**
	 * Expression r�guli�re permettant de d�tecter les formes complexes de nom de rezoJDM telles que les questions ou les agr�gats. 
	 */
	private Pattern schemaAgregat = Pattern.compile("::>(\\d+):(\\d+)>(\\d+):(\\d+)(>(\\d+))?");

	/**
	 * Connexion avec la base MySQL
	 */
	protected static Connection connexion;

	/**
	 * Requ�te utiliser de nombreuses fois pour obtenir un noeud � partir de son identifiant. 
	 * Cela permet notamment de construire les mots format�s.  
	 */
	protected PreparedStatement noeudDepuisID; //select name, type, weight from nodes where id=?	
	protected PreparedStatement noeudDepuisNom; //select id, type, weight from nodes where name=?
	protected PreparedStatement nomNoeud; //select name from nodes where id=?

	protected PreparedStatement relationDepuisID;//"select source, destination, type, weight from edges where id=?;"

	protected PreparedStatement nomTypeRelation;//connexion.prepareStatement("select name from edge_types where id=?;");

	protected PreparedStatement relationsSortantes;
	protected PreparedStatement relationsSortantesType;
	protected PreparedStatement relationsEntrantes;
	protected PreparedStatement relationsEntrantesType;


	/**
	 * Construit un objet RequeterRezoSQL � partir d'une configuration sp�ficique puis effectue les requ�tes n�cessaires afin de construire les
	 * �quivalences entre nom et type de relation. 
	 * @param configuration Configuration sp�cifique � RequeterRezoSQL comprenant les informations de bases ainsi que les �l�ments sp�cifiques
	 * � la connexion � un serveur MySQL.
	 */
	public RequeterRezoSQL(ConfigurationSQL configuration) {
		super();
		connexion(configuration);
		construireRelations();
	}

	/**
	 * Construit un objet RequeterRezoSQL � partir des �l�ments par d�faut et des informations n�cessaires pour se connecter au serveur MySQL.
	 * @param serveurSql Adresse du serveur MySQL.
	 * @param nomBaseDeDonnees Nom de la base de donn�es MySQL h�bergeant les donn�es de rezoJDM.
	 * @param nomUtilisateur Nom d'utilisateur.
	 * @param motDePasse Mot de passe.
	 */
	public RequeterRezoSQL(String serveurSql, String nomBaseDeDonnees, String nomUtilisateur, String motDePasse) {
		super();
		ConfigurationSQL configuration = new ConfigurationSQL(serveurSql, nomBaseDeDonnees, nomUtilisateur, motDePasse);
		connexion(configuration);
		construireRelations();
	}

	@Override
	protected final void construireRelations() {
		CorrespondanceRelation correspondance = RequeterRezo.correspondancesRelations;        
		String nom;
		int id;
		try {
			try (Statement statement = connexion.createStatement()) {
				try (ResultSet rs = statement.executeQuery("select name, id from edge_types;")) {
					while (rs.next()) {
						nom = rs.getString(1);
						id = rs.getInt(2);
						correspondance.ajouter(id, nom);
						correspondance.ajouter(nom, id);
					}
				}

			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Construit le mot format� � partir d'un nom.
	 * @param nom Nom d'un noeud.
	 * @return Le param�tre d'entr�e si le mot format� est identique. Le mot format� sinon (remplace notamment les identifiants par leurs noms
	 * lorsque cela est n�cessaire). 
	 */
	protected String construireMotFormate(String nom) {
		String res = nom;
		ResultSet rs;
		//cas particulier QUESTIONS, exemple :  ::>16:70527>29:83270>13
		//"Qui pourrait divertir avec une musique ?"
		//::>ID_REL_1:ID_MOT_1>ID_REL_2:ID_MOT_2>ID_REL_3
		//second cas particulier TRIPLET, exemple :   ::>66:60902>17:219016
		//"dent [carac] cari�e"
		String[] raffs;
		int raff;		
		Matcher matcher = schemaAgregat.matcher(nom);
		if (matcher.find()) {
			try {				
				int typeRel1 = Integer.parseInt(matcher.group(1));
				long idMot1 = Long.parseLong(matcher.group(2));
				int typeRel2 = Integer.parseInt(matcher.group(3));
				long idMot2 = Long.parseLong(matcher.group(4));
				int typeRel3 = -1;
				String motFormateIntermediaire;
				if (matcher.group(5) != null) {
					typeRel3 = Integer.parseInt(matcher.group(6));
				}
				res = "::>";
				//1er type relation
				nomTypeRelation.setInt(1, typeRel1);				
				rs = nomTypeRelation.executeQuery();
				if (rs.next()) {
					res += rs.getString(1) + ":";
				} else {
					res += "[TYPE_INCONNU]:";
				}
				rs.close();
				//1er nom noeud
				nomNoeud.setLong(1, idMot1);				
				rs = nomNoeud.executeQuery();				
				if (rs.next()) {
					motFormateIntermediaire = rs.getString(1);
					motFormateIntermediaire = construireMotFormate(motFormateIntermediaire);
					res += motFormateIntermediaire + ">";
				} else {
					res += "[NOEUD_INCONNU]>";
				}
				rs.close();
				//2e type relation
				nomTypeRelation.setInt(1, typeRel2);				
				rs = nomTypeRelation.executeQuery();
				if (rs.next()) {
					res += rs.getString(1) + ":";
				} else {
					res += "[TYPE_INCONNU]:";
				}
				rs.close();
				//2e nom noeud
				nomNoeud.setLong(1, idMot2);				
				rs = nomNoeud.executeQuery();
				if (rs.next()) {		
					motFormateIntermediaire = rs.getString(1);
					motFormateIntermediaire = construireMotFormate(motFormateIntermediaire);
					res += motFormateIntermediaire + "";
				} else {
					res += "[NOEUD_INCONNU]";
				}
				rs.close();
				//3e type relation
				if (typeRel3 != -1) {
					nomTypeRelation.setInt(1, typeRel3);					
					rs = nomTypeRelation.executeQuery();
					if (rs.next()) {
						res += ">" + rs.getString(1);
					} else {
						res += ">[TYPE_INCONNU]";
					}
					rs.close();
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		} else if (nom.contains(">")) {
			raffs = nom.split(">");
			res = raffs[0];
			try {		
				for(int i = 1; i < raffs.length; ++i) {					
					if(raffs[i].matches("\\d+")) {
						raff = Integer.parseInt(raffs[i]);
						nomNoeud.setInt(1, raff);					
						rs = nomNoeud.executeQuery();
						if(rs.next()) {
							res += ">" + rs.getString(1);
						} else {
							res += ">" + raff;
							if(avertissement) {
								System.err.println("Avertissement RequeterRezo : lors de la cr�ation du mot format� pour le noeud \"" + nom + "\", le raffinement \"" + raff + "\" n'a pas pu �tre trouv�");
							}
						}
						rs.close();
					} else { 
						res += ">"+raffs[i];
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		return res;
	}

	@Override
	protected Resultat construireMot(CleCache cleCache) {
		Resultat resultat = new Resultat(cleCache); 	
		String nom = cleCache.nom;
		boolean estType128 = cleCache.typeRelation == 128;
		String definition = "Pas de d�finition dans RequeterRezoSQL.";
		String nomFormate;		
		long idRezo, idRelation;
		int type;
		int poids;
		Noeud noeudCourant;
		HashMap<Long, Noeud> voisinage = new HashMap<>();
		HashMap<Integer, ArrayList<Relation>> relationsEntrantes = new HashMap<>();
		HashMap<Integer, ArrayList<Relation>> relationsSortantes = new HashMap<>();
		ArrayList<Relation> voisins;
		ArrayList<Annotation> annotations = new ArrayList<>();

		Noeud motAjoute;
		ResultSet rsNoeud;
		ResultSet rsRelations;
		ResultSet rsAnnotation;		

		String motFormateAutreNoeud, nomAutreNoeud;
		int  typeAutreNoeud, poidsAutreNoeud;
		long idAutreNoeud;

		int typeRel, poidsRel;				
		long idRelationAnnote;
		Noeud source, destination;
		int typeRelationAnnote, poidsRelationAnnote;
		String nomRelationAnnote;
		PreparedStatement requeteSortante, requeteEntrante;
		try {			
			noeudDepuisNom.setString(1, nom);
			rsNoeud = noeudDepuisNom.executeQuery("select id, type, weight from nodes where name=\""+nom+"\";");
			if(rsNoeud.next()) {				
				idRezo= rsNoeud.getInt(1);			
				nomFormate=this.construireMotFormate(nom);
				type=rsNoeud.getInt(2);				
				poids=rsNoeud.getInt(3);
				//On ajoute le noeud dans son voisinage
				noeudCourant = new Noeud(nom, idRezo, type, nomFormate, poids);
				voisinage.put(idRezo, noeudCourant);

				//Relations sortantes
				if(cleCache.typeRelation >= 0) {
					requeteSortante = this.relationsSortantesType;
					requeteSortante.setInt(2, cleCache.typeRelation);
				}else {
					requeteSortante = this.relationsSortantes;
				}
				requeteSortante.setLong(1, idRezo);				
				if(cleCache.filtre != Filtre.RejeterRelationsSortantes  && cleCache.filtre != Filtre.RejeterRelationsEntrantesEtSortantes) {					
					rsRelations = requeteSortante.executeQuery();
					while(rsRelations.next()) {										
						typeRel=rsRelations.getInt(1);	
						poidsRel = rsRelations.getInt(2);
						idAutreNoeud = rsRelations.getInt(3);
						nomAutreNoeud = rsRelations.getString(4);
						motFormateAutreNoeud = this.construireMotFormate(nomAutreNoeud);
						typeAutreNoeud = rsRelations.getInt(5);
						poidsAutreNoeud = rsRelations.getInt(6);
						idRelation = rsRelations.getInt(7);
						//cas annotation. Si la req�ete porte sur le type 128, on ne consid�re pas cela comme une annotation
						if(!estType128 && typeRel == 128 && nomAutreNoeud.startsWith(":r")) {
							idRelationAnnote = Long.parseLong(nomAutreNoeud.substring(2));
							relationDepuisID.setLong(1, idRelationAnnote);															
							rsAnnotation = relationDepuisID.executeQuery();							
							if(rsAnnotation.next()) {								
								source=this.formerNoeud(rsAnnotation.getInt(1));		
								if(source != null) {
									destination=this.formerNoeud(rsAnnotation.getInt(2));	
									if(destination != null) {
										typeRelationAnnote=rsAnnotation.getInt(3);																
										poidsRelationAnnote=rsAnnotation.getInt(4);		
										nomRelationAnnote = RequeterRezo.correspondancesRelations.get(typeRelationAnnote);
										annotations.add(new Annotation(
												nomAutreNoeud,idAutreNoeud,typeAutreNoeud,poidsAutreNoeud,
												source,
												typeRelationAnnote,nomRelationAnnote,
												destination,
												poidsRelationAnnote));
									}else if(avertissement) {										
										System.err.println("Avertissement RequeterRezo : la destination (id="+rsAnnotation.getInt(2)+") de l'annotation \""+motFormateAutreNoeud+" n'existe pas.");										
									}
								}else if(avertissement) {
									System.err.println("Avertissement RequeterRezo : la source (id="+rsAnnotation.getInt(1)+") de l'annotation \""+motFormateAutreNoeud+" n'existe pas.");								
								}
							}else if(avertissement) {
								System.err.println("Avertissement RequeterRezo : aucune relation ne correspond � l'annotation \""+nomAutreNoeud+"\".");								
							}
							rsAnnotation.close();
						} else {
							if(!(relationsSortantes.containsKey(typeRel))) {	
								voisins = new ArrayList<>();
								relationsSortantes.put(typeRel, voisins);
							}
							motAjoute=new Noeud(nomAutreNoeud, idAutreNoeud,typeAutreNoeud, motFormateAutreNoeud,poidsAutreNoeud);						
							voisinage.put(idAutreNoeud,motAjoute);
							relationsSortantes.get(typeRel).add(new Relation(idRelation, noeudCourant, typeRel, motAjoute, poidsRel));							
						}
					}	
					rsRelations.close();
				}

				//relations entrantes
				if(cleCache.typeRelation >= 0) {
					requeteEntrante = this.relationsEntrantesType;
					requeteEntrante.setInt(2, cleCache.typeRelation);
				}else {
					requeteEntrante= this.relationsEntrantes;
				}
				requeteEntrante.setLong(1, idRezo);					
				if(cleCache.filtre != Filtre.RejeterRelationsEntrantes && cleCache.filtre != Filtre.RejeterRelationsEntrantesEtSortantes) {					
					rsRelations = requeteEntrante.executeQuery();
					while (rsRelations.next()) {
						typeRel=rsRelations.getInt(1);	
						poidsRel = rsRelations.getInt(2);
						idAutreNoeud = rsRelations.getInt(3);
						nomAutreNoeud = rsRelations.getString(4);
						motFormateAutreNoeud = this.construireMotFormate(nomAutreNoeud);
						typeAutreNoeud = rsRelations.getInt(5);
						poidsAutreNoeud = rsRelations.getInt(6);
						idRelation = rsRelations.getInt(7);
						//Pas d'annotations dans les relations entrantes 
						if(!(relationsEntrantes.containsKey(typeRel))) {
							voisins = new ArrayList<>();
							relationsEntrantes.put(typeRel, voisins);
						}								
						motAjoute=new Noeud(nomAutreNoeud, idAutreNoeud,typeAutreNoeud, motFormateAutreNoeud,poidsAutreNoeud);
						voisinage.put(idAutreNoeud,motAjoute);
						relationsEntrantes.get(typeRel).add(new Relation(idRelation, motAjoute, typeRel, noeudCourant, poidsRel));													
					}
					rsRelations.close();
				}
				Mot mot = new Mot(nom, idRezo, type, nomFormate, poids, definition,
						voisinage, relationsEntrantes, relationsSortantes, annotations,
						cleCache);
				resultat = new Resultat(cleCache, mot, Etat.OK, EtatCache.EN_ATTENTE);
			}else {
				resultat.etat = Etat.INEXISTANT;
			}
			rsNoeud.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return resultat;
	}

	/**
	 * Construit un noeud � partir de son identifiant. 
	 * @param id Identifiant rezoJDM.
	 * @return Le noeud s'il existe, null sinon.
	 */
	protected Noeud formerNoeud(int id) {
		Noeud res=null;
		try {			
			this.noeudDepuisID.setInt(1, id);
			ResultSet rs = noeudDepuisID.executeQuery();
			if(rs.next()) {				
				String nom = rs.getString(1);
				//String nom, long id, int type, String mot_formate, int poids
				res= new Noeud(nom,id,rs.getInt(2), this.construireMotFormate(nom),rs.getInt(3));
			} else if(avertissement){
				System.err.println("Avertissement RequeterRezo : le noeud d'id "+id+" n'existe pas.");
			}
			rs.close();
		}
		catch(SQLException e) {
			e.printStackTrace();					
		}
		return res;
	}

	/**
	 * Construit une connexion avec le serveur MySQL � partir d'un objet de configuration. 
	 * @param configuration Configuration sp�ficique � RequeterRezoSQL
	 */
	protected final void connexion(ConfigurationSQL configuration) {
		String connexion_string = "jdbc:mysql://" + configuration.getServeur_SQL() + "/" + configuration.getNom_base_de_donnees();
		if (!configuration.getParametres().isEmpty()) {
			connexion_string += "?";
			for (Entry<String, String> entry : configuration.getParametres()) {
				connexion_string += entry.getKey() + "=" + entry.getValue() + "&";
			}
		}
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			connexion = DriverManager.getConnection(connexion_string, configuration.getNom_utilisateur(), configuration.getMot_de_passe());
			/*
			 * PreparedStatement : requ�tes courantes, pr�compil�es.
			 */
			//Noeuds
			this.noeudDepuisID = connexion.prepareStatement("select name, type, weight from nodes where id=?;");
			this.noeudDepuisNom = connexion.prepareStatement("select id, type, weight from nodes where name=?;");
			this.nomNoeud = connexion.prepareStatement("select name from nodes where id=?;");

			//Type relation
			this.nomTypeRelation = connexion.prepareStatement("select name from edge_types where id=?;");

			//Relations
			this.relationDepuisID = connexion.prepareStatement("select source, destination, type, weight from edges where id=?;");

			//Requ�tes			
			this.relationsSortantes = connexion.prepareStatement(""
					+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
					+ "from edges e, nodes n "
					+ "where e.source=? and e.destination=n.id;");
			this.relationsSortantesType = connexion.prepareStatement(""
					+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
					+ "from edges e, nodes n "
					+ "where e.source=? and e.destination=n.id and e.type=?;");

			this.relationsEntrantes = connexion.prepareStatement(""
					+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
					+ "from edges e, nodes n "
					+ "where e.destination=? and e.source=n.id;");
			this.relationsEntrantesType = connexion.prepareStatement(""
					+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
					+ "from edges e, nodes n "
					+ "where e.destination=? and e.source=n.id and e.type=?;");


		} catch (SQLException e) {
			e.printStackTrace();
			this.sauvegarder();
			System.exit(1);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			this.sauvegarder();
			System.exit(1);
		}
	}
}
