package homdork.code.comm;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {
	// map<address,client> of API and all hubs connected to this server.
	static Map<String, Client> clients = new HashMap<>();

	public static void main(String[] args) throws IOException {
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