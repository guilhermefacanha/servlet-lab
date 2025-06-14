package interceptor;

import annotations.TransactionalResourceLocal;
import jakarta.inject.Inject;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

/**
 * Classe Interceptor de controle de transacao com banco de dados
 *
 * @author gfsolucoesti
 */
@Slf4j
@Interceptor
@TransactionalResourceLocal
public class TransactionInterceptor implements Serializable {

    private static final long serialVersionUID = 1L;

    private @Inject EntityManager manager;

    @AroundInvoke
    public Object invoke(InvocationContext context) throws Exception {
        log.info("TransactionInterceptor invoked for method: {}", context.getMethod().getName());
        EntityTransaction trx = manager.getTransaction();
        try {
            if (!trx.isActive()) {
                trx.begin();
            }
            return context.proceed();
        } catch (Exception e) {
            log.error(" error in transaction: {}", e.getMessage(), e);
            if (trx != null) {
                trx.rollback();
            }

            throw e;
        } finally {
            if (trx != null && trx.isActive()) {
                trx.commit();
                log.info(" Transaction committed successfully for method {}", context.getMethod().getName());
            }
        }
    }

}