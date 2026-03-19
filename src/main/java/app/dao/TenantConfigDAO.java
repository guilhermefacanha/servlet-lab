package app.dao;

import app.entity.TenantConfig;
import com.sun.faces.util.CollectionsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

@Slf4j
@Stateless
public class TenantConfigDAO implements Serializable {

    private static final long serialVersionUID = -760066567420064194L;

    @PersistenceContext
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

    public String findTenantByHost(String host) {
        return CollectionUtils.emptyIfNull(findAll()).stream().filter(tc -> StringUtils.equals(tc.getHost(), host)).findFirst().map(TenantConfig::getTenantId).orElse(null);
    }
    // ... other CRUD operations
}