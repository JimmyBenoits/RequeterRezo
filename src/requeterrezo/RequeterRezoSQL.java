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
 * Version "locale" de RequeterRezo. Les requêtes qui ne sont pas directement récupérées depuis le cache sont effectuées sur un serveur MySQL
 * que l'utilisateur doit mettre en place.
 * L'intérêt de RequeterRezoSQL est sa performance par rapport à {@link RequeterRezoDump} et l'absence de limitation. 
 * En contrepartie, l'utilisateur doit importer les données de rezoJDM (disponible sous licence "Domaine Publique" à l'adresse :
 * http://www.jeuxdemots.org/JDM-LEXICALNET-FR/?C=M;O=D
 * 
 * L'importation est laissée à l'utilisateur mais il doit respecter certaines règles. 
 * 
 * I] Les noeuds
 * Les noeuds doivent être stockés dans une table "nodes" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *   - "name" (varchar)
 *   - "type" (int, qui vient de node_types)
 *   - "weight" (int)
 *   
 * II] Les relations
 * Les relations doivent être stockées dans une tables "edges" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *   - "source" (int, id de "nodes")
 *   - "destination" (int, id de "nodes")
 *   - "type" (int, qui vient de edge_types)
 *   - "weight" (int)
 *   
 * III] Type de noeuds 
 * Les types de noeuds doivent être stockés dans une table "node_types" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *	 - "name" (varchar)
 *
 * IV] Type de relations 
 * Les types de relations doivent être stockés dans une table "edge_types" contenant (au moins) les colonnes suivantes : 
 *   - "id" (int, primary)
 *	 - "name" (varchar)
 * 
 * 
 * De plus, pour faire fonctionner RequeterRezoSQL, il est nécessaire d'ajouter à votre projet un mysql-connector.
 * 
 * Enfin, si vous souhaitez contribuer au projet JeuxDeMots en envoyant les données récoltées, vous pouvez utiliser des identifiants négatifs 
 * pour vos noeuds et vos relations. Ces valeurs ne sont pas utilisées et permettent une fusion simplifiée !

 * @author jimmy.benoits
 */
public class RequeterRezoSQL extends RequeterRezo {

	/**
	 * Connexion avec la base MySQL
	 */
	protected static Connection connexion;

	/**
	 * Requête utiliser de nombreuses fois pour obtenir un noeud à partir de son identifiant. 
	 * Cela permet notamment de construire les mots formatés.  
	 */
	protected PreparedStatement formerNoeud;


	/**
	 * Construit un objet RequeterRezoSQL à partir d'une configuration spéficique puis effectue les requêtes nécessaires afin de construire les
	 * équivalences entre nom et type de relation. 
	 * @param configuration Configuration spécifique à RequeterRezoSQL comprenant les informations de bases ainsi que les éléments spécifiques
	 * à la connexion à un serveur MySQL.
	 */
	public RequeterRezoSQL(ConfigurationSQL configuration) {
		super();
		connexion(configuration);
		construireRelations();
	}

