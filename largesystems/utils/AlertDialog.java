package surfdep.largesystems.utils;

import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JLabel;

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
