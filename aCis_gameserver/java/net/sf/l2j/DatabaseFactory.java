package net.sf.l2j;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseFactory
{
	protected static Logger _log = Logger.getLogger(DatabaseFactory.class.getName());
	
	private static HikariDataSource _hds;
	
	public DatabaseFactory()
	{
		_hds = new HikariDataSource();
		_hds.setDriverClassName("org.mariadb.jdbc.Driver");
		_hds.setJdbcUrl(Config.DATABASE_URL);
		_hds.setUsername(Config.DATABASE_LOGIN);
		_hds.setPassword(Config.DATABASE_PASSWORD);
		_hds.setMaximumPoolSize(Config.DATABASE_MAX_CONNECTIONS);
		_hds.setIdleTimeout(0);
		// A maximum life time of 15 minutes
		_hds.setMaxLifetime(900000);
		
        try
        {
    		// Test the connection
    		_hds.getConnection().close();
        }
        catch (Exception e)
        {
            _log.warning("Database Factory: Could not initialize database connection. Reason: " + e.getMessage());
        }
	}
	
	public void shutdown()
	{
		try
		{
			_hds.close();
			_hds = null;
		}
		catch (Exception e)
		{
			_log.log(Level.INFO, "", e);
		}
	}

	/**
	 * Use brace as a safty precaution in case name is a reserved word.
	 * @param whatToCheck the list of arguments.
	 * @return the list of arguments between brackets.
	 */
	public static final String safetyString(String... whatToCheck)
	{
		final StringBuilder sb = new StringBuilder();
		for (String word : whatToCheck)
		{
			if (sb.length() > 0)
				sb.append(", ");
			
			sb.append('`');
			sb.append(word);
			sb.append('`');
		}
		return sb.toString();
	}
	
	public Connection getConnection()
	{
		Connection con = null;
		
		while (con == null)
		{
			try
			{
				con = _hds.getConnection();
			}
			catch (SQLException e)
			{
				_log.warning("L2DatabaseFactory: getConnection() failed, trying again " + e);
			}
		}
		return con;
	}
	
	public static DatabaseFactory getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DatabaseFactory _instance = new DatabaseFactory();
	}
}