import java.sql.*;

public class DBService {

    private static Connection connection;
    private static Statement stmt;

    public static boolean connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:userDB.db");
            stmt = connection.createStatement();
            return true;
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getNickByLoginAndPass(String login, String pass) throws SQLException {
        String sql = String.format("select nickname from main where " +
                "login = '%s' and password = '%s'", login, pass);
        ResultSet rs = stmt.executeQuery(sql);

        if (rs.next()) {
            return rs.getString(1);
        } else {
            return null;
        }
    }

    public static void logIn(String nick) {
        try {
            String sql = String.format("update main set logged = 'true' where nickname = '%s'", nick);
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }public static void logOut(String nick) {
        try {
            String sql = String.format("update main set logged = 'false' where nickname = '%s'", nick);
            stmt.executeUpdate(sql);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean isNickBusy(String nick) throws SQLException {
        String sql = String.format("select nickname from main where logged = 'true' and nickname = '%s'", nick);
        ResultSet rs = stmt.executeQuery(sql);
        return rs.next();
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
