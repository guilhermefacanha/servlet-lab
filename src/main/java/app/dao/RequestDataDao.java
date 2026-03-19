package app.dao;

import app.entity.RequestData;
import lombok.extern.slf4j.Slf4j;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.Serializable;
import java.util.List;

@Slf4j
@Stateless
public class RequestDataDao implements Serializable {

    private static final long serialVersionUID = 4053248598231988138L;

    @PersistenceContext
    private EntityManager entityManager;

    public RequestData save(RequestData data) {
        log.info("=======  save .... ========");
        data =entityManager.merge(data);
        entityManager.merge(data);
        log.info("=======  save  finished========");
        return data;
    }

    public void deleteById(Long id) {
        log.info("=======  deleteById {} .... ========", id);
        RequestData entity = entityManager.find(RequestData.class, id);
        if (entity != null) {
            entityManager.remove(entity);
        }
        log.info("=======  deleteById {}  finished========", id);
    }

    public List<RequestData> getRequests() {
        log.info("=======  getRequests .... ========");
        List<RequestData> result = entityManager.createQuery("SELECT r FROM RequestData r order by r.id", RequestData.class)
                .setHint("org.hibernate.cacheable", true)
                .getResultList();
        log.info("=======  getRequests  finished========");
        return result;
    }

    public RequestData getRequestById(Long id) {
        log.info("=======  getRequestById {} .... ========", id);
        RequestData result = entityManager.find(RequestData.class, id);
        log.info("=======  getRequestById {}  finished========", id);
        return result;
    }

    public void clear() {
        log.info("=======  clear .... ========");
        entityManager.createQuery("DELETE FROM RequestData").executeUpdate();
        log.info("=======  clear  finished========");
    }

}
