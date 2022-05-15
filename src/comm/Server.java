package src.comm;

import java.net.Socket;
import java.net.ServerSocket;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import src.Monitor;

/**
	Class that implements the Server that established the socket communication 
	with the python webcam client
*/
public class Server implements Runnable{

	private ServerSocket server;
	private DataOutputStream out;
	private BufferedReader in;
	private Socket client;
	private Monitor mon;

	private boolean status = false;
	// ------------------------------ Remove section if remove delim
	private String endTag;

	/*
	 * Creates a server object.
	 * If endTag is desired explicitly, this constructor can be used to choose one
	 */
	public Server(String endTag, Monitor mon) {
		this.endTag = endTag;
		this.mon = mon;
	}

	/*
	 * Creates a server object.
	 */
	public Server(Monitor mon) {
		this("",mon);
	}
	// ------------------------------ Remove section if remove delim

	/*
	 * Connects the server to the client
	 * Creates and saves reader and writer classes.
	 * Args:
	 *      port: port on which to open socket
	 * return: boolean - Describing whether initialization was successful or not
	 */
	public boolean connect(int port) {
		try {
			// Create a socket opening for a specific port
			server = new ServerSocket(port);
			// Wait for another socket (client) to connect to the port
			// NOTE: This call will wait for connection (it freezes here unless you connect to it)
			// This could be fixed easily but I don't think you'll need it for these projects.
			client = server.accept();
			// Connect to the connected sockets out_stream
			out = new DataOutputStream(client.getOutputStream());
			// Connect to the connected sockets in_stream
			in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			status = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return status;
	}

	/*
	 * Sends message over the connected sockets out-stream
	 */
	public void send(String msg) throws IOException {
		if (!status) {
			System.out.println("Client not connected"); 
			return;
		}

		// Handle exceptions
		try {
			out.writeBytes(msg + endTag + "\n"); 
		} catch (IOException e) {
			throw e;
		}
	}

	/*
	 * Receives message over the connected sockets out-stream
	 */
	public String receive() {
		if (!status) {
			System.out.println("Client not connected"); 
			return "";
		}

		try {
			return in.readLine();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}

	/*
	 * Closes the socket connection.
	 * This has to be done so we don't leave a socket open in our computer system when program shuts down or crashes
	 */
	public void close() {
		try {
			// Close our socket and let client worry about communication
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	 * Test method to show how to run the server
	 */
	public void run() {
		// NOTE: Port number has to be bigger than 1024
		// Due to the first 1024 being reserved for system level communication (Ethernet, USB, internal, etc.)
		System.out.println("Waiting for client connection...");
		connect(4444);
		System.out.println("Connection to client established");
		while(true) {
			try {
				String dist = receive();
				send("Ok");
				mon.setDist(Double.parseDouble(dist));
				Thread.sleep(90);
			} catch(InterruptedException e){
				close();
			} catch (IOException e) {
				System.out.println("Connection closed");
				System.out.println("Exiting");
				close();
				System.exit(0);
				e.printStackTrace();
			}
		}
	}
}

