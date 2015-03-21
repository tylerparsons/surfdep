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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLClient {
	
	protected Connection mConnection;
	
	protected int BATCH_SIZE = 1024;
	
	
	protected SQLClient(String connectionUrl, String driver) {
	
		try {
	
			// Load Driver class by reflection
			Class.forName(driver);
			// Make initial connection
			mConnection = DriverManager.getConnection(connectionUrl);		
			
		}
		catch (SQLException e) {e.printStackTrace();}
		catch (ClassNotFoundException e) {e.printStackTrace();}
		
	}
	

	protected void finalize() throws Throwable {
		mConnection.close();
		super.finalize();
	}
	
}
