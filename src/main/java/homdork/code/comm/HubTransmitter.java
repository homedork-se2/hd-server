package homdork.code.comm;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HubTransmitter {

	public static void transmit(String message, Map<String, ClientModel> connectedClient,
								String hubAddress, String deviceId, double level, Logger logger) throws Exception {

		ClientModel c = connectedClient.get(hubAddress);

		Socket socket = c.getSocket();
		DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(socket.getInputStream()));  // read local hub response to device state update

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

		// receive local hub response ... success/fail
		String hubResponse = bufferedReader.readLine();
		if(!hubResponse.contains("success")) {
			logger.log(Level.WARNING, "EXPECTED A DIFF HUB RESPONSE");
		} else
			logger.log(Level.INFO, "HUB RESPONSE SUCCESS");
		dos.flush();
		bufferedReader.close();
		dos.close();


	}
}
