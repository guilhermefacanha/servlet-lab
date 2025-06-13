package dao;

import entity.RequestData;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Stateless
public class RequestDataDao {

    @PersistenceContext(unitName = "requestDataPU")
    private EntityManager entityManager;

    @Transactional
    public RequestData save(RequestData data) {
        log.info("=======  save .... ========");
        data =entityManager.merge(data);
        entityManager.merge(data);
        entityManager.flush();
        log.info("=======  save  finished========");
        return data;
    }

    @Transactional
    public void deleteById(Long id) {
        log.info("=======  deleteById .... ========");
        RequestData entity = entityManager.find(RequestData.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
        log.info("=======  deleteById  finished========");
    }

    public List<RequestData> getRequests() {
        log.info("=======  getRequests .... ========");
        List<RequestData> result = entityManager.createQuery("SELECT r FROM RequestData r", RequestData.class)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        log.info("=======  getRequests  finished========");
        return result;
    }

    public RequestData getRequestById(Long id) {
        log.info("=======  getRequestById .... ========");
        RequestData result = entityManager.find(RequestData.class, id);
        log.info("=======  getRequestById  finished========");
        return result;
    }

    @Transactional
    public void clear() {
        log.info("=======  clear .... ========");
        entityManager.createQuery("DELETE FROM RequestData").executeUpdate();
        log.info("=======  clear  finished========");
    }

}
