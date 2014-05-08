package ch13;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import java.awt.Container;
import org.opensourcephysics.display.DrawingFrame;

import java.util.Scanner;
import java.util.ArrayList;
import ch13.Parameter;

public class DepositionDataManager {

	private File txt;
	private File csv;
	private File idLog;
	
	private Scanner in;
	private FileWriter out;
	
	private int output_id;
	
	final static String baseDir = "C:\\Users\\Tyler\\Documents\\Classes\\CurrentClasses\\PHYS436\\workspace\\csm\\data\\";
	
	public DepositionDataManager (String idLogPath, String txtPath, String csvPath) {
		idLog = new File(idLogPath);
		txt = new File(txtPath);
		csv = new File(csvPath);
		output_id = readOutputID();
	}
	
/*****************
 * Log Functions *
 *****************/	

	/*
	 * Creates a directory structure to store data
	 * for the current trial based on output_id.
	 * */
	public void startTrial() {
		
		incrementOutputID();
		//Outer Trial Directory
		File trialDir = new File(baseDir+"\\trial"+output_id);
		trialDir.mkdir();
		//Inner Directories for Data Storage
		File latticeDir = new File(baseDir+"\\trial"+output_id+"\\lattices");
		latticeDir.mkdir();
		File plotDir = new File(baseDir+"\\trial"+output_id+"\\plots");
		plotDir.mkdir();
	}
	
	// id is determined externally from log file
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
		output_id = readOutputID();
		try {
			out = new FileWriter(idLog);
			out.write(""+(++output_id));
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
/****************
 * Data Writing *
 ****************/
	
	public void saveToTxt(Deposition model, ArrayList<Parameter> addlParams) {
		try {
			out = new FileWriter(txt, true);
			String[] packages = model.getClass().getName().split("\\.");
			out.append("\n***************************");
			out.append("\n"+packages[packages.length-1]);
			out.append("\n***************************");
			out.append("\nTrial\t" + output_id);
			
			//Output given Parameters
			for (Parameter p: model.parameters())
				out.append("\n" + p.name + "\t" + p.value);
			for (Parameter p: addlParams)
				out.append("\n" + p.name + "\t" + p.value);
			
			out.append('\n');
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveToCSV(Deposition model, ArrayList<Parameter> addlParams) {
		try {
			out = new FileWriter(csv, true);
			out.append(output_id + "\t");
			
			//Output given Parameters
			for (Parameter p: model.parameters())
				out.append(p.value + "\t");
			for (Parameter p: addlParams)
				out.append(p.value + "\t");
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
		String imgPath = baseDir+"trial"+output_id+"\\"+directory+"\\"+name;
		try {
			ImageIO.write(image, "jpeg", new File(imgPath));
		} catch(IOException e) {
			e.printStackTrace();
		}
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
	
	
}
