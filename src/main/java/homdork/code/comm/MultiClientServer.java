package homdork.code.comm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MultiClientServer extends Thread {
	final int portNumber = 1234;

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNumber);
			System.out.println("[LOG] Server Socket has successfully been created.");
		} catch (IOException e) {
			System.out.println("[ERROR] There was an error when listening at port number: " + portNumber);
			System.exit(-1);
		}

		Socket clientSocket;
		while (true) {
			try {
				// API or any user hub
				clientSocket = serverSocket.accept();

				ServerMain.addClient(clientSocket);

				System.out.println("[INFO] Connection has successfully been established with " + clientSocket.getInetAddress());
				Server server = new Server(clientSocket);
				server.start();
			} catch (IOException e) {
				System.out.println("[ERROR] Connection failed at port number: " + portNumber);
				System.exit(-1);
			}
		}
	}

}

class Client {
	String ipAddress;
	Socket socket = null;

	public Client(String ipAddress, Socket socket) {
		this.ipAddress = ipAddress;
		this.socket = socket;
	}

	public Socket getSocket() {
		return this.socket;
	}
}
