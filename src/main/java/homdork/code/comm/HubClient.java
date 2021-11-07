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


	public void setUp(String hubAddress) {
		try {
			hubAddress = "194.47.44.225";
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
	 */
	public void transmit(String message, String pinNumber, String hubAddress, double level, Logger logger) {
		setUp(hubAddress);
		System.out.println(message);
		String m = null;
		try {
			// turn device off
			if(message.contains("state='OFF'")) {
				m = String.format("D:'%s':OFF", pinNumber);
				// sliding
			} else if(message.contains("level") && message.contains("state='ON'") && !message.contains("80")) {
				m = String.format("D:'%s':'%.1f'", pinNumber, level);
				// turn device on
			} else if(message.contains("state='ON'")) {
				m = String.format("D:'%s':ON", pinNumber);
			}

			assert m != null;
			dos.write(m.getBytes(StandardCharsets.UTF_8));

			logger.log(Level.INFO, "DEVICE COMMAND SENT TO HUB");

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
