package API;

/**
 * @ClassName DBConfig
 * @Description 获取数据库连接
 * @Auther liujing
 * @Date 2020/7/23 10:08]
 * @Version 1.0
 **/
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBConfig {
    private static final String host = "127.0.0.1";
    private static final int port = 3306;
    private static final String user = "root";
    private static final String password = "";
    private static final String database = "2020_tangshi";

    // 用的饿汉模式的单例
    private static final DataSource dataSource;
    static {
        MysqlDataSource mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setServerName(host);
        mysqlDataSource.setPort(port);
        mysqlDataSource.setUser(user);
        mysqlDataSource.setPassword(password);
        mysqlDataSource.setDatabaseName(database);
        mysqlDataSource.setUseSSL(false);
        mysqlDataSource.setCharacterEncoding("utf8");

        dataSource = mysqlDataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close(Connection connection, PreparedStatement statement, ResultSet resultSet) {
        try {
            if(resultSet!=null){
                resultSet.close();
            }
            if(statement!=null){
                statement.close();
            }
            if(connection!=null){
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

