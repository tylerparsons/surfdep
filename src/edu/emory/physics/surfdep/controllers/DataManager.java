package edu.emory.physics.surfdep.controllers;

import edu.emory.physics.surfdep.models.Deposition;
import edu.emory.physics.surfdep.utils.MySQLClient;

import java.awt.image.BufferedImage;
import java.awt.Container;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;

import org.opensourcephysics.display.DrawingFrame;

/**
 * DepositionDataManager.java
 * Created:	7 May 2014
 * @author	Tyler Parsons
 *  
 * A class that manages the recording of 
 * simulation data. Automatically creates 
 * a file system to store data, outputs 
 * numerical values and saves images with
 * file I/O. Also saves data to MySQL db.
 */
public class DataManager {

	private File txt;
	private File csv;
	private File idLog;
	
	private Scanner in;
	private FileWriter out;
	
	private int outputId;
	
	final static String baseDir = "data\\";
	
	private MySQLClient db;
	
	private final static String DB_TABLE_MODELS = "models";
	private final static String DB_TABLE_AVERAGES = "averages";
	private final static String DB_TABLE_SCALED_AVERAGES = "scaled_averages";
	private final static String DB_TABLE_LOG_AVERAGES = "logarithmic_averages";
	
	public DataManager(String txtPath) {
		txt = new File(txtPath);
	}
	
	public DataManager(String txtPath, String csvPath) {
		this(txtPath);
		csv = new File(csvPath);
	}
	
	public DataManager(String txtPath, String csvPath, MySQLClient db) {
		this(txtPath, csvPath);
		this.db = db;
	}
	
	public DataManager(String idLogPath, String txtPath, String csvPath) {
		this(txtPath, csvPath, MySQLClient.getSingleton("depositions", "bdm", "d3po$ition$"));
		idLog = new File(idLogPath);
		outputId = readOutputID();
	}
	
/*****************
 * Log Functions *
 *****************/	

	/**
	 * Creates a directory structure to store data
	 * for the current trial based on outputId.
	 */
	public void startTrial() {
		
		incrementOutputID();
		//Outer Trial Directory
		File trialDir = new File(baseDir+"\\trial"+outputId);
		trialDir.mkdir();
		//Inner Directories for Data Storage
		File latticeDir = new File(baseDir+"\\trial"+outputId+"\\lattices");
		latticeDir.mkdir();
		File plotDir = new File(baseDir+"\\trial"+outputId+"\\plots");
		plotDir.mkdir();
	}
	
