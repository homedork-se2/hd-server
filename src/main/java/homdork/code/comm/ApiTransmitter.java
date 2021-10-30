package homdork.code.comm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import homdork.code.data.SQLHandler;
import homdork.code.model.*;
import homdork.code.security.CryptoHandler;

import java.io.DataOutputStream;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApiTransmitter {

	static void transmit(Object objectClass, DataOutputStream outputStream, CryptoHandler cryptoHandler, Logger logger) throws Exception {
		String json = new GsonBuilder()
				.setPrettyPrinting()
				.create()
				.toJson(objectClass, objectClass.getClass());
		System.out.println(json);
		outputStream.writeBytes("status code: 200-" + cryptoHandler.aesEncrypt(json) + "\r\n");
		outputStream.flush();
		logger.log(Level.INFO, "DEVICE OBJECT SENT TO API");
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

	public static String[] retrieveReturnDevice(String message, DataOutputStream outputStream, SQLHandler sqlHandler,
												CryptoHandler cryptoHandler, Logger logger) {
		String[] parts = new String[4];
		try {
			ResultSet resultSet = sqlHandler.selectDeviceWhereUUID(getDeviceId(message));
			if(resultSet.next()) {
				logger.log(Level.INFO, "RESULT SET RECEIVED");
				String deviceId = resultSet.getString("id");
				String type = resultSet.getString("type");
				String state = resultSet.getString("state");
				String userID = resultSet.getString("users_id");
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
						fan.setPin(pin);
						if(state.equals("ON")) {
							fan.setState(homdork.code.model.State.ON);
						} else {
							fan.setState(homdork.code.model.State.OFF);
						}
						fan.setLevel(level);
						transmit(fan, outputStream, cryptoHandler, logger);
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
						transmit(lamp, outputStream, cryptoHandler, logger);
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
						transmit(curtain, outputStream, cryptoHandler, logger);
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
						transmit(therm, outputStream, cryptoHandler, logger);
					}
				}
				return parts;
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		return null;
	}

	private static String getDeviceId(String message) {
		StringBuilder builder = new StringBuilder();
		boolean isParenthesis = false;
		for(char c : message.toCharArray()) {
			if(c == '\'' || isParenthesis) {
				if(isParenthesis) {
					if(c != '\'') {
						builder.append(c);
					} else { // last "'" mark
						// query dependent check
						if(message.contains("deviceId")) {
							System.out.println("DEVICE ID: " + builder);
						} else {
							System.out.println("USER ID: " + builder);
						}
						return builder.toString();

					}
				}
				isParenthesis = true;
			}
		}
		return null;
	}
}
