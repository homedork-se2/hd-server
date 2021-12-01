package homdork.code.comm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import homdork.code.data.SQLHandler;
import homdork.code.model.*;
import homdork.code.security.CryptoHandler;

import java.io.DataOutputStream;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiTransmitter {

	/**
	 * @param objectClass   - Device object to be sent back to API as JSON response on "GET DEVICE" request.
	 * @param outputStream  - Stream on with response is written.
	 * @param cryptoHandler - For encryption of response to API.
	 * @param logger        - server LOGGER object.
	 */
	static void transmit(Object objectClass, DataOutputStream outputStream, CryptoHandler cryptoHandler, Logger logger) {
		try {
			String json = new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(objectClass, objectClass.getClass());
			System.out.println(json);
			outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
			outputStream.flush();
			logger.log(Level.INFO, "DEVICE OBJECT SENT TO API");
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}

	}

	/**
	 * @param devices       - List of devices to be sent back to API as JSON response on "GET * DEVICES" request.
	 * @param outputStream  - Stream on with response is written.
	 * @param cryptoHandler - For encryption of response to API.
	 * @param logger        - server LOGGER object.
	 */
	static void transmit(List<Device> devices, DataOutputStream outputStream, CryptoHandler cryptoHandler, Logger logger) {
		try {
			String json = new GsonBuilder()
					.setPrettyPrinting()
					.create()
					.toJson(devices, devices.getClass());
			System.out.println(json);
			outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
			outputStream.flush();
			logger.log(Level.INFO, "DEVICES LIST SENT TO API");
		} catch (Exception e) {
			logger.severe(e.getMessage());
		}
	}

	/**
	 * @param message       decrypted message from API
	 * @param outputStream  socket's outputStream for writes
	 * @param sqlHandler    SQL operations handling class
	 * @param cryptoHandler cryptography class
	 * @throws Exception -
	 *                   <p>
	 *                   <p>
	 *                   Called when newly saved user object or updated user object is to be returned
	 *                   (ON INSERT;SELECT AND UPDATE)
	 */
	public static void retrieveReturnUser(String message, DataOutputStream outputStream,
										  SQLHandler sqlHandler, CryptoHandler cryptoHandler, Logger logger) throws Exception {

		ResultSet resultSet = sqlHandler.selectUserWhereUUID(getUUIDFromMessage(message));
		try {
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
				logger.log(Level.INFO, "USER OBJECT FROM RESULT_SET SENT TO API");
			} else {
				outputStream.writeBytes("status code: 350-" + null + "\r\n");
				logger.info("NULL USER REQUESTED");
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, e.getMessage());
		}

	}

	static String getUUIDFromMessage(String message) {
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
	 * @param message       - Decrypted message from API
	 * @param outputStream  - Socket's outputStream for writes
	 * @param sqlHandler    - SQL operations handling class
	 * @param cryptoHandler - Cryptography class
	 *                      <p>
	 *                      <p>
	 *                      Called when newly saved device(abstract) object or updated device object is to be returned
	 *                      (ON INSERT;SELECT AND UPDATE)
	 *                      <p>
	 *                      Based on the value of {@code #{type} } a device type object is created and transmitted to the API
	 *                      using the {@link #transmit(List, DataOutputStream, CryptoHandler, Logger)} function.
	 */
	public static String[] retrieveReturnDevice(String message, DataOutputStream outputStream, SQLHandler sqlHandler,
												CryptoHandler cryptoHandler, Logger logger) {
		String[] parts = new String[5];
		try {
			String devId = getDeviceId(message);
			System.out.println(devId);
			assert devId != null;
			ResultSet resultSet = sqlHandler.selectDeviceById(devId.trim());
			if(resultSet.next()) {
				logger.log(Level.INFO, "RESULT SET RECEIVED");
				String deviceId = resultSet.getString("id");
				String type = resultSet.getString("type");
				String state = resultSet.getString("state");
				String userID = resultSet.getString("user_id");
				double level = resultSet.getDouble("level");
				// new
				String hubAddress = resultSet.getString("hub_address");
				String pin = resultSet.getString("pin");

				parts[0] = deviceId;
				parts[1] = String.valueOf(((int) level));
				parts[2] = hubAddress;
				parts[3] = pin;
				parts[4] = type;


				switch (type) {
					case "FAN" -> {
						Fan fan = new Fan(deviceId);
						fan.setId(deviceId);
						fan.setUserId(userID);
						fan.setDeviceType(DeviceType.FAN);
						fan.setPin(pin);
						fan.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							fan.setState(State.ON);
						} else {
							fan.setState(State.OFF);
						}
						fan.setLevel(level);
						transmit(fan, outputStream, cryptoHandler, logger);
					}
					case "LAMP" -> {
						System.out.println("lamp obj");
						Lamp lamp = new Lamp(deviceId);
						lamp.setDeviceType(DeviceType.LAMP);
						lamp.setId(deviceId);
						lamp.setUserId(userID);
						lamp.setPin(pin);
						lamp.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							lamp.setState(State.ON);
						} else {
							lamp.setState(State.OFF);
						}
						lamp.setLevel(level);
						transmit(lamp, outputStream, cryptoHandler, logger);
					}
					case "CURTAIN" -> {
						Curtain curtain = new Curtain(deviceId);
						curtain.setDeviceType(DeviceType.CURTAIN);
						curtain.setId(deviceId);
						curtain.setUserId(userID);
						curtain.setPin(pin);
						curtain.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							curtain.setState(State.ON);
						} else {
							curtain.setState(State.OFF);
						}
						curtain.setLevel(level);
						transmit(curtain, outputStream, cryptoHandler, logger);
					}
					case "THERM" -> {
						Thermometer therm = new Thermometer(deviceId);
						therm.setDeviceType(DeviceType.THERM);
						therm.setId(deviceId);
						therm.setPin(pin);
						therm.setUserId(userID);
						therm.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							therm.setState(State.ON);
						} else {
							therm.setState(State.OFF);
						}
						therm.setLevel(level);
						transmit(therm, outputStream, cryptoHandler, logger);
					}
					case "WINDOW" -> {
						Window window = new Window(deviceId);
						window.setDeviceType(DeviceType.WINDOW);
						window.setId(deviceId);
						window.setPin(pin);
						window.setUserId(userID);
						window.setHubAddress(hubAddress);
						if(state.equals("OPEN")) {
							window.setState(State.OPEN);
						} else {
							window.setState(State.CLOSED);
						}
						window.setLevel(level);
						transmit(window, outputStream, cryptoHandler, logger);
					}
					case "ALARM" -> {
						Alarm alarm = new Alarm(deviceId);
						alarm.setDeviceType(DeviceType.ALARM);
						alarm.setId(deviceId);
						alarm.setPin(pin);
						alarm.setUserId(userID);
						alarm.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							alarm.setState(State.ON);
						} else {
							alarm.setState(State.OFF);
						}
						alarm.setLevel(level);
						transmit(alarm, outputStream, cryptoHandler, logger);
					}
					default -> outputStream.writeBytes("status code: 350-" + null + "\r\n");
				}
				return parts;
			} else {
				outputStream.writeBytes("status code: 350-" + null + "\r\n");
				logger.info("NULL DEVICE REQUESTED");
			}


		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		return null;
	}

	private static String getDeviceId(String message) {
		StringBuilder builder = new StringBuilder();
		boolean isParenthesis = false;
		int deviceIdLength = 5;
		String deviceId;

		if(message.contains("state") && !message.contains("level")) {
			message = message.substring(39);
			message = message.replace("'", "");
			deviceId = message.replace(";", "");
			System.out.println("DEVICE ID: " + message);
			return deviceId;
		} else if(message.contains("state") && message.contains("level")) {
			message = message.substring(39);
			message = message.replace("'", "");
			message = message.replace(";", "");
			deviceId = message.substring(message.length() - deviceIdLength);
			System.out.println("DEVICE ID: " + deviceId);
			return deviceId;
		} else {
			for(char c : message.toCharArray()) {
				if(c == '\'' || isParenthesis) {
					if(isParenthesis) {
						if(c != '\'') {
							builder.append(c);
						} else { // last "'" mark
							return builder.toString();
						}
					}
					isParenthesis = true;
				}
			}

		}
		return null;
	}

	public static List<Device> getUserDevices(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler, Logger logger, boolean checker) {

		try {

			ResultSet resultSet;
			if(checker) {
				String userId = getDeviceId(message);
				System.out.println(userId);
				assert userId != null;
				resultSet = sqlHandler.selectDeviceByUserId(message);
			} else {
				// construct select sql query based on user ID
				String[] parts = message.split(" ");
				String userId = parts[1];
				String query = String.format("SELECT * from devices WHERE user_id='%s';", userId);
				resultSet = sqlHandler.selectDeviceByUserId(query);
			}

			List<Device> devices = new ArrayList<>();

			while (resultSet.next()) {
				logger.log(Level.INFO, "RESULT SET RECEIVED");
				String deviceId = resultSet.getString("id");
				String type = resultSet.getString("type");
				String state = resultSet.getString("state");
				String userID = resultSet.getString("user_id");
				double level = resultSet.getDouble("level");
				// new
				String hubAddress = resultSet.getString("hub_address");
				String pin = resultSet.getString("pin");

				switch (type) {
					case "FAN" -> {
						Fan fan = new Fan(deviceId);
						fan.setId(deviceId);
						fan.setDeviceType(DeviceType.FAN);
						fan.setUserId(userID);
						fan.setPin(pin);
						fan.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							fan.setState(homdork.code.model.State.ON);
						} else {
							fan.setState(homdork.code.model.State.OFF);
						}
						fan.setLevel(level);
						devices.add(fan);
					}
					case "LAMP" -> {
						System.out.println("lamp obj");
						Lamp lamp = new Lamp(deviceId);
						lamp.setDeviceType(DeviceType.LAMP);
						lamp.setId(deviceId);
						lamp.setUserId(userID);
						lamp.setPin(pin);
						lamp.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							lamp.setState(State.ON);
						} else {
							lamp.setState(State.OFF);
						}
						lamp.setLevel(level);
						devices.add(lamp);
					}
					case "CURTAIN" -> {
						Curtain curtain = new Curtain(deviceId);
						curtain.setDeviceType(DeviceType.CURTAIN);
						curtain.setId(deviceId);
						curtain.setUserId(userID);
						curtain.setPin(pin);
						curtain.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							curtain.setState(State.ON);
						} else {
							curtain.setState(State.OFF);
						}
						curtain.setLevel(level);
						devices.add(curtain);
					}
					case "THERM" -> {
						Thermometer therm = new Thermometer(deviceId);
						therm.setDeviceType(DeviceType.THERM);
						therm.setId(deviceId);
						therm.setPin(pin);
						therm.setUserId(userID);
						therm.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							therm.setState(State.ON);
						} else {
							therm.setState(State.OFF);
						}
						therm.setLevel(level);
						devices.add(therm);
					}
					case "WINDOW" -> {
						Window window = new Window(deviceId);
						window.setDeviceType(DeviceType.WINDOW);
						window.setId(deviceId);
						window.setPin(pin);
						window.setUserId(userID);
						window.setHubAddress(hubAddress);
						if(state.equals("OPEN")) {
							window.setState(State.OPEN);
						} else {
							window.setState(State.CLOSED);
						}
						window.setLevel(level);
						devices.add(window);
					}
					case "ALARM" -> {
						Alarm alarm = new Alarm(deviceId);
						alarm.setDeviceType(DeviceType.ALARM);
						alarm.setId(deviceId);
						alarm.setPin(pin);
						alarm.setUserId(userID);
						alarm.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							alarm.setState(State.ON);
						} else {
							alarm.setState(State.OFF);
						}
						alarm.setLevel(level);
						devices.add(alarm);
					}
					default -> outputStream.writeBytes("status code: 350-" + null + "\r\n");
				}
			}

            if (checker) {
                // devices being "null" or "not"
                transmit(devices, outputStream, cryptoHandler, logger);
            } else
                return devices;

		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

		return null;
	}


}
