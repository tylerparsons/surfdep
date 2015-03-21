/**
 * Copyright 2015, Tyler Parsons
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.emory.physics.surfdep.utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLClient extends SQLClient {

	private final static String driver = "com.mysql.jdbc.Driver";
	private final static String host = "jdbc:mysql://localhost/";
	
	private static MySQLClient singleton;
	
	private MySQLClient(String db, String user, String pass) {
		
		super(host+db+"?user="+user+"&password="+pass, driver);
	
	}
	
	public static MySQLClient getSingleton(String db, String user, String pass) {
		if (singleton == null) {
			singleton = new MySQLClient(db, user, pass);
		}
		return singleton;
	}
	
	/**
	 * Executes an SQL statement.
	 * @param sql An SQL statement
	 */
	public void exec(String sql) {
		
		try {
			Statement stmt = mConnection.createStatement();
			stmt.execute(sql);
			stmt.close();
		}
		catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		
	}
	
	/**
	 * Executes an SQL query.
	 * @param sql An SQL statement
	 * @return ResultSet or null
	 */
	public ResultSet query(String sql) {
		
		try {
			Statement stmt = mConnection.createStatement();
			ResultSet rs = stmt.executeQuery(sql);
			return rs;
		}
		catch (SQLException sqle) {
			sqle.printStackTrace();
			return null;
		}
		
	}
	
	/**
	 * Updates a table or inserts a new row if there are no matching rows.
	 * 
	 * @param setClause SQL assignment expression(s)
	 * @param whereClause SQL boolean expression
	 */
	public void update(String table, String setClause, String whereClause) {
		
		try {

			Statement stmt = mConnection.createStatement();
			String updateStmt = "UPDATE "+table+" SET "+setClause+" WHERE "+whereClause;
			
			// Insert new record if update is unsuccessful
			if (stmt.executeUpdate(updateStmt) == 0) {
				
				// Generate insert statement
				String columns = "";
				String values = "";
				String[] setAssignments = setClause.split(",");
				String[] whereAssignments = whereClause.split(",");
				// Parse set clauses
				for (String assignment: setAssignments) {
					String[] kvPair = assignment.split("=");
					columns += kvPair[0] + ",";
					values += kvPair[1] + ",";
				}
				// Parse where clauses
				for (String assignment: whereAssignments) {
					String[] kvPair = assignment.split("=");
					columns += kvPair[0] + ",";
					values += kvPair[1] + ",";
				}
				
				stmt.execute(
					"INSERT INTO " + table +
					"COLUMNS " + columns.substring(0, columns.length() - 1) +
					"VALUES " + values.substring(0, values.length() - 1)
				);
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
/****************************
 * Width Table Manipulation *
 ****************************/
	
	public void clearWidthTable() {
		
		try {
			
			// Create width table
			Statement stmt = mConnection.createStatement();
			stmt.executeUpdate("DELETE FROM width WHERE 1;");
			stmt.close();
			
		}
		catch (SQLException e) {e.printStackTrace();}
		
	}
	
	public void addWidthRecords(double[] vals) {
	
		long start = System.currentTimeMillis();
	
		try {
	
			// Add the value for width at the specified time
			PreparedStatement stmt = mConnection.prepareStatement(
				"INSERT INTO width (w) VALUES (?)"
			);
			// Disable auto commit for batch insert
			mConnection.setAutoCommit(false);
			
			int i;
			for (i = 0; i < vals.length; i++) {
			
				stmt.setDouble(1, vals[i]);
				stmt.addBatch();
				
				if (i % BATCH_SIZE == 0)
					stmt.executeBatch();
				
			}
			
			// Execute remaining batches
			if ((i-1) % BATCH_SIZE != 0)
				stmt.executeBatch();
			
			// Reset auto commit
			mConnection.setAutoCommit(true);
			
			// Suggest Garbage Collection
			stmt = null;
			System.gc();
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (OutOfMemoryError e) {
			e.printStackTrace();
		}
	
		System.out.println(	"Added "+vals.length+" records in "
							+(System.currentTimeMillis()-start)+"ms");
	
	}
	
	/**
	 * Selects all records between the {@code from} time, inclusive,
	 * to the {@code to} time, exclusive.
	 */
	public ResultSet queryWidthRecords(long from, long to) {
		
		long start = System.currentTimeMillis();
		
		try {
	
			// Add the value for width at the specified time
			Statement stmt = mConnection.createStatement();
			
			ResultSet results = stmt.executeQuery(
				"SELECT * FROM width WHERE t>="+from+" AND t<"+to
			);
			
			System.out.println("Queried "+(to-from)+" records in "+(System.currentTimeMillis()-start)+"ms.");
			
			return results;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	
	}
	
}
