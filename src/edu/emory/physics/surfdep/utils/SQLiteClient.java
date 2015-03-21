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

import java.io.File;
import java.lang.Throwable;
import java.sql.*;

/**
 * @deprecated
 * @author Tyler
 */
public class SQLiteClient extends SQLClient {
	
	private String dbUrl;
	
	public SQLiteClient(String dbUrl) {
		
		super("jdbc:sqlite:"+dbUrl, "org.sqlite.JDBC");
		
		this.dbUrl = dbUrl;
		
		try {
	
			// Create width table
			Statement stmt = mConnection.createStatement();
			stmt.executeUpdate("DROP TABLE IF EXISTS width");
			stmt.executeUpdate(
				"CREATE TABLE width (" +
					"time INTEGER PRIMARY KEY AUTOINCREMENT," +
					"value REAL" +
				")"
			);			
			
		}
		catch (SQLException e) {e.printStackTrace();}
	
	}
	
	public void deleteDb() {
		File db = new File(".\\"+dbUrl);
		db.delete();
	}
	
	/**
	 * Adds a record with the given time and width
	 * without checking for duplicate entries.
	 */
	public void addRecord(long time, double width) {
	
		try {
	
			// Add the value for width at the specified time
			Statement stmt = mConnection.createStatement();
			stmt.executeUpdate("INSERT INTO width (value) VALUES ("+width+")");
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	public void addRecords(double[] vals) {
	
		long start = System.currentTimeMillis();
	
		try {
	
			// Add the value for width at the specified time
			PreparedStatement stmt = mConnection.prepareStatement(
				"INSERT INTO width (value) VALUES (?)"
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
	public ResultSet queryRecords(long from, long to) {
		
		long start = System.currentTimeMillis();
		
		try {
	
			// Add the value for width at the specified time
			Statement stmt = mConnection.createStatement();
			
			ResultSet results = stmt.executeQuery(
				"SELECT * FROM width WHERE time>="+from+" AND time<"+to
			);
			
			System.out.println("Queried "+(to-from)+" records in "+(System.currentTimeMillis()-start)+"ms.");
			
			return results;
			
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		deleteDb();
	}
	

}
