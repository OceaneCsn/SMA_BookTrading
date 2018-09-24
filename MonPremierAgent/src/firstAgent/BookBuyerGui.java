package firstAgent;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import jade.core.AID;

public class BookBuyerGui extends JFrame {
	
	private BookBuyerAgent myAgent;
	private JTextField titleField, priceField;
	private JComboBox<String> stateField;

	BookBuyerGui(BookBuyerAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(3, 2));
		p.add(new JLabel("Target book title:"));
		titleField = new JTextField(15);
		p.add(titleField);
		p.add(new JLabel("Maximum price:"));
		priceField = new JTextField(15);
		p.add(priceField);
		p.add(new JLabel("The target book should be at least:"));
		String[] states = {"New","Good","Used","Damaged"};
		stateField = new JComboBox<String>(states);
		p.add(stateField);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Ok");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String title = titleField.getText().trim();
					String price = priceField.getText().trim();
					String state = (String)stateField.getSelectedItem();
					myAgent.updateTarget(title, Integer.parseInt(price),state);
					titleField.setEnabled(false);
					priceField.setEnabled(false);
					stateField.setEnabled(false);
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(BookBuyerGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
		
		// Make the agent terminate when the user closes 
		// the GUI using the button on the upper right corner	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}
