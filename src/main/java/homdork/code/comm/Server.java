package homdork.code.comm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import homdork.code.data.SQLHandler;
import homdork.code.model.*;
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
			handler();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getDeviceId(String message) {
		StringBuilder builder = new StringBuilder();
		boolean isParenthesis = false;
		for(char c : message.toCharArray()) {
			if(c == '\'' || isParenthesis) {
				if(isParenthesis) {
					if(c != '\'') {
						builder.append(c);
					} else { // last "'" mark

						System.out.println("DEVICE ID: " + builder.toString());
						return builder.toString();
					}
				}
				isParenthesis = true;
			}
		}

		return null;
	}

	public String getUUIDFromMessage(String message) {
		//Get the UUID from table users or devices
		StringBuilder builder = new StringBuilder();
		boolean isParenthesis = false;
		for(char c : message.toCharArray()) {
			if(c == '\'' || isParenthesis) {
				if(isParenthesis) {
					if(c != '\'' && c != ';') {
						builder.append(c);
					}
				}
				isParenthesis = true;
			}
		}
		String[] parts = builder.toString().split(",");
		String userID = parts[0];
		System.out.println("USERID: " + userID);
		return userID;
	}

	/**
	 * @param message       decrypted message from API
	 * @param outputStream  socket's outputStream for writes
	 * @param sqlHandler    sqö operations handling class
	 * @param cryptoHandler cryptography class
	 * @throws Exception called when newly saved user object or updated user object is to be returned (ON INSERT;SELECT AND UPDATE)
	 */
	void retrieveReturnUser(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler) throws Exception {
		ResultSet resultSet = sqlHandler.selectUserWhereUUID(getUUIDFromMessage(message));
		if(resultSet.next()) {
			String uuid = resultSet.getString("id");
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

	}

	boolean checkClient(String message) {
		return message.contains("API-");
	}

	// handler for both API and Local hub
	public void handler() throws Exception {
		boolean running = true;
		BufferedReader bis = new BufferedReader(new InputStreamReader(client.getInputStream()));
		CryptoHandler cryptoHandler = new CryptoHandler();
		SQLHandler sqlHandler = new SQLHandler();

		do {
			try {
				String encryptedMessage = bis.readLine();
				byte[] byteArray = encryptedMessage.getBytes(StandardCharsets.UTF_8);


				cryptoHandler.setUpCipher();
				//Decrypt encryptedMessage and print it
				String message = cryptoHandler.aesDecrypt(byteArray);
				System.out.println("[DECRYPTED/READ]: " + message);

				// API- client = API
				// HUB- client = Local hub

				if(checkClient(message)) {
					sqlHandler.setUp();
					message = message.substring(4); // remove "API-" or "HUB-"

					if(message.contains("INSERT") && message.contains("users")) {
						System.out.println("[LOG] Entered insert handler.");

						//save new user[1]
						sqlHandler.updateHandler(message);

						//select new saved user in [1] and write to output stream
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("UPDATE") && message.contains("users")) {
						System.out.println("[LOG] Entered update handler.");

						//update user[1]
						sqlHandler.updateHandler(message);

						//select newly updated user in [1] and write to output stream
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("SELECT") && message.contains("users")) {
						System.out.println("[LOG] Entered select handler.");

						//select user
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

						// DEVICE OPERATIONS
					} else if(message.contains("SELECT") && message.contains("devices")) {
						System.out.println("[LOG] Entered select handler.");

						// select device
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("UPDATE") && message.contains("devices")) {
						System.out.println("[LOG] Entered update handler.");
						//update user[1]
						sqlHandler.updateHandler(message);

						// select newly updated device in [1] and write to output stream
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);
					} else if(message.contains("INSERT") && message.contains("devices")) {
						System.out.println("[LOG] Entered insert handler.");
						//update user[1]
						sqlHandler.updateHandler(message);
						//select new saved device in [1] and write to output stream
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);
					}

					//Send return code & string of encrypted message to the client
					outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(message) + "\r\n");
					outputStream.flush();
				} else {
					// client = Local Hub

					// physical change in device state
					// add new device
					// client device operations like turn lamp off are handled up in the else if
				}


			} catch (SocketException e) {
				running = false;
				System.out.println("[ERROR] Client disconnected.");
			}
		} while (running);

	}

	private void retrieveReturnDevice(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler) {

		try {
			ResultSet resultSet = sqlHandler.selectDeviceWhereUUID(getDeviceId(message));
			if(resultSet.next()) {
				String deviceId = resultSet.getString("id");
				String type = resultSet.getString("type");
				String state = resultSet.getString("state");
				String userID = resultSet.getString("users_id");
				double level = resultSet.getDouble("level");

				switch (type) {
					case "FAN" -> {
						Fan fan = new Fan(deviceId);
						fan.setId(deviceId);
						fan.setUserId(userID);
						if(state.equals("ON")) {
							fan.setState(homdork.code.model.State.ON);
						} else {
							fan.setState(homdork.code.model.State.OFF);
						}
						fan.setLevel(level);
						write(fan, outputStream, cryptoHandler);
					}
					case "LAMP" -> {
						Lamp lamp = new Lamp(deviceId);
						lamp.setDeviceType(DeviceType.LAMP);
						lamp.setId(deviceId);
						lamp.setUserId(userID);
						if(state.equals("ON")) {
							lamp.setState(homdork.code.model.State.ON);
						} else {
							lamp.setState(homdork.code.model.State.OFF);
						}
						lamp.setLevel(level);
						write(lamp, outputStream, cryptoHandler);
					}
					case "CURTAIN" -> {
						Curtain curtain = new Curtain(deviceId);
						curtain.setDeviceType(DeviceType.CURTAIN);
						curtain.setId(deviceId);
						curtain.setUserId(userID);
						if(state.equals("ON")) {
							curtain.setState(homdork.code.model.State.ON);
						} else {
							curtain.setState(homdork.code.model.State.OFF);
						}
						curtain.setLevel(level);
						write(curtain, outputStream, cryptoHandler);
					}
					default -> {  //THERM
						Thermometer therm = new Thermometer(deviceId);
						therm.setDeviceType(DeviceType.THERM);
						therm.setId(deviceId);
						therm.setUserId(userID);
						if(state.equals("ON")) {
							therm.setState(homdork.code.model.State.ON);
						} else {
							therm.setState(homdork.code.model.State.OFF);
						}
						therm.setLevel(level);
						write(therm, outputStream, cryptoHandler);
					}
				}

			}
		} catch (Exception e) {
			System.err.println("[ERROR]: " + e.getMessage());
		}

	}


	void write(Object objectClass, DataOutputStream outputStream, CryptoHandler cryptoHandler) throws Exception {
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(objectClass, objectClass.getClass());
		System.out.println(json);
		outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
		outputStream.flush();
	}
}
