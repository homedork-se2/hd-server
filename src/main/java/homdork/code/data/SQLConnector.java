package homdork.code.data;

import java.sql.*;

public class SQLConnector {

    private final String database = "homedork";
    private final String portNumber = "3306";
    private final String username = "root";
    private final String password = "root";

    Connection connection;
    Statement statement;
    ResultSet resultSet;
    PreparedStatement preparedStatement;

    public SQLConnector() {

    }

    // Connect to the database
    public Connection connect() {
        try {
            String url = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
            connection = DriverManager.getConnection(url);
            System.out.println("Connection successfully established..");

        } catch (SQLException sq) {
            System.out.println("[ERROR]: Could not connect to the database. Make sure the credentials are correct.");
        }
        return connection;
    }

    // Disconnect from the database
    public void disconnect() {
        try {
            if (connection != null) {
                System.out.println("Connection has successfully closed..");
                connection.close();
            }
            if (statement != null) {
                System.out.println("Connection has successfully closed..");
                statement.close();
            }
            if (resultSet != null) {
                System.out.println("Connection has successfully closed..");
                resultSet.close();
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            System.out.println(ex.getMessage());
        }
    }

}
