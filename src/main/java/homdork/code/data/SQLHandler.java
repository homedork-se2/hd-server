package homdork.code.data;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLHandler {

	Connection connection;
	Statement statement;
	ResultSet resultSet;
	Logger logger;

	public void setUp(Logger logger) throws IOException {
		SQLConnector sqlConnector = new SQLConnector();
		connection = sqlConnector.connect();
		this.logger = logger;
	}

	/**
	 * @param query select query on either `users` or `devices` table.
	 * @return result set of selected
	 */
	public ResultSet selectHandler(String query) {
		try {
			statement = connection.createStatement();
			resultSet = statement.executeQuery(query);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}
		return resultSet;
	}

	/**
	 * @param query both user and device specific queries
	 */
	public void updateHandler(String query) {
		try {
			statement = connection.createStatement();
			statement.executeUpdate(query);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage());
		}

	}

	public ResultSet selectUserWhereUUID(String id) {
		String query = "SELECT * FROM `users` WHERE uuid ='" + id + "';";
		return selectHandler(query);
	}

	// change uuid to deviceId in DB
	public ResultSet selectDeviceWhereUUID(String id) {
		String query = "SELECT * FROM `devices` WHERE id ='" + id + "';";
		return selectHandler(query);
	}

	public void createUser() {
		try {
			String sql = "INSERT INTO `users` (`uuid`, `name`, `email`) VALUES ('28285656', 'addddlsgg', 'widddddddil.com');";
			statement = connection.createStatement();
			statement.executeUpdate(sql);

			System.out.println("User created");

		} catch (SQLException e) {
			System.err.println(e.getMessage());
		}
	}

	/**
	 * @param substring - message from local hub client, "HUB-" prefix removed.
	 * @param logger    - logger with added handler for server log.
	 */
	public void handleDeviceOperation(String substring, Logger logger) {
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
			updateHandler(q);

		} else {
			logger.log(Level.INFO, "DEVICE LEVEL ADJUSTMENT");
			String query = String.format("UPDATE devices SET state='on' AND level='%f' WHERE id='%s' AND WHERE userId='%s';", level, deviceID, userID);
			updateHandler(query);
		}

		logger.log(Level.INFO, "UPDATE DEVICE QUERY SENT");
	}
}
