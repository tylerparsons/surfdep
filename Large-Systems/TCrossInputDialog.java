package ch13;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class TCrossInputDialog extends JFrame {
	
	/**
	 * Added to appease eclipse.
	 */
	private static final long serialVersionUID = 4560725703376287782L;
	
	private JButton analyzeButton;
	private JTextArea t0;
	private JTextArea tx1;
	private JTextArea tx2;
	
	public final static String KEY_T_0 = "t_0";
	public final static String KEY_T_CROSS1 = "t_cross1";
	public final static String KEY_T_CROSS2 = "t_cross2";
	
	public interface InputHandler {
		
		public void onInputReceived(HashMap<String, Double> input);
		
	}
	
	private InputHandler mHandler;

	public TCrossInputDialog(InputHandler handler) {
		mHandler = handler;
		init();
	}
	
	private void init() {
		
		JPanel panel = new JPanel();
		
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		
		// Setup border
		panel.setBorder(BorderFactory.createTitledBorder(
			"Input t_cross values"
		));
		
		// Labels
		c.gridx = 0;
		c.weightx = 0.25;
		
		c.gridy = 0;
		panel.add(new JLabel("ln(t_0)"), c);
		c.gridy = 1;
		panel.add(new JLabel("ln(t_cross1)"), c);
		c.gridy = 2;
		panel.add(new JLabel("ln(t_cross2)"), c);
		
		
		// Input Fields
		c.gridx = 1;
		c.weightx = 0.75;
		
		c.gridy = 0;
		t0 = new JTextArea();
		t0.setEditable(true);
		panel.add(t0, c);
		c.gridy = 1;
		tx1 = new JTextArea();
		tx1.setEditable(true);
		panel.add(tx1, c);
		c.gridy = 2;
		tx2 = new JTextArea();
		tx2.setEditable(true);
		panel.add(tx2, c);
		
		
		// Analyze Button
		analyzeButton = new JButton("Analyze Model");
		analyzeButton.addActionListener(new ActionListener() {
			/*
			 * Calls analyzeModel, parsing input from TextAreas
			 * */
			@Override
			public void actionPerformed(ActionEvent e) {
				
				HashMap<String, Double> input = new HashMap<String, Double>();
				
				input.put(KEY_T_0, (t0.getText().trim() == "") ? 0 :
					Double.parseDouble(t0.getText().trim()));
				input.put(KEY_T_CROSS1, (tx1.getText().trim() == "") ? 0 :
					Double.parseDouble(tx1.getText().trim()));
				input.put(KEY_T_CROSS2, (tx2.getText().trim() == "") ? 0 :
					Double.parseDouble(tx2.getText().trim()));
				
				// Dispose of window, no longer needed
				dispose();
				
				
				// Invoke InputHandler callback
				mHandler.onInputReceived(input);
			}
		});
		c.gridx = 1;
		c.gridy = 3;
		panel.add(analyzeButton, c);
		
		add(panel);
		
		// Size and center in screen
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		setSize(screen.width/4, screen.height/4);
		setLocationRelativeTo(null);
		
		setVisible(true);
		
	}
	
}
