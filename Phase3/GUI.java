import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GUI extends JFrame 
{
	private static final int width = 500;
	private static final int height = 300;
	
	// Take as a command-line argument the name of the .torrent file to be loaded and the name of the file to save the data to. For example:
    //java my.package.RUBTClient somefile.torrent somepicture.jpg
	
	private JLabel inputf;
	private JTextField inputflength;
	private JButton okB, exitB;
	
	//Button handlers
	//private CalculateButtonHandler confirm;
	private ExitButtonHandler exitBHandler;
	
	public GUI()
	{
			//Instantiating labels
			inputf = new JLabel("Enter the command argument for the input file: ", SwingConstants.LEFT);
			
			//Text fields
			inputflength = new JTextField(100);
			
			//Buttons
			/*okB = new JButton("Confirm");
			confirm = new CalculateButtonHandler(); //to confirm inputted file
			okB.addActionListener(confirm);*/
			exitB = new JButton("Exit");
			exitBHandler = new ExitButtonHandler();
			exitB.addActionListener(exitBHandler);
			
			//Window's Title
			setTitle("BitTorrent Client");
			
			//Content pane
			Container pane = getContentPane();
			pane.setLayout(new GridLayout(8,2));
			
			//Add things to the pane in the order of appearance, L->R, T->B
			pane.add(inputf);
			pane.add(exitB);
			
			setSize(width, height);
			setVisible(true);
			setDefaultCloseOperation(EXIT_ON_CLOSE);
		
	}
	
	public class ExitButtonHandler implements ActionListener
	{
			public void actionPerformed(ActionEvent e){
				System.exit(0);
			}
	}
	
	public static void main(String[] args)
	{
			GUI openclient = new GUI();
	}
	
}
