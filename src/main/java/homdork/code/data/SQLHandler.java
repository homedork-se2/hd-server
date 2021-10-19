package homdork.code.data;

import java.sql.*;

public class SQLHandler {

    Connection connection;
    Statement statement;
    ResultSet resultSet;
    PreparedStatement preparedStatement;

    public void setUp() {
        SQLConnector sqlConnector = new SQLConnector();
        connection = sqlConnector.connect();
    }

    public ResultSet selectHandler(String query) throws SQLException {
        statement = connection.createStatement();
        resultSet = statement.executeQuery(query);

        return resultSet;
    }

    public void updateHandler(String query) throws SQLException {
        statement = connection.createStatement();
        statement.executeUpdate(query);
    }

    public ResultSet selectUsersWhereUUD(String id) throws SQLException {
        String query = "SELECT * FROM `users` WHERE uuid =" + id + ";";
        return selectHandler(query);
    }

    public void createUser() {
        try {
            String sql = "INSERT INTO `users` (`uuid`, `name`, `email`) VALUES ('28285656', 'addddlsgg', 'widddddddil.com');";
            statement = connection.createStatement();
            statement.executeUpdate(sql);

            System.out.println("User created");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
