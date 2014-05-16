/*
######################################
Parameter.java
@author		Tyler Parsons
@created	7 May 2014
 
A class used to wrap numerical values
along with their associated names for
easy access and storage.
######################################
*/
package ch13;

public class Parameter {
	
	public String name;
	public double value;
	public double defaultValue;
	
	public Parameter (String n, double dv) {
		name = n;
		value = dv;
		defaultValue = dv;
	}

}
