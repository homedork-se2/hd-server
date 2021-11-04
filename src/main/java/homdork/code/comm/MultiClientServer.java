package homdork.code.comm;

import homdork.code.data.SQLConnector;
import homdork.code.data.SQLHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MultiClientServer extends Thread {
	final int portNumber = 1234;
	Logger logger;
	SQLHandler handler = new SQLHandler();


	public MultiClientServer(SQLHandler handler, Logger logger) throws IOException {
		this.handler = handler;
		this.logger = logger;
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNumber);
			logger.log(Level.INFO,"SERVER SOCKET CREATED");
		} catch (IOException e) {
			logger.log(Level.SEVERE, e.getMessage());
			System.exit(-1);
		}

		Socket clientSocket;
		while (true) {
			try {
				// API or any user hub
				clientSocket = serverSocket.accept();
				ServerMain.addClient(clientSocket);
				logger.log(Level.INFO, "CLIENT SOCKET CONNECTION ESTABLISHED WITH ADDRESS: " + clientSocket.getInetAddress().toString());
				Server server = new Server(clientSocket, handler);
				server.start();
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage());
				System.exit(-1);
			}
		}
	}
}


