package homdork.code.comm;

import homdork.code.data.SQLHandler;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerMain {
	public static SQLHandler handler = new SQLHandler();
	// map<address,client> of API and all hubs connected to this server.
	static Map<String, ClientModel> clients = new HashMap<>();
	static Logger logger = Logger.getLogger("SERVER_LOG");
	static FileHandler fileHandler;

	static {
		try {
			fileHandler = new FileHandler("server.log", true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ServerMain() {
	}

	public static void main(String[] args) throws IOException {
		logger.addHandler(fileHandler);
		SimpleFormatter formatter = new SimpleFormatter();
		fileHandler.setFormatter(formatter);
		handler.setUp(logger);
		MultiClientServer multiClientServer = new MultiClientServer(handler, logger);
		multiClientServer.start();
	}

	static void addClient(Socket clientSocket) {
		String ip = clientSocket.getInetAddress().toString();
		ClientModel clientModel = new ClientModel(ip, clientSocket);
		clients.put(ip, clientModel);
	}

	public static Map<String, ClientModel> getMap() {
		return clients;
	}
}

