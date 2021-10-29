package homdork.code.comm;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MultiClientServer extends Thread {
	final int portNumber = 1234;
	Logger logger = Logger.getLogger("SERVER_LOG");
	FileHandler fileHandler = new FileHandler("server.log", true);

	public MultiClientServer() throws IOException {
	}

	@Override
	public void run() {
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(portNumber);
			logger.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);
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
				Server server = new Server(clientSocket);
				server.start();
			} catch (IOException e) {
				logger.log(Level.SEVERE, e.getMessage());
				System.exit(-1);
			}
		}
	}
}


