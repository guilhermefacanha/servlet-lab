package app.tenants.connection;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.Stoppable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Dynamic schema-based MultiTenantConnectionProvider for Hibernate 5
 */
@Slf4j
public class DynamicMultiTenantConnectionProvider implements MultiTenantConnectionProvider, Stoppable {

    private static final long serialVersionUID = 1L;

    private DataSource dataSource;

    public DynamicMultiTenantConnectionProvider() {
        try {
            javax.naming.Context ctx = new javax.naming.InitialContext();
            this.dataSource = (DataSource) ctx.lookup("java:jboss/PostgresTenantDS");
        } catch (Exception e) {
            throw new RuntimeException("Failed to lookup DataSource via JNDI", e);
        }
    }

    public DynamicMultiTenantConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        log.debug("Getting connection for tenant '{}'", tenantIdentifier);
        final Connection connection = getAnyConnection();
        try {
            // Set schema for the tenant
            connection.setSchema(tenantIdentifier);
        } catch (SQLException e) {
            throw new SQLException("Could not alter JDBC connection to schema ["
                    + tenantIdentifier + "]", e);
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Reset to default schema if needed
            connection.setSchema("public");
        } catch (SQLException e) {
            // ignore, just close
        }
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    // Hibernate 5 SPI
    @Override
    public boolean isUnwrappableAs(Class unwrapType) {
        return MultiTenantConnectionProvider.class.equals(unwrapType)
                || DynamicMultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return (T) this;
        }
        throw new IllegalArgumentException("Unknown unwrap type " + unwrapType);
    }

    @Override
    public void stop() {
        // nothing to clean
    }
}
