package src.comm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.fazecast.jSerialComm.*;
import src.Monitor;

/** 
	Class containing thread that establishes the continous communication of the distance control
	signal that is calculated in the monitor, based on input from the webcam
*/
public class BtComm implements Runnable {

	// Declaring instance and class variables
	static SerialPort chosenPort;
	private BtGUI gui;
	private Monitor mon;

	public BtComm(BtGUI gui, Monitor mon){
		this.gui = gui;
		this.mon = mon;
	}

	public void run() {
		while (true){

			// If the user is connected to the MinSeg and has pressed the distance control button, the
			// thread should be sending the distance control output based on webcam data to the MinSeg
			if (gui.connected() && gui.regulateOn()) {
				chosenPort = gui.getPort();
				char sendChar = mon.calculateOutPut();
				System.out.println(sendChar);
				gui.getOutput().print(sendChar);
				gui.getOutput().flush();
				gui.setMessage("Distance: " + Double.toString(mon.getDist()) + "cm,\nDesired Distance: " 
											+ Double.toString(mon.getDesiredDist()) 
											+ "\nSignal to MinSeg: " + sendChar);
			} 
			
			// If the user has not pressed the control button or is not connected, the messageBox output
			// should simply be printed and no MinSeg communication be done
			else {
				double sendChar = gui.getMoveStatus();
				gui.setMessage("Distance: " + Double.toString(mon.getDist()) + "cm,\nDesired Distance: " 
										+ Double.toString(mon.getDesiredDist()) 
										+ "\nSignal to MinSeg: " + Double.toString(sendChar));
			}
			try {
				Thread.sleep(100);
			} catch (Exception e) {}
	}
}
}

