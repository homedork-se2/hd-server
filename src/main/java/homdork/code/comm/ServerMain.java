package homdork.code.comm;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {
	// map<address,client> of API and all hubs connected to this server.
	static Map<String, ClientModel> clients = new HashMap<>();

	public static void main(String[] args) throws IOException {
		MultiClientServer multiClientServer = new MultiClientServer();
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

