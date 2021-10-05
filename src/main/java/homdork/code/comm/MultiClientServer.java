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
            System.out.println("[LOG] Server Socket has been created.");
        } catch (IOException e) {
            System.out.println("[ERROR] There was an error when listening at port number: " + portNumber);
            System.exit(-1);
        }

        Socket clientSocket = null;
        while (true) {
            try {
                clientSocket = serverSocket.accept();
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
