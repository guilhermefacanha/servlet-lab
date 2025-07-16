package dao;

import annotations.ApplicationTenantDB;
import annotations.CentralTenantDB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import jakarta.enterprise.context.ApplicationScoped;
import tenants.context.TenantContext;

@ApplicationScoped
public class Manager {

    // EntityManagerFactory for the CENTRAL Tenant Configuration DB
    @PersistenceUnit(unitName = "centralTenantPU")
    private EntityManagerFactory centralTenantFactory;

    // EntityManagerFactory for the MULTI-TENANT Application DBs
    @PersistenceUnit(unitName = "appDataPU")
    private EntityManagerFactory appDataFactory;

    // --- Producers for Central Tenant DB EntityManager ---
    @Produces
    @RequestScoped // Or @Dependent, depending on your CDI scope preference for this.
    @CentralTenantDB // A custom CDI qualifier to distinguish this EntityManager
    public EntityManager getCentralTenantEntityManager() {
        System.out.println("Producing EntityManager for centralTenantPU.");
        return centralTenantFactory.createEntityManager();
    }

    public void disposeCentralTenantEntityManager(@Disposes @CentralTenantDB EntityManager em) {
        if (em.isOpen()) {
            em.close();
            System.out.println("Disposed centralTenantPU EntityManager.");
        }
    }

    // --- Producers for Multi-Tenant Application DB EntityManager ---
    @Produces
    @RequestScoped
    @ApplicationTenantDB // Another custom CDI qualifier
    public EntityManager getApplicationEntityManager() {
        System.out.println("Producing EntityManager for appDataPU for tenant: " + TenantContext.getTenantId());
        return appDataFactory.createEntityManager();
    }

    public void disposeApplicationEntityManager(@Disposes @ApplicationTenantDB EntityManager em) {
        if (em.isOpen()) {
            em.close();
            System.out.println("Disposed appDataPU EntityManager.");
        }
        // CRITICAL: Clear the ThreadLocal after the request is complete
        TenantContext.clear();
        System.out.println("Cleared TenantContext.");
    }
}