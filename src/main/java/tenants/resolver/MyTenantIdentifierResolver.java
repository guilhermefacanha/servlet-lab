package tenants.resolver;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import tenants.context.TenantContext;

public class MyTenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    public static final String DEFAULT_TENANT_ID = "tenants_db"; // Fallback or initial tenant

    /**
     * This method is called by Hibernate to determine the current tenant identifier.
     * It should return the ID of the tenant associated with the current operation.
     */
    @Override
    public String resolveCurrentTenantIdentifier() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            System.out.println("Resolved tenant ID from context: " + tenantId);
            return tenantId;
        }
        // Fallback for operations that might not have a tenant context (e.g., schema generation)
        // or for your central 'tenants' database access.
        System.out.println("No tenant ID found in context, using default: " + DEFAULT_TENANT_ID);
        return DEFAULT_TENANT_ID;
    }

    /**
     * Indicates whether the current tenant identifier is "validated"
     * (i.e., whether its existence should be checked by Hibernate).
     * For 'DATASOURCE' strategy, it's typically true.
     */
    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}