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
			logger.log(Level.INFO, "DEVICE OBJECT SENT TO API");
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
				logger.info("NULL DEVICE REQUESTED");
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
		String[] parts = new String[4];
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
				int pin = resultSet.getInt("pin");

				parts[0] = deviceId;
				parts[1] = String.valueOf(((int) level));
				parts[2] = hubAddress;
				parts[3] = String.valueOf(pin);


				switch (type) {
					case "FAN" -> {
						Fan fan = new Fan(deviceId);
						fan.setId(deviceId);
						fan.setUserId(userID);
						fan.setDeviceType(DeviceType.FAN);
						fan.setPin(pin);
						fan.setHubAddress(hubAddress);
						if(state.equals("ON")) {
							fan.setState(homdork.code.model.State.ON);
						} else {
							fan.setState(homdork.code.model.State.OFF);
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
							lamp.setState(homdork.code.model.State.ON);
						} else {
							lamp.setState(homdork.code.model.State.OFF);
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
							curtain.setState(homdork.code.model.State.ON);
						} else {
							curtain.setState(homdork.code.model.State.OFF);
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
							therm.setState(homdork.code.model.State.ON);
						} else {
							therm.setState(homdork.code.model.State.OFF);
						}
						therm.setLevel(level);
						transmit(therm, outputStream, cryptoHandler, logger);
					}
					default -> {
						outputStream.writeBytes("status code: 350-" + null + "\r\n");
					}
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

	public static void getUserDevices(String message, DataOutputStream outputStream, SQLHandler sqlHandler, CryptoHandler cryptoHandler, Logger logger) {

		try {
			String userId = getDeviceId(message);
			System.out.println(userId);
			assert userId != null;
			ResultSet resultSet = sqlHandler.selectDeviceByUserId(message);
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
				int pin = resultSet.getInt("pin");


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
							lamp.setState(homdork.code.model.State.ON);
						} else {
							lamp.setState(homdork.code.model.State.OFF);
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
							curtain.setState(homdork.code.model.State.ON);
						} else {
							curtain.setState(homdork.code.model.State.OFF);
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
							therm.setState(homdork.code.model.State.ON);
						} else {
							therm.setState(homdork.code.model.State.OFF);
						}
						therm.setLevel(level);
						devices.add(therm);
					}
					default -> {
						outputStream.writeBytes("status code: 350-" + null + "\r\n");
					}
				}
			}

			transmit(devices, outputStream, cryptoHandler, logger);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}


	}


}
