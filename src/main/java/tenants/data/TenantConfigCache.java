package tenants.data;

import dao.TenantConfigDAO;
import entity.TenantConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class TenantConfigCache {

    @Inject
    private TenantConfigDAO tenantConfigDao;

    private static final Map<String, TenantConfig> tenantConfigMap = new ConcurrentHashMap<>();

    public void onApplicationStart(@Observes @Initialized(ApplicationScoped.class) ServletContext context) {
        System.out.println("Application Startup Event Observed: ServletContext initialized.");
    }

    @PostConstruct
    public void init() {
        System.out.println("TenantConfigCache: Initializing cache...");
        List<TenantConfig> configs = tenantConfigDao.findAll();
        for (TenantConfig config : configs) {
            tenantConfigMap.put(config.getTenantId(), config);
            System.out.println("TenantConfigCache: Cached tenant: " + config.getTenantId());
        }
        System.out.println("TenantConfigCache: Cache initialization complete. Total tenants: " + tenantConfigMap.size());
    }

    public static TenantConfig getTenantConfig(String tenantId) {
        return tenantConfigMap.get(tenantId);
    }

    public static Map<String, TenantConfig> getAll() {
        return tenantConfigMap;
    }
}