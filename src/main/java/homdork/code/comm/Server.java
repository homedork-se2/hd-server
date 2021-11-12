package homdork.code.comm;

import homdork.code.data.SQLHandler;
import homdork.code.model.Device;
import homdork.code.security.CryptoHandler;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
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
							ApiTransmitter.getUserDevices(message, outputStream, sqlHandler, cryptoHandler, logger, true);

					} else if(message.contains("UPDATE") && message.contains("devices")) {
						logger.log(Level.INFO, "UPDATE DEVICE HANDLER");

						//update user[1]
						sqlHandler.updateHandler(message);
						logger.log(Level.INFO, "UPDATE DEVICE QUERY SENT");

						// select newly updated device in [1] and write to output stream
						// returns deviceId, hubAddress and level of updated device
						String[] parts = ApiTransmitter.retrieveReturnDevice(message, outputStream, sqlHandler, cryptoHandler, logger);
						outputStream.flush();
						try {
							assert parts != null;
							double level = Double.parseDouble(parts[1]);
							String hubAddress = parts[2];
							String pinNumber = parts[3];
							String deviceType = parts[4];
						} catch (Exception e) {
							logger.severe(e.getMessage());
						}


						// communication with local hub
						/*HubClient hubClient = new HubClient();
						hubClient.transmit(message, pinNumber, hubAddress, level, deviceType, logger);*/

					} else if(message.contains("FREE-PIN")) {
						logger.log(Level.INFO, "INSERT DEVICE HANDLER");
						// FREE PINS 27,28,29
						List<Integer> pins = new ArrayList<>();
						pins.add(27);
						pins.add(28);
						pins.add(29);

						String[] parts = message.split(" ");
						String userId = parts[1];
						String deviceType = parts[2];

						List<Device> devices = ApiTransmitter.getUserDevices(message, outputStream, sqlHandler, cryptoHandler, logger, false);

						for(int i = 0; i < Objects.requireNonNull(devices).size(); i++) {
							for(int j = 0; j < pins.size(); j++) {
								if(devices.get(i).getPin() == pins.get(j)) {
									pins.remove(pins.get(j));
								}
							}
						}

						String freePin;
						if(pins.size() == 0) {
							outputStream.writeBytes("status code: 350-" + null + "\r\n");
						} else {
							freePin = pins.get(0).toString();
							outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(freePin) + "\r\n");

							String deviceId = gen();
							String hubAddress = devices.get(0).getHubAddress();

							// insert into devices(id,type,users_id,level,hubAddress,pin,state) values("1111","LAMP","34341",0,"194.47.44.225",11,’OFF’);
							String query = String.format("INSERT into devices(id,type,user_id,level,hub_address,pin,state) VALUES('%s','%s','%s'," + 0.0 + ",'%s'," + Integer.parseInt(freePin) + ",'OFF');", deviceId, deviceType, userId, hubAddress);
							System.out.println(query);
							sqlHandler.updateHandler(query); // insert new device in DB
							logger.log(Level.INFO, "INSERT NEW DEVICE QUERY SENT");

						/*	HubClient hubClient = new HubClient();
							hubClient.transmit(message, freePin, hubAddress, 0.0, deviceType, logger);*/
						}
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

	public String gen() {
		Random r = new Random(System.currentTimeMillis());
		return String.valueOf(((1 + r.nextInt(2)) * 10000 + r.nextInt(10000)));
	}
}