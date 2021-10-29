package homdork.code.data;

import java.io.IOException;
import java.sql.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLConnector {

	Connection connection;
	Statement statement;
	ResultSet resultSet;
	Logger logger;
	FileHandler fileHandler;

	public SQLConnector() throws IOException {
		this.logger = Logger.getLogger("SERVER_LOG");
		fileHandler = new FileHandler("server.log", true);
		logger.addHandler(fileHandler);
	}

	// Connect to the database
	public Connection connect() {
		try {
			String database = "homedork", portNumber = "3306", username = "root", password = "root";
			String url = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
			connection = DriverManager.getConnection(url);

			logger.log(Level.INFO, "DB CONNECTION SUCCESSFUL");

		} catch (SQLException ex) {
			logger.log(Level.SEVERE, ex.getMessage());
		}
		return connection;
	}

	// Disconnect from the database
	public void disconnect() {
		try {
			if(connection != null) {
				connection.close();
			}
			if(statement != null) {
				statement.close();
			}
			if(resultSet != null) {
				resultSet.close();
			}
			logger.log(Level.INFO, "DB CONNECTION CLOSED");
		} catch (SQLException ex) {
			System.err.println(ex.getMessage());
			logger.log(Level.SEVERE, ex.getMessage());
		}
	}

}
