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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class InputDialog extends JFrame {
	
	/**
	 * Added to appease eclipse.
	 */
	private static final long serialVersionUID = 4560725703376287782L;
	
	private JButton enter;
	private JTextField[] inputs;
	private String[] keys;
	private Consumer<HashMap<String, String>> inputHandler;
	
/******************
 * Initialization *
 ******************/

	public InputDialog(
		String title, String[] fieldKeys,
		Consumer<HashMap<String, String>> handler
	) {
		inputHandler = handler;
		keys = fieldKeys;
		inputs = new JTextField[keys.length];
		init(title);
	}
	
	private void init(String title) {
		
		JPanel panel = new JPanel();
		
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		
		// Setup border
		panel.setBorder(BorderFactory.createTitledBorder(title));
		
		// Labels
		c.gridx = 0;
		c.weightx = 0.25;
		c.gridy = 0;
		
		for (String key: keys) {
			panel.add(new JLabel(key), c);
			c.gridy++;
		}
		
		// Input Fields
		c.gridx = 1;
		c.weightx = 0.75;
		c.gridy = 0;
		
		// Add enter key listener
		KeyListener kl = enterKeyListener();
		
		for (int i = 0; i < inputs.length; i++) {
			inputs[i] = new JTextField();
			inputs[i].setEditable(true);
			inputs[i].addKeyListener(kl);
			panel.add(inputs[i], c);
			c.gridy++;
		}		
		
		// Enter Button
		enter = new JButton("Enter");
		enter.addActionListener(inputEntryListener());
		c.gridx = 1;
		c.gridy = keys.length;
		panel.add(enter, c);
		
		add(panel);
		
		// Size and center in screen
		sizeAndCenter();
		
		setVisible(true);
		
	}
	
	private void sizeAndCenter() {
		
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screen.width/4, Math.max(3, keys.length + 1) * screen.height/16);
		setLocationRelativeTo(null);
		
	}
	
/********************
 * Action Listeners *
 ********************/
	
	private void processInput() {
		
		HashMap<String, String> input = new HashMap<String, String>();
		
		for (int i = 0; i < keys.length; i++) {
			input.put(keys[i], inputs[i].getText());
		}
		
		// Dispose of window, no longer needed
		dispose();
		
		// Invoke InputHandler callback
		inputHandler.accept(input);
		
	}
	
	/**
	 * Calls analyzeModel, parsing input from TextAreas
	 */
	private ActionListener inputEntryListener() {
		
		return (ActionEvent e) -> {
			processInput();
		};
		
	}
	
	private KeyListener enterKeyListener() {
		
		return new KeyListener() {

			@Override
			public void keyPressed(KeyEvent ke) {
				if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
					processInput();
				}				
			}

			@Override
			public void keyReleased(KeyEvent ke) {}

			@Override
			public void keyTyped(KeyEvent ke) {}
			
		};
		
	}
	
}
