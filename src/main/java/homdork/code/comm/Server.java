package homdork.code.comm;

import java.net.Socket;

public class Server extends Thread {
    Socket client = null;

    public Server(Socket clientSocket) {
        this.client = clientSocket;
    }

    @Override
    public void run() {
        //
    }
}
