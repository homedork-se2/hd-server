package homdork.code.data;

import java.io.IOException;
import java.sql.*;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SQLConnector {

    static Connection connection = null;
    Statement statement;
    ResultSet resultSet;
    static Logger logger;
    FileHandler fileHandler;

    public SQLConnector() throws IOException {
        logger = Logger.getLogger("SERVER_LOG");
        fileHandler = new FileHandler("server.log", true);
        logger.addHandler(fileHandler);
    }

    // Connect to the database
    public Connection connect() {
        if (connection == null) {
            try {
                String database = "mydb", portNumber = "3306", username = "root", password = "Elev6477";
                String url = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
                // String lite = "jdbc:sqlite:\\W:\\sqlite3\\sqlite-tools-win32-x86-3360000\\homedork3.db";
                connection = DriverManager.getConnection(url);

                logger.log(Level.INFO, "DB CONNECTION SUCCESSFUL");

            } catch (SQLException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
            return connection;
        }
        return null;
    }

    // Disconnect from the database
    public void disconnect() {
        try {
            if (connection != null) {
                connection.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
            logger.log(Level.INFO, "DB CONNECTION CLOSED");
        } catch (SQLException ex) {
            logger.log(Level.SEVERE, ex.getMessage());
        }
    }

}
