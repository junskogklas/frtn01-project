package src.comm;

import src.Monitor;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextArea;

import com.fazecast.jSerialComm.*;

/** 
	Class containing the visual and functional GUI elements of the program
*/
public class BtGUI {

	// Declaring instance and class variables 
	static SerialPort chosenPort;
	static PrintWriter output;
	static double moveStatus;
	private Monitor mon;

	// Declaring graphical Swing objects
	private JFrame window;
	private JButton connectButton;
	private JButton regulateButton;
	private JButton moveForward;
	private JButton moveBackward;
	private JButton stay;
	private JTextField distance;
	private JTextArea messageBox;
	private JTextArea status;
	private JPanel northPanel;
	private JPanel southPanel;
	private JComboBox<String> portList;

	// Instance variables connected to buttons
	private boolean connected = false;
	private boolean regulOn = false;

	public BtGUI(Monitor monitor){
		moveStatus = 0.0;
		mon = monitor;

		// Initializing window properties
		window = new JFrame();
		window.setTitle("Bluetooth");
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		window.setSize((int)screenSize.getWidth()/2,(int)screenSize.getHeight()/3);
		window.setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// Initizializing and configuring properties and positions of all graphical Swing objects
		portList = new JComboBox<String>();

		connectButton = new JButton("Connect");
		connectButton.setPreferredSize(new Dimension(window.getWidth()/4, window.getHeight()/8));

		moveForward = new JButton("Move Forward");
		moveForward.setPreferredSize(new Dimension(window.getWidth()/4, window.getHeight()/8));

		moveBackward = new JButton("Move Backward");
		moveBackward.setPreferredSize(new Dimension(window.getWidth()/4, window.getHeight()/8));

		stay = new JButton("Stay");
		stay.setPreferredSize(new Dimension(window.getWidth()/4, window.getHeight()/8));

		regulateButton = new JButton("Control");
		regulateButton.setPreferredSize(new Dimension(window.getWidth()/4,window.getHeight()/8));

		messageBox = new JTextArea();
		messageBox.setPreferredSize(new Dimension(window.getWidth()/2 - 10, window.getHeight()/4));

		status = new JTextArea();
		status.setPreferredSize(new Dimension(window.getWidth()/2 - 10, window.getHeight()/4));

		distance = new JTextField();
		distance.setPreferredSize(new Dimension(window.getWidth()/4, window.getHeight()/8));

		northPanel = new JPanel();
		southPanel = new JPanel();

		GroupLayout northLayout = new GroupLayout(northPanel);
   	northPanel.setLayout(northLayout);
		northLayout.setAutoCreateGaps(true);
		northLayout.setAutoCreateContainerGaps(true);

		GroupLayout.SequentialGroup hGroup = northLayout.createSequentialGroup();
		hGroup.addGroup(northLayout.createParallelGroup()
			.addComponent(portList)
			.addComponent(distance)
			.addGroup(northLayout.createSequentialGroup()
				.addComponent(moveForward)
				.addComponent(moveBackward)
				.addComponent(stay)
			)
		);
   	hGroup.addGroup(northLayout.createParallelGroup()
		 	.addComponent(connectButton)
			.addComponent(regulateButton)
		);
   	northLayout.setHorizontalGroup(hGroup);

		GroupLayout.SequentialGroup vGroup = northLayout.createSequentialGroup();
		vGroup.addGroup(northLayout.createParallelGroup(Alignment.BASELINE)
			.addComponent(portList)
			.addComponent(connectButton)
		);
    vGroup.addGroup(northLayout.createParallelGroup(Alignment.BASELINE)
			.addComponent(distance)
			.addComponent(regulateButton)
		);
		vGroup.addGroup(northLayout.createParallelGroup(Alignment.BASELINE)
			.addComponent(moveForward)
			.addComponent(moveBackward)
			.addComponent(stay)
		);
    northLayout.setVerticalGroup(vGroup);

		southPanel.add(messageBox);
		southPanel.add(status);

		window.add(northPanel, BorderLayout.NORTH);
		window.add(southPanel, BorderLayout.SOUTH);
		// Conifguration of properties and positioning of graphical elemts end here 

		// Fetching ports from system
		SerialPort[] portNames = SerialPort.getCommPorts();
		for(int i = 0; i < portNames.length; i++){
			portList.addItem(portNames[i].getSystemPortName());
		}

		// Definition of button functionality begins here
		regulateButton.addActionListener(e->{
			regulOn = !regulOn;
			if (regulateButton.getText().equals("Control")){
				changeStatus("Control of distance enabled");
				regulateButton.setText("Turn off");
				mon.setDesiredDist(Double.parseDouble(distance.getText()));
			} else {
				changeStatus("Control of distance disabled");
				regulateButton.setText("Control");
			}
		});

		connectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				if (connectButton.getText().equals("Connect")) { //Connecting to serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());

					// Important communication parameters that needs to be correctly configured
					chosenPort.setComPortParameters(9600, 16, 1, 0); 
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);

					if(chosenPort.openPort()) {
						connectButton.setText("Disconnect");
						output = new PrintWriter(chosenPort.getOutputStream());
						portList.setEnabled(false);
						connected = true;
						changeStatus("Bluetooth connected");
					}			
				} else { // Disconnecting from serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setText("Connect");
					connected = false;
					changeStatus("Bluetooth disconnected");
				}
			}				
		});

		// These next two buttons make up the manual control of the MinSeg movement, which exists mainly for
		// demonstration and debugging purposes
		//
		// Movement communication protocol is defined as sending a + or - char to the MinSeg, which interprets
		// the corresponding character to an increment or decrement of arbitrary size on the alpha-angle offset
		moveForward.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				output.print('+');
				output.flush();
				moveStatus += 0.1;
				if (moveStatus > 1) {
					moveStatus = 1;
				}
			}
		});
		moveBackward.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				output.print('-'); 
				output.flush();
				moveStatus -= 0.1;
				if (moveStatus < -1) {
					moveStatus = -1;
				}
			}
		});

		// The movement protocol also states that if a 0 char is sent, the offset will be set to the
		// default offset that balances the MinSeg in place
		stay.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				output.print('0'); 
				output.flush();
				moveStatus = '0';
			}
		});
		// Definition of button functionality ends here

		changeStatus("Waiting for bluetooth connection...");
		window.setVisible(true);
	}

	// Method to communicate the status of the program
	public void changeStatus(String announcement) {
		System.out.println(announcement);
		status.setText(announcement);
	}

	// Access methods
	public boolean connected() {
		return connected;
	}
	public SerialPort getPort() {
		return chosenPort;
	}
	public void setMessage(String message) {
		messageBox.setText(message);
	}
	public boolean regulateOn() {
		return regulOn;
	}
	public PrintWriter getOutput() {
		return output;
	}
	public double getMoveStatus() {
		return moveStatus;
	}
	public void setMoveStatus(char newMoveStatus) {
		moveStatus = newMoveStatus;
	}
}

