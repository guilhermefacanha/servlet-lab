package app.dao;

import app.annotations.ApplicationTenantDB;
import app.annotations.CentralTenantDB;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import javax.enterprise.context.ApplicationScoped;
import app.tenants.context.TenantContext;

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