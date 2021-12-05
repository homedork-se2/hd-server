package homdork.code.comm;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HubClient {
	Socket socket;
	InputStream dataInputStream;
	OutputStream dos;
	BufferedReader bufferedReader;


	private void setUp(String hubAddress) {
		try {
			socket = new Socket(hubAddress, 1234);
			dataInputStream = socket.getInputStream();
			dos = socket.getOutputStream();
			bufferedReader = new BufferedReader(new InputStreamReader(dataInputStream));
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @param message    - decrypted query from API
	 * @param pinNumber  - pin/slot associated with device object.
	 * @param hubAddress - address of device being updated.
	 * @param level      - level of device being updated
	 * @param logger     - server LOGGER object.
	 * @param deviceType - device Type for local hub
	 */
	public void transmit(String message, String pinNumber, String hubAddress, double level, String deviceType, Logger logger) {
		setUp(hubAddress);
		System.out.println(message);
		String m = null;
		try {
			// turn device off
			if(message.contains("state='OFF'")) {
				m = String.format("'%s':'%s':OFF", deviceType, pinNumber);
				// sliding
			} else if(message.contains("level") && message.contains("state='ON'") && !message.contains("80")) {
				m = String.format("'%s':'%s':'%.1f'", deviceType, pinNumber, level);
				// turn device on
			} else if(message.contains("state='ON'")) {
				m = String.format("'%s':'%s':ON", deviceType, pinNumber);
			} else if(message.contains("FREE-PIN")) {
				// ND - New Device
				m = String.format("ND:'%s':'%s'", deviceType, pinNumber);
			}else if(message.contains("DELETE")){
				// RD - Remove Device
				m = String.format("RD:'%s':'%s'", deviceType, pinNumber);
			}

			assert m != null;
			dos.write(m.getBytes(StandardCharsets.UTF_8));

			logger.log(Level.INFO, "DEVICE COMMAND(INSERT,UPDATE) SENT TO HUB");

			// receive local hub response ... success/fail
			String hubResponse = bufferedReader.readLine();
			if(!hubResponse.toLowerCase(Locale.ROOT).contains("success")) {
				logger.log(Level.WARNING, "EXPECTED A DIFF HUB RESPONSE");
			} else
				logger.log(Level.INFO, "HUB RESPONSE SUCCESS");

			dos.flush();
			bufferedReader.close();
			dos.close();
		} catch (IOException e) {
			logger.severe(e.getMessage());
		}


	}
}
