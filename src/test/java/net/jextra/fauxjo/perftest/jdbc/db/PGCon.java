package net.jextra.fauxjo.perftest.jdbc.db;

import net.jextra.fauxjo.perftest.jdbc.IDEConsoleInputScroller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Create and connect to postgresql database.
 * Is not connection-pooled, do not use this for production!
 */
@SuppressWarnings("CommentedOutCode")
public class PGCon implements DBCon {
	public static Logger _log = LoggerFactory.getLogger(PGCon.class);
	private Connection _con = null;

	/** * Postgres test databases (eg dev, uat, prod) */
	public enum PGDB {
		LOCALDB("jdbc:postgresql://localhost:5432/fjoperf?user=test");
		public final String URL;
		PGDB(String conURL) {
			URL = conURL;
		}
	}

	/**
	 * Return a connection to the postgres database after prompting user to enter the password in the current console.
	 * @param jdbcURI must contain the host, user, and database (port is optional) - will prompt for password in console.
	 */
	public Connection connect(String jdbcURI) throws Exception {
		_log.trace("> PG db: " + jdbcURI);
		if (_con != null && !_con.isClosed()) {
			throw new Exception("Disconnect before creating a new connection");
		}
		try {
			String pw = IDEConsoleInputScroller.getIDEConsoleInput(false);
			Properties props = new Properties();
			props.setProperty("password",pw);
			_log.info(String.format("- PG %s", jdbcURI));
			_con = DriverManager.getConnection(jdbcURI, props);
			_log.info(String.format("< PG con[%s] %s", _con.hashCode(), jdbcURI));
		} catch (SQLException x) {
			_log.error("PG db con failed", x);
		}
		return _con;
	}

	/** Return the current connection to the database. */
	public Connection getConnection() {
		return _con;
	}

	/** * Disconnect from the database if already connected. */
	public void disconnect() throws Exception {
		if (_con != null && !_con.isClosed()) {
			_con.close();
			_log.info(String.format("> H2 con[%s]", _con.hashCode()));
		}
	}

	/* public static void main(String[] args) {
		PGCon postgresCon = null;
		Connection con = null;
		try {
			postgresCon = new PGCon();
			con = postgresCon.connect(PGDB.LOCALDB.URL);
			var stm = con.createStatement();
			var rs = stm.executeQuery("SELECT * from hello");
			if (rs.next()) {
				System.out.println(rs.getInt(1));
			}
			postgresCon.disconnect();
		} catch(Exception x) {
			x.printStackTrace();
		}
	} */


}


