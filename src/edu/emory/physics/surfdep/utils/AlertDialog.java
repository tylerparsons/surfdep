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

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * @deprecated Use JOPtionPane instead
 * @author Tyler Parsons
 */
public class AlertDialog extends JFrame {

	/**
	 * Added to appease eclipse.
	 */
	private static final long serialVersionUID = -263256770778925904L;
	
	private JLabel message;
	
	
	public AlertDialog(String title, String messageText) {
		super(title);
		init(messageText);
	}
	
	private void init(String messageText) {
		
		// Display Message
		message = new JLabel(messageText);
		add(message);
		
		// Size and Center in screen
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screen.width/4, screen.height/4);
		setLocationRelativeTo(null);
		setVisible(true);
		
	}
	
	public void showMessage(String msg) {
		message.setText(msg);
		add(message);
	}
	
}
