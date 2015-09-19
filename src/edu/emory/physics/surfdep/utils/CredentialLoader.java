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
import java.util.Scanner;

/**
 * @author Tyler Parsons
 *
 */
public class CredentialLoader {

	/**
	 * Loads credentials from a file having the format
	 * <username>
	 * <password>
	 * into the specified {@link StringBuilder}s.
	 * 
	 * @param credentialsFile 	file path
	 * @param username			StringBuilder for storing username
	 * @param password			StringBuilder for storing password
	 * @throws Exception 		If a valid credentials file could
	 * 							not be read.
	 */
	public static void load(String credentialsFile,
							StringBuilder username,
							StringBuilder password)
		throws Exception {
		
		Scanner scanner = new Scanner(new File(credentialsFile));
		username.append(scanner.nextLine());
		password.append(scanner.nextLine());
		scanner.close();
		
	}
	
}
