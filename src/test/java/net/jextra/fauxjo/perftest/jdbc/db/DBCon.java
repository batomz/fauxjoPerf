package net.jextra.fauxjo.perftest.jdbc.db;

import java.sql.Connection;

public interface DBCon {

	public Connection connect(String jdbcURI) throws Exception;
	public void disconnect() throws Exception;
	public Connection getConnection();

}
