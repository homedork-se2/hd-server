package homdork.code.data;

import java.sql.*;

public class SqlConnector {


    public class SQLConnector {

        private final String database = "homedork";
        private final String portNumber = "3306";
        private final String username = "admin";
        private final String password = "Ed5!_3nNLzwI";

        Connection connection;
        Statement statement;
        ResultSet resultSet;
        PreparedStatement preparedStatement;

        public SQLConnector() {
            connect();
        }

        // Connect to the database
        public void connect() {
            try {
                System.out.println("Connection successfully established..");
                String url = "jdbc:mysql://localhost:" + portNumber + "/" + database + "?user=" + username + "&password=" + password + "&serverTimezone=UTC";
                connection = DriverManager.getConnection(url);

            } catch (SQLException sq) {
                sq.printStackTrace();
                System.out.println(sq.getMessage());
            }
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
}