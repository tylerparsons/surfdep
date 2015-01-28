package bdm.largesystems.utils;

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
