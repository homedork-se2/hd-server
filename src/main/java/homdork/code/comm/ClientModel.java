package homdork.code.comm;

import java.net.Socket;

public class ClientModel {
	String ipAddress;
	Socket socket = null;

	public ClientModel(String ipAddress, Socket socket) {
		this.ipAddress = ipAddress;
		this.socket = socket;
	}

	public Socket getSocket() {
		return this.socket;
	}
}
