package surfdep.utils;
	
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;


public class EmbeddedDBArray {

	public final static long MAX_ARRAY_SIZE = (long)((Integer.MAX_VALUE) >> 6);
	
	final static int DUMMY_OFFSET = -1;

	/**
	 * Handles connections to the MySQL database with which
	 * this EmbeddedDBArray stores and accesses its values.
	 */
	private MySQLClient dbClient;
	
	/**
	 * Stores a "working copy" of a section of the database,
	 * which can be modified and accessed in memory and then
	 * pushed to the database.
	 */
	private double[] local;
	
	/**
	 * Indicates the position in the database corresponding 
	 * to the start of the array stored in memory.  Stored 
	 * as an index of an array of size MAX_ARRAY_SIZE in the
	 * pseudo two-dimensional array of the database, such that
	 * {@code ((long)currentOffset)*MAX_ARRAY_SIZE} gives the actual
	 * position of the first element of the array in the db.
	 */
	private int currentOffset;
	
	/**
	 * Stores a change flag for each db section, indicating
	 * whether the database has been updated since the array
	 * was last populated.
	 */
	private ArrayList<Boolean> changeList;
	
	/**
	 * Stores a flag whether each db section has been used for
	 * the first time. This eliminates a costly, unnecessary
	 * pull call when the section is used for the first time.
	 */
	private ArrayList<Boolean> useList;
	
	/**
	 * Tracks how many rows have been added to the database.
	 */
	private long recordsAdded;
	
	/**
	 * Callback interface to implement instructions upon the start
	 * and conclusion of pushing the local array to the database.
	 */
	private DBOperationCallback pushCallback;
	
	/**
	 * Callback interface to implement instructions upon the start
	 * and conclusion of pulling the local array from the database.
	 */
	private DBOperationCallback pullCallback;
	
	
	public EmbeddedDBArray (long suggestedCapacity) {
	
		dbClient = MySQLClient.getSingleton("depositions", "bdm", "d3po$ition$");
		dbClient.clearWidthTable();
	
		local = new double[(int)(suggestedCapacity < MAX_ARRAY_SIZE ? suggestedCapacity : MAX_ARRAY_SIZE)];
		
		int listSize = (int)(suggestedCapacity/MAX_ARRAY_SIZE) + 1;
		changeList = new ArrayList<Boolean>(listSize);
		useList = new ArrayList<Boolean>(listSize);
		
		for (int i = 0; i < listSize; i++) {
			changeList.add(new Boolean(false));
			useList.add(new Boolean(false));
		}
		
		currentOffset = DUMMY_OFFSET;	// Forces currentOffset to be reset when any offset is accessed
		recordsAdded = 0L;
	}
	
	/**
	 * Adds a value to the db at the auto-incremented row {@code time}.
	 */
	public void record(double value) {
	
		int newOffset = (int)(recordsAdded/MAX_ARRAY_SIZE);
	
		if (newOffset != currentOffset) {
			
			// Push any changes for previous currentOffset to database
			if (currentOffset != DUMMY_OFFSET
			&&  changeList.get(currentOffset)) {
				push(currentOffset);
			}
			
			// Populate local array from database
			if (useList.get(newOffset)) {
				pull(newOffset);
			}
			// Indicate that this array has been used
			else {
				useList.remove(newOffset);
				useList.add(newOffset, new Boolean(true));
			}
		
			// Update currentOffset
			currentOffset = newOffset;
		}
		
		// Set value in working copy i.e. array stored in memory, indicate change
		local[(int)(recordsAdded++ % MAX_ARRAY_SIZE)] = value;
		changeList.remove(currentOffset);
		changeList.add(currentOffset, new Boolean(true));
	}
	
	
	public double get(long index) {
	
		int newOffset = (int)(index/MAX_ARRAY_SIZE);
	
		if (newOffset != currentOffset) {
			
			// Push any changes for previous currentOffset to database
			if (currentOffset != DUMMY_OFFSET
			&&  changeList.get(currentOffset)) {
				push(currentOffset);
			}
			
			// Populate local array from database
			if (useList.get(newOffset)) {
				pull(newOffset);
			}
			// Indicate that this array has been used
			else {
				useList.remove(newOffset);
				useList.add(newOffset, new Boolean(true));
			}
		
			// Update currentOffset
			currentOffset = newOffset;
		}
		

		// Return value from working copy i.e. array stored in memory
		return local[(int)(index%MAX_ARRAY_SIZE)];
	}
	
	
/****************
 * DB Utilities *
 ****************/


	private void pull(int offset) {
		
		// Invoke opStart callback
		if (pullCallback != null)
			pullCallback.onOperationStarted();
	
		System.out.println("pull("+offset+") called.");
		long start = System.currentTimeMillis();
	
		ResultSet records = dbClient.queryWidthRecords(
			((long)offset)	* MAX_ARRAY_SIZE,
			((long)offset+1)* MAX_ARRAY_SIZE
		);
		
		// Populate array from ResultSet, fill remaining indices with NaN
		int index = 0;
		try {
		
			while(records.next()) {
				local[index++] = records.getDouble("w");
			}
			System.out.println("Read "+index+" records from db.");
			// Let remaining array elements be 0
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		// Update changeList
		changeList.remove(offset);
		changeList.add(offset, new Boolean(false));
		
		long opTime = System.currentTimeMillis() - start;
		System.out.println("pull("+offset+") completed in "+opTime+"ms.");
		
		// Invoke opCompleted callback
		if (pullCallback != null)
			pullCallback.onOperationCompleted(opTime);
		
	}
	
	private void push(int offset) {
		
		// Invoke opStart callback
		if (pushCallback != null)
			pushCallback.onOperationStarted();
	
		System.out.println("push("+offset+") called.");
		long start = System.currentTimeMillis();
	
		dbClient.addWidthRecords(local);
		
		// Update changeList
		changeList.remove(offset);
		changeList.add(offset, new Boolean(false));

		long opTime = System.currentTimeMillis() - start;
		System.out.println("push("+offset+") completed in "+opTime+"ms.");
		
		// Invoke opCompleted callback
		if (pushCallback != null)
			pushCallback.onOperationCompleted(opTime);
		
	}
	
	public void save() {
		push(currentOffset);
	}

	
/********************
 * Nested Callbacks *
 ********************/
	
	public interface DBOperationCallback {
		
		public void onOperationStarted();
		
		public void onOperationCompleted(final long opTime);
		
	}
	
	public void registerPushCallback(DBOperationCallback callback) {
		pushCallback = callback;
	}
	
	public void registerPullCallback(DBOperationCallback callback) {
		pullCallback = callback;
	}

}