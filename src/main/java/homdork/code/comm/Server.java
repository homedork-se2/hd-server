package homdork.code.comm;

import com.google.gson.Gson;
import homdork.code.data.SQLHandler;
import homdork.code.model.User;
import homdork.code.security.CryptoHandler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;

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
        BufferedReader bis = new BufferedReader(new InputStreamReader(client.getInputStream()));
        CryptoHandler cryptoHandler = new CryptoHandler();
        SQLHandler sqlHandler = new SQLHandler();

        do {
            try {
                //Read all bytes from the message
                String encryptedMessage = bis.readLine();
                byte[] byteArray = encryptedMessage.getBytes(StandardCharsets.UTF_8);

                //Decrypt encryptedMessage and print it
                cryptoHandler.setUpCipher();
                String message = cryptoHandler.aesDecrypt(byteArray);
                System.out.println("[DECRYPTED/READ]: " + message);

                //Perform query
                sqlHandler.setUp();

                //Parse the query, check if insert, select etc.
                if (message.contains("INSERT") && message.contains("users")) {
                    System.out.println("[LOG] Entered insert handler.");
                    //Update handler to update the table, because were inserting
                    sqlHandler.updateHandler(message);

                    //Get the UUID
                    StringBuilder builder = new StringBuilder();
                    boolean isParenthesis = false;
                    for (char c : message.toCharArray()) {
                        if (c == '\'' || isParenthesis) {
                            if (isParenthesis) {
                                if (c != '\'') {
                                    builder.append(c);
                                }
                            }
                            isParenthesis = true;
                        }
                    }
                    String[] parts = builder.toString().split(",");
                    String userID = parts[0];
                    System.out.println("USERID: " + userID);

                    //Select user with the UUID
                    ResultSet resultSet = sqlHandler.selectUsersWhereUUD(userID);
                    if (resultSet.next()) {
                        String uuid = resultSet.getString("uuid");
                        String name = resultSet.getString("name");
                        String email = resultSet.getString("email");

                        //make user model, use json to make an object based on that above ^
                        User user = new User(name, email, uuid);
                        Gson gson = new Gson();
                        String json = gson.toJson(user, User.class);
                        System.out.println("UUID: " + uuid);
                        System.out.println("JSON: " + json);
                        outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
                        outputStream.flush();
                    }


                } else if (message.contains("UPDATE") && message.contains("users")) {

                } else if (message.contains("SELECT") && message.contains("users")) {

                    StringBuilder builder = new StringBuilder();
                    boolean ifPar = false;
                    for (char c : message.toCharArray()) {
                        if (c == '\'' || ifPar) {
                            ifPar = true;
                            builder.append(c);
                        }
                    }
                    String[] parts = builder.toString().split(",");
                    String userID = parts[0];
                    System.out.println("USERID: " + userID);
                    ResultSet resultSet = sqlHandler.selectUsersWhereUUD(userID);

                } else if (message.contains("SELECT") && message.contains("devices")) {

                } else if (message.contains("UPDATE") && message.contains("devices")) {

                } else if (message.contains("INSERT") && message.contains("devices")) {
                    //insert new device, get uuid of device

                    StringBuilder builder = new StringBuilder();
                    for (char c : message.toCharArray()) {
                        if (c == '(') {
                            builder.append(c);
                        }
                    }
                    String[] parts = builder.toString().split(",");
                    String deviceID = parts[0];

                }

                //Send return code & string of encrypted message to the client
                outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(message) + "\r\n");
                outputStream.flush();

            } catch (SocketException e) {
                running = false;
                System.out.println("[ERROR] Client disconnected.");
            }
        } while (running);

    }
}
