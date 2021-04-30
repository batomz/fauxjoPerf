package net.jextra.fauxjo.perftest.jdbc.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Create and connect to a connection to an H2 in-memory database.
 * Is not connection-pooled, do not use this for production!
 */
public class H2Con implements DBCon {
	public static Logger _log = LoggerFactory.getLogger(H2Con.class);
	private Connection _con = null;

	/** * H2 database types placeholder. */
	public enum H2DB {
		MEMORY("jdbc:h2:mem:");
		public final String URL;
		H2DB(String conURL) {
			URL = conURL;
		}
	}

	/**
	 * Return a connection to the H2 database.
	 * @param jdbcURI for the connection
	 */
	public Connection connect(String jdbcURI) throws Exception {
		_log.trace("> H2 db: " + jdbcURI);
		if (_con != null && !_con.isClosed()) {
			throw new Exception("Disconnect before creating a new connection");
		}
		try {
			_con = DriverManager.getConnection(jdbcURI);
			_log.info(String.format("< H2 con[%s] %s", _con.hashCode(), jdbcURI));
		} catch (SQLException x) {
			_log.error("H2 db con failed", x);
		}
		return _con;
	}

	/** * Disconnect from the database if connected. */
	public void disconnect() throws Exception {
		if (_con != null && !_con.isClosed()) {
			_con.close();
			_log.info(String.format("> H2 con[%s]", _con.hashCode()));
		}
	}

	/** * Return the current connection to the database. */
	public Connection getConnection() {
		return _con;
	}

}


