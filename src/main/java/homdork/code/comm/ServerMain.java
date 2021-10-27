package homdork.code.comm;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {

	static Map<String, Client> clients = new HashMap<>();

	public static void main(String[] args) {
		MultiClientServer multiClientServer = new MultiClientServer();
		multiClientServer.start();
	}

	static void addClient(Socket clientSocket) {
		String ip = clientSocket.getInetAddress().toString();
		Client client = new Client(ip, clientSocket);
		clients.put(ip, client);
	}

	public static Map<String, Client> getMap() {
		return clients;
	}
}
