package homdork.code.comm;

import homdork.code.data.SQLHandler;
import homdork.code.security.CryptoHandler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Arrays;
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
	SQLHandler sqlHandler;

	Map<String, ClientModel> connectedClients = ServerMain.getMap();

	public Server(Socket clientSocket, SQLHandler handler) throws IOException {
		this.client = clientSocket;
		this.outputStream = new DataOutputStream(client.getOutputStream());
		this.inputStream = new DataInputStream(client.getInputStream());
		this.reader = new BufferedReader(new InputStreamReader(inputStream));
		this.logger = Logger.getLogger("SERVER_LOG");
		this.sqlHandler = handler;
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

	boolean checkClient(String message) {
		return message.contains("API-");
	}

	// handler for both API and Local hub
	public void handler() throws Exception {
		boolean running = true;
		BufferedReader bis = new BufferedReader(new InputStreamReader(client.getInputStream()));
		CryptoHandler cryptoHandler = new CryptoHandler();

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
                byte[] byteArray = new byte[0];
                try {
                    byteArray = encryptedMessage.getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    logger.severe(e.getMessage());
                }

				cryptoHandler.setUpCipher();
				//Decrypt encryptedMessage and print it
				String message = cryptoHandler.aesDecrypt(byteArray);
				logger.log(Level.INFO, "[DECRYPTED/READ]: " + message);

				// API- client = API
				// HUB- client = Local hub

				if(checkClient(message)) {
					//	sqlHandler.setUp(logger);
					message = message.substring(4); // remove "API-" or "HUB-"
					logger.log(Level.INFO, "API OPERATION");

					if(message.contains("INSERT") && message.contains("users")) {
						logger.log(Level.INFO, "INSERT USER HANDLER OPERATION");

						//save new user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "INSERT USER QUERY SENT");

						//select new saved user in [1] and write to output stream
						ApiTransmitter.retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler, logger);

					} else if(message.contains("UPDATE") && message.contains("users")) {
						logger.log(Level.INFO, "UPDATE USER HANDLER OPERATION");

						//update user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "UPDATE USER QUERY SENT");

						//select newly updated user in [1] and write to output stream
						ApiTransmitter.retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler, logger);

					} else if(message.contains("SELECT") && message.contains("users") && !message.contains("users_id")) {
						logger.log(Level.INFO, "SELECT USER HANDLER OPERATION");

						//select user
						ApiTransmitter.retrieveReturnUser(message, outputStream, sqlHandler, cryptoHandler, logger);

						// DEVICE OPERATIONS
					} else if(message.contains("SELECT") && message.contains("devices")) {
						logger.log(Level.INFO, "SELECT DEVICE HANDLER OPERATION");

						if(!message.contains("user_id"))
							ApiTransmitter.retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler, logger);
						else
							ApiTransmitter.getUserDevices(message, outputStream, sqlHandler, cryptoHandler, logger);

					} else if(message.contains("UPDATE") && message.contains("devices")) {
						logger.log(Level.INFO, "UPDATE DEVICE HANDLER");

						//update user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "UPDATE DEVICE QUERY SENT");

						// select newly updated device in [1] and write to output stream
						// returns deviceId, hubAddress and level of updated device
						String[] parts = ApiTransmitter.retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler, logger);

						assert parts != null;
						double level = Double.parseDouble(parts[1]);
						String hubAddress = parts[2];
						String pinNumber = parts[3];

						outputStream.flush();
                        // communication with local hub
						HubClient hubClient = new HubClient();
						hubClient.transmit(message,pinNumber,hubAddress,level,logger);

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
						logger.log(Level.INFO, "INSERT DEVICE HANDLER");

						//update user[1]
						message = message.replace("ppp", "pinNr");
						message = message.replace("ddd", "deviceId");
						sqlHandler.updateHandler(message); // modified message for pin
						logger.log(Level.INFO, "INSERT DEVICE QUERY SENT");

						//select new saved device in [1] and write to output stream
						ApiTransmitter.retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler, logger);
					}
				} else {
					// client = Local Hub
					message = message.substring(4); // remove "API-" or "HUB-"
					logger.log(Level.INFO, "LOCAL HUB OPERATION");
					logger.log(Level.INFO, "HUB COMMAND: " + message);

					if(message.contains("D:")) {
						sqlHandler.handleDeviceOperation(message.substring(2), logger, outputStream); // remove "D:"
					}
					System.out.println(Arrays.toString(connectedClients.keySet().toArray()));
				}
			} catch (SocketException e) {
				running = false;
				logger.log(Level.SEVERE, e.getMessage());
			}
		} while (running);
	}
}