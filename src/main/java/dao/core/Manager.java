package dao.core;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

@RequestScoped
public class Manager {

    @PersistenceUnit(unitName = "requestDataPersistentUnit")
    private static EntityManagerFactory factory;

    @Produces
    @RequestScoped
    public EntityManager getEntityManager() {
        return factory.createEntityManager();
    }

    public void dispose(@Disposes EntityManager em) {
        em.close();
    }

}