	/**
	 * id is determined externally from log file
	 * @return outputId
	 */
	public int readOutputID() {
		try {
			in = new Scanner(idLog);
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
		String idLogText = in.nextLine();
		in.close();
		if (idLogText == "")
			return 0;
		return Integer.parseInt(idLogText);
	}
	
	// write updated id to log file
	public void incrementOutputID() {
		outputId = readOutputID();
		try {
			out = new FileWriter(idLog);
			out.write(""+(++outputId));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
/******************
 * Nested Classes *
 ******************/
	
	protected interface Printer {
		
		public void print() throws IOException;
		
	}

	protected interface Stringifier {
		
		public <T> String paramToString(String key, T value);
		
	}
	
	
/****************
 * Data Writing *
 ****************/
	
	protected void printSafely(File f, Printer p) {
		try {
			out = new FileWriter(f, true);
			p.print();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public <T> String txtParamToString(String key, T value) {
		return "\n" + key + "\t" + value;
	}
	
	public <T> String csvParamToString(String key, T value) {
		return value + "\t";
	}

	@SafeVarargs
	public final <T> String getOutput(Stringifier s, HashMap<String, T> ... paramMaps) {
		StringBuilder builder = new StringBuilder();
		for(HashMap<String, T> params: paramMaps)
			for (String key: params.keySet())
				builder.append(s.paramToString(key, params.get(key)));
		return builder.toString();
	}
	
	@SafeVarargs
	public final <T> String getTxtOutput(HashMap<String, T> ... paramMaps) {
		return getOutput(new Stringifier() {
			@Override
			@SuppressWarnings("hiding")
			public <T> String paramToString(String key, T value) {
				return txtParamToString(key, value);
			}
		}, paramMaps);
	}
	
	@SafeVarargs
	public final <T> String getCSVOutput(HashMap<String, T> ... paramMaps) {
		return getOutput(new Stringifier() {
			@Override
			@SuppressWarnings("hiding")
			public <T> String paramToString(String key, T value) {
				return csvParamToString(key, value);
			}
		}, paramMaps);
	}
	
	public void saveToTxt(Deposition model, HashMap<String, Double> addlParams) {
		printSafely(txt, new Printer() {
			@Override
			public void print() throws IOException {
				out = new FileWriter(txt, true);
				String[] packages = model.getClass().getName().split("\\.");
				out.append("\n***************************");
				out.append("\n"+packages[packages.length-1]);
				out.append("\n***************************");
				out.append("\nTrial\t" + outputId);
				
				// Print parameters
				out.append(getTxtOutput(model.parameters(), addlParams));
				
				out.append('\n');
				out.close();
			}
		});
	}
	
	@SafeVarargs
	public final <T> void saveToTxt(HashMap<String, T> ... paramMaps) {
		printSafely(txt, new Printer() {
			@Override
			public void print() throws IOException {
				// Print parameters
				out.append(getTxtOutput(paramMaps)+"\n");
			}
		});
	}
	
	public void saveToCSV(Deposition model, HashMap<String, Double> addlParams) {
		printSafely(csv, new Printer() {
			@Override
			public void print() throws IOException {
				// Output given Parameters
				out.append(getCSVOutput(model.parameters(), addlParams));
				out.append('\n');
			}
		});
	}
	
	public void saveToCSV(String csvPath, HashMap<String, Double> params) {
		printSafely(new File("csvPath"), new Printer() {
			@Override
			public void print() throws IOException {
				// Output given Parameters
				out.append(getCSVOutput(params));
				out.append('\n');
			}
		});
	}
	
	public void saveImage(DrawingFrame frame, String directory, String name) {

		//Save content to a BufferedImage
		Container content = frame.getContentPane();
		BufferedImage image = new BufferedImage(content.getWidth(),
												content.getHeight(),
												BufferedImage.TYPE_INT_RGB);
		content.paint(image.getGraphics());
		
		//Print Content to correct folder
		String imgPath = baseDir+"trial"+outputId+"\\"+directory+"\\"+name;
		try {
			ImageIO.write(image, "jpeg", new File(imgPath));
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveAll(Deposition model, HashMap<String, Double> addlParams) {
		saveToTxt(model, addlParams);
		saveToCSV(model, addlParams);
		saveToDB(model, addlParams);
	}
	
/*******************
 * Print Utilities *
 *******************/
	
	public void printToCSV(String output) {
		try {
			out = new FileWriter(csv, true);
			out.append(output + "\t");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printToCSV(String csvPath, String output) {
		csv = new File(csvPath);
		printToCSV(output);
	}
	
	public void printToTxt(String output) {
		try {
			out = new FileWriter(txt, true);
			out.append(output);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
/*********************
 * MySQL Integration *
 *********************/
	
	/**
	 * Input strings to be converted to SQL NULL type
	 */
	private String[] nullValues = {
			"NaN",
			"",
			"Infinity",
			"-Infinity"
	};
	
	/**
	 * Fields to ignore while saving model to db
	 */
	private String[] ignoredModelFields = {
			"A"
	};
	
	/**
	 * Cleans inputs to protect from a variety
	 * of syntax errors
	 * @param raw	A potentially unclean string
	 * @return	A string that will not produce
	 * 			syntax errors by itself
	 */
	public String cleanInput(String raw) {
		
		// Replace inputs with null values where applicable
		for (String n: nullValues)
			if (raw.equals(n))
				return "NULL";		
		
		// raw is clean!
		return raw;
	}
	
	public void saveToDB(Deposition model, HashMap<String, Double> addlParams) {
		
		// Add trial param
		addlParams.put("trial", (double)outputId);
		HashMap<String, Double> modelParams = new HashMap<>(model.parameters());
		
		// Generate insert statement
		String columns = "";
		String values = "";
		// Parse model params
		for (String ignoredKey: ignoredModelFields)
			modelParams.remove(ignoredKey);
		for (String key: modelParams.keySet()) {
			columns += key + ",";
			values += cleanInput(""+modelParams.get(key)) + ",";
		}
		// Parse addlParams
		for (String key: addlParams.keySet()) {
			columns += key + ",";
			values += cleanInput(""+addlParams.get(key)) + ",";
		}
		
		db.exec(
			"INSERT INTO " + DB_TABLE_MODELS +
			" (" + columns.substring(0, columns.length() - 1) +
			") VALUES (" + values.substring(0, values.length() - 1) + ")"
		);
		
	}
	
	public void updateW_avg(Deposition model) {
		
		// Query current average value
		
		String whereClause = 
			"t=" + model.getTime() + " AND " +
			"L=" + model.getLength() + " AND " +
			"x=" + model.getParameter("x") + " AND " +
			"p_diff=" + model.getParameter("p_diff") + " AND " +
			"l_0=" + model.getParameter("l_0");
		
		ResultSet results = db.query(
			"SELECT w_avg, S FROM " + DB_TABLE_AVERAGES +
			" WHERE " + whereClause
		);
		
		try {
				
			// Insert new record if none exists
			if(results == null || !results.first()) {
				
				String columns = "(t,w_avg,L,x,p_diff,l_0,S)";
				
				String values = 
					model.getTime() + "," +
					model.getWidth(model.getTime()) + "," +
					model.getLength() + "," +
					model.getParameter("x") + "," +
					model.getParameter("p_diff") + "," +
					model.getParameter("l_0") + 
					",1";	// Number of samples
				
				db.exec(
					"INSERT INTO " + DB_TABLE_AVERAGES +
					" " + columns + " VALUES (" +
					values + ")"
				);
				
			}
			// Update existing record
			else {
				
				// Obtain w_avg, samples from results
				double w_avg = results.getDouble("w_avg");
				int S = results.getInt("S");
				
				// Calculate running average
				w_avg = (w_avg*S + model.getWidth(model.getTime()))/(S+1);
				
				String assignments = 
					"w_avg=" + w_avg +
					", S=" + (S+1);
				
				db.exec(
					"UPDATE " + DB_TABLE_AVERAGES +
					" SET " + assignments +
					" WHERE " + whereClause
				);
				
			}
			
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		
	}
	
	public void updateScaledW_avg(Deposition model, double w_avg, int S) {
		
		// Query current average value
		
		String whereClause = 
			"h_avg=" + model.getScaledTime() + " AND " +
			"L=" + model.getLength() + " AND " +
			"x=" + model.getParameter("x") + " AND " +
			"p_diff=" + model.getParameter("p_diff") + " AND " +
			"l_0=" + model.getParameter("l_0");
		
		ResultSet results = db.query(
			"SELECT w_avg, S FROM " + DB_TABLE_SCALED_AVERAGES +
			" WHERE " + whereClause
		);
		
		try {
				
			// Insert new record if none exists
			if(results == null || !results.first()) {
				
				String columns = "(h_avg,w_avg,L,x,p_diff,l_0,S)";
				
				String values = 
					model.getScaledTime() + "," +
					w_avg + "," +
					model.getLength() + "," +
					model.getParameter("x") + "," +
					model.getParameter("p_diff") + "," +
					model.getParameter("l_0") + 
					","+S;	// Number of samples
				
				db.exec(
					"INSERT INTO " + DB_TABLE_SCALED_AVERAGES +
					" " + columns + " VALUES (" +
					values + ")"
				);
				
			}
			// Update existing record
			else {
				
				// Obtain w_avg, samples from results
				double w_avg0 = results.getDouble("w_avg");
				int S0 = results.getInt("S");
				
				// Calculate running average
				w_avg = (w_avg0*S0 + model.getWidth(model.getScaledTime()))/(S0+S);
				
				String assignments = 
					"w_avg=" + w_avg +
					", S=" + (S0+S);
				
				db.exec(
					"UPDATE " + DB_TABLE_SCALED_AVERAGES +
					" SET " + assignments +
					" WHERE " + whereClause
				);
				
			}
			
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		
	}
	
	public void updateScaledW_avg(Deposition model) {
		updateScaledW_avg(model, model.getWidth(model.getScaledTime()), 1);
	}

	public void updateAverages(Deposition model) {

		// Look for current average value
		String whereClause = 
			"t=" + model.getTime() + " AND " +
			"L=" + model.getLength() + " AND " +
			"x=" + model.getParameter("x") + " AND " +
			"p_diff=" + model.getParameter("p_diff") + " AND " +
			"l_0=" + model.getParameter("l_0");
		
		ResultSet results = db.query(
			"SELECT w_avg, h_avg, S FROM " + DB_TABLE_LOG_AVERAGES +
			" WHERE " + whereClause
		);
		
		try {
				
			// Insert new record if none exists
			if(results == null || !results.first()) {
				
				String columns = "(t,w_avg,h_avg,L,x,p_diff,l_0,S,A)";
				
				String values = 
					model.getTime() + "," +
					model.getWidth(model.getTime()) + "," +
					model.getAverageHeight() + "," +
					model.getLength() + "," +
					model.getParameter("x") + "," +
					model.getParameter("p_diff") + "," +
					model.getParameter("l_0") + 
					",1" +	// Number of samples
					"," + model.getParameter("A");
				
				db.exec(
					"INSERT INTO " + DB_TABLE_LOG_AVERAGES +
					" " + columns + " VALUES (" +
					values + ")"
				);
				
			}
			// Update existing record
			else {
				
				// Obtain w_avg, samples from results
				double w_avg = results.getDouble("w_avg");
				double h_avg = results.getDouble("h_avg");
				int S = results.getInt("S");
				double A = model.getParameter("A");
				
				// Calculate running average
				w_avg = (w_avg*S + model.getWidth(model.getTime()))/(S+1);
				h_avg = (h_avg*S + model.getAverageHeight())/(S+1);
				
				String assignments = 
					"w_avg=" + w_avg + "," +
					"h_avg=" + h_avg + "," +
					"S=" 	 + (S+1) + "," +
					"A="     + A;
				
				db.exec(
					"UPDATE " + DB_TABLE_LOG_AVERAGES +
					" SET " + assignments +
					" WHERE " + whereClause
				);
				
			}
			
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		
	}
	
}
