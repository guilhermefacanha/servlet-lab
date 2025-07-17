package app.tenants.connection;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import app.entity.TenantConfig;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.hibernate.service.spi.ServiceRegistryAwareService;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import app.tenants.data.TenantConfigCache;
import app.tenants.resolver.MyTenantIdentifierResolver;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicMultiTenantConnectionProvider implements MultiTenantConnectionProvider, ServiceRegistryAwareService {

    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    private ServiceRegistryImplementor serviceRegistry; // For potential future use if needed

    private Map<String, TenantConfig> tenantConfigs = new ConcurrentHashMap<>();

    String dbHost = StringUtils.defaultIfBlank(System.getenv("DB_HOST"), "localhost");

    public DynamicMultiTenantConnectionProvider() {
        tenantConfigs.put(MyTenantIdentifierResolver.DEFAULT_TENANT_ID, new TenantConfig(MyTenantIdentifierResolver.DEFAULT_TENANT_ID, MyTenantIdentifierResolver.DEFAULT_TENANT_ID, "jdbc:postgresql://"+dbHost+":5432/servlet_tenants", "usuario", "senha"));
    }

    @Override
    public Connection getAnyConnection() throws SQLException {
        // This connection is used by Hibernate for operations that don't yet have a tenant context
        // (e.g., schema validation for the multi-tenant PU).
        // It should provide a connection to a 'default' or 'template' tenant database.
        System.out.println("Getting ANY connection for appDataPU (using default tenant template)");
        checkLoadedTenantConfigs();
        return getConnection(MyTenantIdentifierResolver.DEFAULT_TENANT_ID);
    }

    private void checkLoadedTenantConfigs() {
        if (tenantConfigs.isEmpty() || tenantConfigs.size() == 1) {
            tenantConfigs.putAll(TenantConfigCache.getAll());
            if (!StringUtils.equals("localhost", dbHost)) {
                tenantConfigs.replaceAll((tenantId, config) -> {
                    String url = config.getDbUrl();
                    if (StringUtils.contains(url, "localhost") ) {
                        url = url.replace("localhost", dbHost);
                        return new TenantConfig(
                                config.getTenantId(),
                                config.getNome(),
                                url,
                                config.getDbUsername(),
                                config.getDbPassword()
                        );
                    }
                    return config;
                });
            }
        }
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close(); // Return to pool
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        System.out.println("Getting connection for tenant: " + tenantIdentifier);
        checkLoadedTenantConfigs();
        DataSource dataSource = tenantDataSources.getOrDefault(tenantIdentifier, null);
        if (dataSource == null) {
            createAndCacheDataSource(tenantIdentifier, tenantConfigs.get(tenantIdentifier));
            dataSource = tenantDataSources.getOrDefault(tenantIdentifier, null);
        }
        return dataSource.getConnection();
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        connection.close(); // Return to pool
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false; // Typically false for pooled connections
    }

    public boolean isInjected() {
        return false; // Not managed by Hibernate's injection mechanism directly
    }

    @Override
    public void injectServices(ServiceRegistryImplementor serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        // At this point, you could potentially get other services from Hibernate
        // if needed, though for a connection provider it's often not necessary.
    }

    // Helper method to create a HikariCP DataSource
    private DataSource createAndCacheDataSource(String tenantId, TenantConfig details) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(details.getDbUrl());
        config.setUsername(details.getDbUsername());
        config.setPassword(details.getDbPassword());
        config.setDriverClassName("org.postgresql.Driver"); // Ensure correct driver
        config.setPoolName("HikariCP-" + tenantId);
        config.setMaximumPoolSize(10); // Adjust pool size per tenant as needed
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setConnectionTimeout(30000); // 30 seconds

        // Add any other HikariCP or PostgreSQL specific properties
        // For example: config.addDataSourceProperty("stringtype", "unspecified");

        HikariDataSource ds = new HikariDataSource(config);
        tenantDataSources.put(tenantId, ds);
        return ds;
    }

    @Override
    public boolean isUnwrappableAs(Class aClass) {
        return false;
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        if (clazz.isAssignableFrom(getClass())) {
            return clazz.cast(this);
        }
        return null;
    }

    // Don't forget to properly close your DataSources on application shutdown
    // You might need a @PreDestroy method in a CDI bean or a ServletContextListener
    public void shutdown() {
        System.out.println("Shutting down DynamicMultiTenantConnectionProvider...");
        tenantDataSources.values().forEach(ds -> {
            if (ds instanceof HikariDataSource) {
                ((HikariDataSource) ds).close();
                System.out.println("Closed HikariCP pool: " + ((HikariDataSource) ds).getPoolName());
            }
        });
        tenantDataSources.clear();
    }
}