package migrator;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {
	private MysqlDataSource dataSource;
	private Connection connection;
	
	public static void checkConnectionParams(String host, String port, String databaseName, String userName, String password) throws SQLException {
		MysqlDataSource dataSource = GetDataSource(host, port, databaseName, userName, password);
		dataSource.getConnection().close();
	}
	
	private static MysqlDataSource GetDataSource(String host, String port, String databaseName, String userName, String password) {
		MysqlDataSource dataSource = new MysqlDataSource();
		dataSource.setUser(userName);
		dataSource.setPassword(password);
		dataSource.setServerName(host);
		dataSource.setDatabaseName(databaseName);
		dataSource.setPort(Integer.parseInt(port));		
		return dataSource;		
	}
	
	DatabaseConnection(String host, String port, String databaseName, String userName, String password){
		dataSource = GetDataSource(host, port, databaseName, userName, password);
	}

	public void setPort(String port){
		dataSource.setPort(Integer.parseInt(port));
	}

	public void setDatabaseName(String databaseName){
		dataSource.setDatabaseName(databaseName);
	}

	public void setUserName(String userName){
		dataSource.setUser(userName);
	}

	public void setPassword(String password){
		dataSource.setPassword(password);
	}

	public void connect() throws SQLException {
		connection = dataSource.getConnection();
	}
	
	public void disconnect() throws SQLException {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	public boolean isConnected() throws SQLException {
		if (connection == null)
			return false;
		return connection.isValid(0);
	}

	public void execSQL(String sql) throws Exception {
		checkConnection();
		Statement statement = connection.createStatement();
		statement.execute(sql);
		statement.close();
	}

	private void checkConnection() throws Exception {
		if (connection == null)
			throw new Exception("Connection was not initialized");
		if (!connection.isValid(0))
			throw new Exception("Connection is not active");
	}

	private String getSingleValue(String sql, String fieldName) throws Exception {
		checkConnection();

		Statement statement = connection.createStatement();
		ResultSet result = statement.executeQuery(sql);
		result.first();
		String value = result.getNString(fieldName);
		statement.close();
		return value;
	}

	public String getLogPath() throws Exception {
		final String sql = "show variables like 'general_log_file';";

		return getSingleValue(sql, "Value").replace('/','\\');
	}

	public void setLogPath(String path) throws Exception {
		path = path.replace('\\', '/'); // MySQL requires Unix-style path delimiters on all platforms
		final String sql = "SET GLOBAL general_log_file = '" + path + "';";

		execSQL(sql);
	}

	public boolean isLogEnabled() throws Exception {
		final String sql = "show variables like 'general_log';";

		String result = getSingleValue(sql, "Value");
		return result.equals("ON");
	}

	public void enableLog() throws Exception {
		final String sql = "SET GLOBAL general_log = 'ON';";

		execSQL(sql);
	}

	public void disableLog() throws Exception {
		final String sql = "SET GLOBAL general_log = 'OFF';";

		execSQL(sql);
	}
}