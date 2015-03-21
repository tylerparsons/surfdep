package edu.emory.physics.surfdep.utils;

import java.util.HashMap;

/**
 * ModelGroupIdentifier.java
 * 
 * A Data class for identifying specific
 * groups of models across varying repr-
 * esentations such as comma delimited 
 * user input and SQL query syntax.
 * 
 * @author Tyler
 *
 */
public class ModelGroupIdentifier {

	protected String sqlWhereClause;
	protected HashMap<String, String> inputParams;
	
	/**
	 * Creates a ModelGroupIdentifier from the given constraints.
	 * @param inputParams	A map of valid model fields to
	 * 						comma delimited lists of dashed
	 * 						ranges for each parameter.
	 */
	public ModelGroupIdentifier(HashMap<String, String> params) throws IllegalArgumentException {
		inputParams = params;
		sqlWhereClause = genSqlWhereClause(inputParams);
	}
	
	/**
	 * Creates a sqlWhereClause from a comma delimited
	 * list of dashed ranges for each model field.
	 */
	public static String genSqlWhereClause(HashMap<String, String> inputParams) throws IllegalArgumentException {
		
		String sql = "";
		
		for (String key: inputParams.keySet()) {
		
			// Split into comma delimited "ranges",
			// which may be ranges or single numbers
			String[] ranges = inputParams.get(key).split(",");
			
			sql += "(";
			
			for (String range: ranges) {
				
				String[] limits = range.split("-");
				
				if (limits.length == 2) {
					sql += "("+key+" BETWEEN "+limits[0]+" AND "+limits[1]+") OR ";
				}
				else if (limits.length == 1) {
					
					if(limits[0].equals("")) {
						// Empty entry, do not generate assignment
						continue;
					}
					
					sql += key+"="+limits[0]+" OR ";
				}
				else {
					throw new IllegalArgumentException("Invalid range entered");
				}
				
			}
			
			// Remove open parenthesis for empty ranges
			if (sql.substring(sql.length()-1).equals("(")) {
				sql = sql.substring(0, sql.length()-1);
			}
			// Replace trailing " OR "s with ") AND "
			else if (sql.substring(sql.length() - 4).matches(" OR ")) {
				sql = sql.substring(0, sql.length() - 4) + ") AND ";
			}
		
		}
			
		// Handle empty case
		if (sql.equals("")) {
			return "1";
		}
		// Remove trailing " AND "
		else if (sql.substring(sql.length() - 5).matches(" AND ")) {
			return sql.substring(0, sql.length() - 5);
		}
		// Default
		else {
			return "1";
		}
		
	}
	
	public String sqlWhereClause() {
		return sqlWhereClause;
	}
	
	public HashMap<String, String> getInputParams() {
		return inputParams;
	}
	
}