	/**
	 * Construit un objet RequeterRezoSQL à partir des éléments par défaut et des informations nécessaires pour se connecter au serveur MySQL.
	 * @param serveurSql Adresse du serveur MySQL.
	 * @param nomBaseDeDonnees Nom de la base de données MySQL hébergeant les données de rezoJDM.
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
	 * Construit le mot formaté à partir d'un nom.
	 * @param nom Nom d'un noeud.
	 * @return Le paramètre d'entrée si le mot formaté est identique. Le mot formaté sinon (remplace notamment les identifiants par leurs noms
	 * lorsque cela est nécessaire). 
	 */
	protected String construireMotFormate(String nom) {
		String res = nom;
		ResultSet rs;
		//cas particulier QUESTIONS, exemple :  ::>16:70527>29:83270>13
		//"Qui pourrait divertir avec une musique ?"
		//::>ID_REL_1:ID_MOT_1>ID_REL_2:ID_MOT_2>ID_REL_3
		String regex = "::>(\\d+):(\\d+)>(\\d+):(\\d+)(>(\\d+))?";
		//second cas particulier TRIPLET, exemple :   ::>66:60902>17:219016
		//"dent [carac] cariÃ©e"
		String[] raffs;
		int raff;
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(nom);
		PreparedStatement ps_node_name;
		PreparedStatement ps_edge_types;
		//		long time_before, time_after;
		if (matcher.find()) {
			try {
				ps_edge_types = connexion.prepareStatement("select name from edge_types where id=?;");
				ps_node_name = connexion.prepareStatement("select name from nodes where id=?;");
				int type_rel_1 = Integer.parseInt(matcher.group(1));
				long id_mot_1 = Long.parseLong(matcher.group(2));
				int type_rel_2 = Integer.parseInt(matcher.group(3));
				long id_mot_2 = Long.parseLong(matcher.group(4));
				int type_rel_3 = -1;
				if (matcher.group(5) != null) {
					type_rel_3 = Integer.parseInt(matcher.group(6));
				}
				res = "::>";
				//1er type relation
				ps_edge_types.setInt(1, type_rel_1);
				rs = ps_edge_types.executeQuery();
				if (rs.next()) {
					res += rs.getString(1) + ":";
				} else {
					res += "[TYPE_INCONNU]:";
				}
				//1er nom noeud
				ps_node_name.setLong(1, id_mot_1);
				rs = ps_node_name.executeQuery();				
				if (rs.next()) {
					res += rs.getString(1) + ">";
				} else {
					res += "[NOEUD_INCONNU]>";
				}
				//2e type relation
				ps_edge_types.setInt(1, type_rel_2);
				rs = ps_edge_types.executeQuery();
				if (rs.next()) {
					res += rs.getString(1) + ":";
				} else {
					res += "[TYPE_INCONNU]:";
				}
				//2e nom noeud
				ps_node_name.setLong(1, id_mot_2);
				rs = ps_node_name.executeQuery();
				if (rs.next()) {
					res += rs.getString(1) + "";
				} else {
					res += "[NOEUD_INCONNU]";
				}
				//3e type relation
				if (type_rel_3 != -1) {
					ps_edge_types.setInt(1, type_rel_3);
					rs = ps_edge_types.executeQuery();
					if (rs.next()) {
						res += ">" + rs.getString(1);
					} else {
						res += ">[TYPE_INCONNU]";
					}
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}

		} else if (nom.contains(">")) {
			raffs = nom.split(">");
			res = raffs[0];
			try {
				ps_node_name = connexion.prepareStatement("select name from nodes where id=?;");
				for (int i = 1; i < raffs.length; ++i) {
					raff = Integer.parseInt(raffs[i]);
					ps_node_name.setInt(1, raff);
					rs = ps_node_name.executeQuery();
					if (rs.next()) {
						res += ">" + rs.getString(1);
					} else {
						res += ">" + raff;
						if(avertissement) {
							System.err.println("Avertissement RequeterRezo : lors de la création du mot formaté pour le noeud \"" + nom + "\", le raffinement \"" + raff + "\" n'a pas pu être trouvé");
						}
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
		boolean isType128 = cleCache.typeRelation == 128;
		String definition = "Pas de définition dans RequeterRezoSQL.";
		String nomFormate;		
		long idRezo, idRelation;
		int type;
		int poids;
		HashMap<Long, Noeud> voisinage = new HashMap<>();
		HashMap<Integer, ArrayList<Voisin>> relationsEntrantes = new HashMap<>();
		HashMap<Integer, ArrayList<Voisin>> relationsSortantes = new HashMap<>();
		ArrayList<Annotation> annotations = new ArrayList<>();

		Noeud motAjoute;
		ResultSet rs_noeud;
		ResultSet rs_relations;
		ResultSet rs_annotation;		
		String mot_formate_autre_noeud;

		Statement requete_relations;	
		long id_autre_noeud;
		int type_rel, poids_rel;
		String nom_autre_noeud;
		int  type_autre_noeud, poids_autre_noeud; 
		long id_relation_annote;
		Noeud source, destination;
		int type_relation_annote, poids_relation_annote;
		String nomRelationAnnote;
		PreparedStatement relation_depuis_id;
		Statement noeud_depuis_nom;
		//		long time_before, time_after;
		try {			
			relation_depuis_id = connexion.prepareStatement("select source, destination, type, weight from edges where id=?;");
			noeud_depuis_nom = connexion.createStatement();
			rs_noeud = noeud_depuis_nom.executeQuery("select id, type, weight from nodes where name=\""+nom+"\";");
			if(rs_noeud.next()) {				
				idRezo= rs_noeud.getInt(1);			
				nomFormate=this.construireMotFormate(nom);
				type=rs_noeud.getInt(2);				
				poids=rs_noeud.getInt(3);
				//On ajoute le noeud dans son voisinage
				voisinage.put(idRezo, new Noeud(nom, idRezo, type, nomFormate, poids));

				//Relations sortantes
				String requeteRelationsSortantes=""
						+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
						+ "from edges e, nodes n "
						+ "where e.source= \""+idRezo+"\" and e.destination=n.id ";
				if(cleCache.typeRelation>=0) {
					requeteRelationsSortantes+="and e.type="+cleCache.typeRelation;
				}	
				requeteRelationsSortantes += ";";
				if(cleCache.filtre != Filtre.RejeterRelationsSortantes  && cleCache.filtre != Filtre.RejeterRelationsEntrantesEtSortantes) {
					requete_relations = connexion.createStatement();
					rs_relations = requete_relations.executeQuery(requeteRelationsSortantes);
					while (rs_relations.next()) {										
						type_rel=rs_relations.getInt(1);	
						poids_rel = rs_relations.getInt(2);
						id_autre_noeud = rs_relations.getInt(3);
						nom_autre_noeud = rs_relations.getString(4);
						mot_formate_autre_noeud = this.construireMotFormate(nom_autre_noeud);
						type_autre_noeud = rs_relations.getInt(5);
						poids_autre_noeud = rs_relations.getInt(6);
						idRelation = rs_relations.getInt(7);
						//cas annotation. Si la reqûete porte sur le type 128, on ne considère pas cela comme une annotation
						if(!isType128 && type_rel == 128 && nom_autre_noeud.startsWith(":r")) {
							id_relation_annote = Long.parseLong(nom_autre_noeud.substring(2));
							relation_depuis_id.setLong(1, id_relation_annote);									
							rs_annotation = relation_depuis_id.executeQuery();							
							if(rs_annotation.next()) {								
								source=this.formerNoeud(rs_annotation.getInt(1));		
								if(source != null) {
									destination=this.formerNoeud(rs_annotation.getInt(2));	
									if(destination != null) {
										type_relation_annote=rs_annotation.getInt(3);																
										poids_relation_annote=rs_annotation.getInt(4);		
										nomRelationAnnote = RequeterRezo.correspondancesRelations.get(type_relation_annote);
										annotations.add(new Annotation(
												nom_autre_noeud,id_autre_noeud,type_autre_noeud,poids_autre_noeud,
												source,
												type_relation_annote,nomRelationAnnote,
												destination,
												poids_relation_annote));
									}else if(avertissement) {										
										System.err.println("Avertissement RequeterRezo : la destination (id="+rs_annotation.getInt(2)+") de l'annotation \""+mot_formate_autre_noeud+" n'existe pas.");										
									}
								}else if(avertissement) {
									System.err.println("Avertissement RequeterRezo : la source (id="+rs_annotation.getInt(1)+") de l'annotation \""+mot_formate_autre_noeud+" n'existe pas.");								
								}
							}else if(avertissement) {
								System.err.println("Avertissement RequeterRezo : aucune relation ne correspond à l'annotation \""+nom_autre_noeud+"\".");								
							}
							rs_annotation.close();
						} else {
							if(!(relationsSortantes.containsKey(type_rel))) {
								relationsSortantes.put(type_rel, new ArrayList<>());
							}
							motAjoute=new Noeud(nom_autre_noeud, id_autre_noeud,type_autre_noeud, mot_formate_autre_noeud,poids_autre_noeud);						
							voisinage.put(id_autre_noeud,motAjoute);
							relationsSortantes.get(type_rel).add(new Voisin(motAjoute, poids_rel, idRelation));
						}
					}					
					relation_depuis_id.close();
					rs_relations.close();
					requete_relations.close();
				}

				//relations entrantes
				String requeteRelationsEntrantes=""
						+ "select e.type, e.weight, n.id, n.name, n.type, n.weight, e.id "
						+ "from edges e, nodes n "
						+ "where e.destination= \""+idRezo+"\" and e.source=n.id ";
				if(cleCache.typeRelation>=0) {
					requeteRelationsEntrantes+="and e.type="+cleCache.typeRelation;
				}				
				requeteRelationsEntrantes+=";";				
				if(cleCache.filtre != Filtre.RejeterRelationsEntrantes && cleCache.filtre != Filtre.RejeterRelationsEntrantesEtSortantes) {
					requete_relations = connexion.createStatement();
					rs_relations = requete_relations.executeQuery(requeteRelationsEntrantes);
					while (rs_relations.next()) {
						type_rel=rs_relations.getInt(1);	
						poids_rel = rs_relations.getInt(2);
						id_autre_noeud = rs_relations.getInt(3);
						nom_autre_noeud = rs_relations.getString(4);
						mot_formate_autre_noeud = this.construireMotFormate(nom_autre_noeud);
						type_autre_noeud = rs_relations.getInt(5);
						poids_autre_noeud = rs_relations.getInt(6);
						idRelation = rs_relations.getInt(7);
						//Pas d'annotations dans les relations entrantes 
						if(!(relationsEntrantes.containsKey(type_rel))) {
							relationsEntrantes.put(type_rel, new ArrayList<>());
						}								
						motAjoute=new Noeud(nom_autre_noeud, id_autre_noeud,type_autre_noeud, mot_formate_autre_noeud,poids_autre_noeud);
						voisinage.put(id_autre_noeud,motAjoute);							
						relationsEntrantes.get(type_rel).add(new Voisin(motAjoute, poids_rel,idRelation));
					}
				}
				Mot mot = new Mot(nom, idRezo, type, nomFormate, poids, definition,
						voisinage, relationsEntrantes, relationsSortantes, annotations,
						cleCache);
				resultat = new Resultat(cleCache, mot, Etat.OK, EtatCache.EN_ATTENTE);
			}else {
				resultat.etat = Etat.INEXISTANT;
			}
			rs_noeud.close();
			noeud_depuis_nom.close();
		}
		catch(SQLException e) {
			e.printStackTrace();
		}
		return resultat;
	}

	/**
	 * Construit un noeud à partir de son identifiant. 
	 * @param id Identifiant rezoJDM.
	 * @return Le noeud s'il existe, null sinon.
	 */
	protected Noeud formerNoeud(int id) {
		Noeud res=null;
		try {			
			this.formerNoeud.setInt(1, id);
			ResultSet rs=formerNoeud.executeQuery();
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
	 * Construit une connexion avec le serveur MySQL à partir d'un objet de configuration. 
	 * @param configuration Configuration spéficique à RequeterRezoSQL
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
			this.formerNoeud = connexion.prepareStatement("select name, type, weight from nodes where id=?;");
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

	@Override
	public int verifierExistenceRelation(String motSource, String nomTypeRelation, String motDestination) {
		//TODO:
		throw new UnsupportedOperationException("Not implemented yet.");
	}
}
