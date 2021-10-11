package homdork.code.comm;

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
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testCommunication() throws IOException {
        boolean running = true;
        BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));

        do {
            try {
                StringBuilder sb = new StringBuilder();
                String readLine = br.readLine();

                System.out.println("Client says: " + readLine);

                // Return reversed string to client
                String reversed = String.valueOf(sb.append(readLine).reverse());
                System.out.println("Sending: " + reversed);
                
                outputStream.writeBytes("status code: 200-" + reversed + "\r\n");
                outputStream.flush();

                if (readLine.equalsIgnoreCase("end")) {
                    client.close();
                    running = false;
                    System.out.println("[SERVER] Exiting...");
                }
            } catch (SocketException e) {
                running = false;
                System.out.println("[ERROR] Client disconnected.");
            }
        } while (running);

    }
}
