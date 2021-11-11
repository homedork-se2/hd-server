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

    public Connection connect() {
        if (connection == null) {
            try {
                String database = "mydb", portNumber = "3306", username = "root", password = "Elev6477";
                String mySql = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
                String lite = "jdbc:sqlite://Users/willz/homedork4.db";
                connection = DriverManager.getConnection(mySql);
                logger.log(Level.INFO, "DB CONNECTION SUCCESSFUL");
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, ex.getMessage());
            }
            return connection;
        }
        return null;
    }

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
