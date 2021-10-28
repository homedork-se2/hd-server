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
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Server extends Thread {
	Socket client;
	DataOutputStream outputStream;
	DataInputStream inputStream;
	BufferedReader reader;
	Logger logger;
	String hubAddress;
	String deviceId;
	double level;
	SQLHandler sqlHandler;

	Map<String, Client> connectedClients = ServerMain.getMap();

	public Server(Socket clientSocket) throws IOException {
		this.client = clientSocket;
		this.outputStream = new DataOutputStream(client.getOutputStream());
		this.inputStream = new DataInputStream(client.getInputStream());
		this.reader = new BufferedReader(new InputStreamReader(inputStream));
		this.logger = Logger.getLogger("SERVER_LOG");
	}

	@Override
	public void run() {
		try {
			handler();
		} catch (Exception e) {
			e.printStackTrace();
			logger.log(Level.SEVERE, e.getMessage());
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
						System.out.println("DEVICE ID: " + builder);
						return builder.toString();
					}
				}
				isParenthesis = true;
			}
		}

		return null;
	}

	// String query = String.format("SELECT * from devices WHERE deviceId='%s' AND WHERE userId='%s';", "727272", "45343");
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
	 * @throws Exception -
	 *                   <p>
	 *                   <p>
	 *                   Called when newly saved user object or updated user object is to be returned
	 *                   (ON INSERT;SELECT AND UPDATE)
	 */
	void retrieveReturnUser(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler) throws Exception {
		ResultSet resultSet = sqlHandler.selectUserWhereUUID(getUUIDFromMessage(message));
		if(resultSet.next()) {
			logger.log(Level.INFO, "RESULT SET RECEIVED");
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
			logger.log(Level.INFO, "USER OBJECT SENT TO API");
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
		sqlHandler = new SQLHandler();

		FileHandler fileHandler;

		try {
			fileHandler = new FileHandler("server.log", true);
			logger.addHandler(fileHandler);
			SimpleFormatter formatter = new SimpleFormatter();
			fileHandler.setFormatter(formatter);

			logger.log(Level.INFO, "-->");
			logger.info("HANDLER INITIATED " + LocalDate.now());
		} catch (Exception e) {
			System.err.println("[ERROR]: " + e.getMessage());
			logger.log(Level.SEVERE, e.getMessage());
		}

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
					logger.log(Level.INFO, "API OPERATION");

					if(message.contains("INSERT") && message.contains("users")) {
						System.out.println("[LOG] Entered insert handler.");
						logger.log(Level.INFO, "INSERT USER HANDLER OPERATION");

						//save new user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "INSERT USER QUERY SENT");

						//select new saved user in [1] and write to output stream
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("UPDATE") && message.contains("users")) {
						System.out.println("[LOG] Entered update handler.");
						logger.log(Level.INFO, "UPDATE USER HANDLER OPERATION");

						//update user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "UPDATE USER QUERY SENT");

						//select newly updated user in [1] and write to output stream
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("SELECT") && message.contains("users")) {
						System.out.println("[LOG] Entered select handler.");
						logger.log(Level.INFO, "SELECT USER HANDLER OPERATION");

						//select user
						retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler);

						// DEVICE OPERATIONS
					} else if(message.contains("SELECT") && message.contains("devices")) {
						System.out.println("[LOG] Entered select handler.");
						logger.log(Level.INFO, "SELECT DEVICE HANDLER OPERATION");

						// select device
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);

					} else if(message.contains("UPDATE") && message.contains("devices")) {
						System.out.println("[LOG] Entered update handler.");
						logger.log(Level.INFO, "UPDATE DEVICE HANDLER");

						//update user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "UPDATE DEVICE QUERY SENT");

						// select newly updated device in [1] and write to output stream
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);

						// communication with local hub
						sendDeviceCommandToHub(message);

					} else if(message.contains("INSERT") && message.contains("devices")) {
						// 12 pin
						// 0 - 3 _ FAN
						// 4 - 7 _ LAMP
						// 8 - 12 THERM

						// gen device id
						// select "type"  === "FAN"  - {type,userId}
						//      - select * from devices where deviceTYpe = FAN AND userID=userId      @queryBuilder
						//			 order devices by pins
						//      - FANS : 0 -3
						// 			 loop i = 0 - 3, if i != get(index).pin   --> return i else return -1

						// let hub know there is a new device ??? not needed
						System.out.println("[LOG] Entered insert handler.");
						logger.log(Level.INFO, "INSERT DEVICE HANDLER");

						//update user[1]
						message = message.replace("ppp", "pinNr");
						message = message.replace("ddd", "deviceId");
						sqlHandler.updateHandler(message); // modified message for pin
						logger.log(Level.INFO, "INSERT DEVICE QUERY SENT");

						//select new saved device in [1] and write to output stream
						retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler);

						// communication with local hub

					}
				} else {
					// client = Local Hub
					message = message.substring(4); // remove "API-" or "HUB-"
					logger.log(Level.INFO, "LOCAL HUB OPERATION");
					logger.log(Level.INFO, "HUB COMMAND: " + message);

					// physical change in device state -- requires DB update
					//       [D:deviceID:ON or level:userID]
					if(message.contains("D:")) {
						handleDeviceOperation(message.substring(2)); // remove "D:"
					}

					// add new device -- synced with UI add, requires DB update(INSERT) (2nd half)


					// client device operations like turn lamp off are handled up in the else if -- ^^
				}

			} catch (SocketException e) {
				running = false;
				System.out.println("[ERROR] Client disconnected.");
				logger.log(Level.SEVERE, e.getMessage());
			}
		} while (running);

	}

	private void handleDeviceOperation(String substring) throws SQLException {
		String[] parts = substring.split(":");
		String deviceID = parts[0];
		String userID = parts[2];

		String op = parts[1];
		double level = 9999;
		// OFF,ON,level(double)
		if(!op.contains("ON")) {
			if(!op.contains("OFF"))
				level = Double.parseDouble(op);
		}

		if(level == 9999) {
			String q;
			if(op.equals("ON")) {
				logger.log(Level.INFO, "DEVICE TURN ON");
				q = String.format("UPDATE devices SET state='on' WHERE id='%s';", deviceID);
			} else {
				logger.log(Level.INFO, "DEVICE TURN OFF");
				q = String.format("UPDATE devices SET state='off' WHERE id='%s';", deviceID);
			}
			sqlHandler.updateHandler(q);

		} else {
			logger.log(Level.INFO, "DEVICE LEVEL ADJUSTMENT");
			String query = String.format("UPDATE devices SET state='on' AND level='%f' WHERE id='%s' AND WHERE userId='%s';", level, deviceID, userID);
			sqlHandler.updateHandler(query);
		}

		logger.log(Level.INFO, "UPDATE DEVICE QUERY SENT");
	}


	private void retrieveReturnDevice(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler) {

		try {
			ResultSet resultSet = sqlHandler.selectDeviceWhereUUID(getDeviceId(message));
			if(resultSet.next()) {
				logger.log(Level.INFO, "RESULT SET RECEIVED");
				deviceId = resultSet.getString("id");
				String type = resultSet.getString("type");
				String state = resultSet.getString("state");
				String userID = resultSet.getString("users_id");
				level = resultSet.getDouble("level");
				// new
				hubAddress = resultSet.getString("hub_address");
				int pin = resultSet.getInt("pin");

				switch (type) {
					case "FAN" -> {
						Fan fan = new Fan(deviceId);
						fan.setId(deviceId);
						fan.setUserId(userID);
						fan.setPin(pin);
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
						lamp.setPin(pin);
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
						curtain.setPin(pin);
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
						therm.setPin(pin);
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
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	void write(Object objectClass, DataOutputStream outputStream, CryptoHandler cryptoHandler) throws Exception {
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(objectClass, objectClass.getClass());
		System.out.println(json);
		outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
		outputStream.flush();
		logger.log(Level.INFO, "DEVICE OBJECT SENT TO API");
	}

	void sendDeviceCommandToHub(String message) throws Exception {
		Client c = connectedClients.get(hubAddress);

		Socket socket = c.getSocket();
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());

		// turn device off
		if(message.contains("state='off'")) {
			String m = String.format("D:'%s':OFF", deviceId);
			dos.writeBytes(m + "\r\n");

		} // sliding
		else if(message.contains("level") && message.contains("state='on'")) {
			String m = String.format("D:'%s':'%f'", deviceId, level);
			dos.writeBytes(m + "\r\n");

		}  // turn device on
		else if(message.contains("state='on'")) {
			String m = String.format("D:'%s':ON", deviceId);
			dos.writeBytes(m + "\r\n");
		}
		logger.log(Level.INFO, "DEVICE COMMAND SENT TO HUB");
		dos.flush();
	}
}