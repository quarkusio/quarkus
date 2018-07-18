package org.hibernate.protean.nativeprocessing;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;

/**
 * We don't really need a connection during native-image processing, but starting the default
 * ConnectionProvider starts threads - which we need to avoid.
 */
public final class NoConnectionProvider implements ConnectionProvider {

	public static void registerToProperties(Map configuration) {
		configuration.put( AvailableSettings.CONNECTION_PROVIDER, new NoConnectionProvider() );
	}

	private NoConnectionProvider() {
		//do not create
	}

	@Override
	public Connection getConnection() throws SQLException {
		throw new IllegalStateException( "guess what, the NoConnectionProvider does provide No Connection" );
	}

	@Override
	public void closeConnection(Connection conn) throws SQLException {
		//hum, whatever?
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class unwrapType) {
		return false;
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return null;
	}
}
