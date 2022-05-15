package src;

import src.comm.Server;
import src.comm.BtComm;
import src.comm.BtGUI;
import src.Monitor;

/** 
	Class to launch the program and connect all elements defined
*/
class Main {
	public static void main(String[] args) {
		Monitor mon = new Monitor();
		BtGUI gui = new BtGUI(mon);

		BtComm bt = new BtComm(gui, mon);
		Thread btThread = new Thread(bt);
		
		Server server = new Server(mon); 
		Thread serverThread = new Thread(server);

		// Arbitrary priorities used
		btThread.setPriority(8);
		serverThread.setPriority(9);	
		serverThread.start();
		btThread.start();
	}
}

