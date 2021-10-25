package homdork.code.data;

import java.sql.*;

public class SQLConnector {

    Connection connection;
    Statement statement;
    ResultSet resultSet;
    PreparedStatement preparedStatement;

    public SQLConnector() {

    }

    // Connect to the database
    public Connection connect() {
        try {
            String database = "homedork";
            String portNumber = "3306";
            String username = "root";
            String password = "root";
            String url = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
            connection = DriverManager.getConnection(url);
            System.out.println("Connection successfully established..");

        } catch (SQLException sq) {
            System.err.println("connect[ERROR]: Could not connect to the database. Make sure the credentials are correct.");
        }
        return connection;
    }

    // Disconnect from the database
    public void disconnect() {
        try {
            if (connection != null) {
                System.err.println("Connection has successfully closed..");
                connection.close();
            }
            if (statement != null) {
                System.err.println("Connection has successfully closed..");
                statement.close();
            }
            if (resultSet != null) {
                System.err.println("Connection has successfully closed..");
                resultSet.close();
            }

        } catch (SQLException ex) {
            System.err.println(ex.getMessage());
        }
    }

}
