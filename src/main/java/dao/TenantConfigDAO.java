package dao;

import annotations.CentralTenantDB;
import entity.TenantConfig;
import jakarta.enterprise.context.ApplicationScoped; // Or @RequestScoped
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.List;

@Slf4j
@ApplicationScoped
public class TenantConfigDAO implements Serializable {

    private static final long serialVersionUID = -760066567420064194L;

    @Inject
    @CentralTenantDB // Inject the EntityManager for the central DB
    private EntityManager em;

    public TenantConfig findByTenantId(String tenantId) {
        return em.find(TenantConfig.class, tenantId);
    }

    public List<TenantConfig> findAll() {
        log.info("=======  TenantConfig findAll .... ======");
        List<TenantConfig> selectTcFromTenantConfigTc = em.createQuery("SELECT tc FROM TenantConfig tc order by tc.nome", TenantConfig.class)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        log.info("=======  TenantConfig findAll finished .... ======");
        return selectTcFromTenantConfigTc;
    }

    public void save(TenantConfig config) {
        em.persist(config);
    }
    // ... other CRUD operations
}