package homdork.code.comm;

import homdork.code.security.CryptoHandler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class Server extends Thread {
    Socket client;
    DataOutputStream outputStream;
    DataInputStream inputStream;
    BufferedReader reader;

    public Server(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        this.outputStream = new DataOutputStream(client.getOutputStream());
        this.inputStream = new DataInputStream(client.getInputStream());
        this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    @Override
    public void run() {
        try {
            testCommunication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testCommunication() throws Exception {
        boolean running = true;
        BufferedInputStream bis = new BufferedInputStream(client.getInputStream());
        do {
            try {
                byte[] encryptedMessage = bis.readAllBytes();

                //Decrypt encryptedMessage
                CryptoHandler cryptoHandler = new CryptoHandler();
                String message = cryptoHandler.aesDecrypt(encryptedMessage);

                System.out.println("READ: " + message);
                //Return string
                outputStream.writeBytes("status code: 200-" + message + "\r\n");
                outputStream.flush();

            } catch (SocketException e) {
                running = false;
                System.out.println("[ERROR] Client disconnected.");
            }
        } while (running);

    }
}
