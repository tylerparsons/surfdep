/*
######################################
DepositionDataManager.java
@author		Tyler Parsons
@created	7 May 2014
 
A class that manages the recording of 
simulation data. Automatically creates 
a file system to store data, outputs 
numerical values and saves images with
file I/O.
######################################
*/
package bdm.largesystems;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.awt.Container;

import org.opensourcephysics.display.DrawingFrame;

import bdm.largesystems.models.LargeSystemDeposition;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Scanner;

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
	
	public DataManager (String idLogPath, String txtPath, String csvPath) {
		idLog = new File(idLogPath);
		txt = new File(txtPath);
		csv = new File(csvPath);
		outputId = readOutputID();
		db = MySQLClient.getSingleton("depositions", "bdm", "d3po$ition$");
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
	
	
/****************
 * Data Writing *
 ****************/
	
	public void saveToTxt(LargeSystemDeposition model, HashMap<String, Double> addlParams) {
		try {
			out = new FileWriter(txt, true);
			String[] packages = model.getClass().getName().split("\\.");
			out.append("\n***************************");
			out.append("\n"+packages[packages.length-1]);
			out.append("\n***************************");
			out.append("\nTrial\t" + outputId);
			
			//Output given Parameters
			HashMap<String, Double> params = model.parameters();
			for (String name: params.keySet())
				out.append("\n" + name + "\t" + model.getParameter(name));
			for (String name: addlParams.keySet())
				out.append("\n" + name + "\t" + addlParams.get(name).doubleValue());
			
			out.append('\n');
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveToCSV(LargeSystemDeposition model, HashMap<String, Double> addlParams) {
		try {
			out = new FileWriter(csv, true);
			out.append(outputId + "\t");
			
			//Output given Parameters
			for (Double d: model.parameters().values())
				out.append(d.doubleValue() + "\t");
			for (Double d: addlParams.values())
				out.append(d.doubleValue() + "\t");
			out.append('\n');
			
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
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
	
	public void saveAll(LargeSystemDeposition model, HashMap<String, Double> addlParams) {
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
	
	public void saveToDB(LargeSystemDeposition model, HashMap<String, Double> addlParams) {
		
		// Add trial param
		addlParams.put("trial", (double)outputId);
		
		// Generate insert statement
		String columns = "";
		String values = "";
		// Parse model params
		for (String key: model.parameters().keySet()) {
			columns += key + ",";
			values += cleanInput(""+model.parameters().get(key)) + ",";
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
	
	public void updateW_avg(LargeSystemDeposition model) {
		
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
	
}
