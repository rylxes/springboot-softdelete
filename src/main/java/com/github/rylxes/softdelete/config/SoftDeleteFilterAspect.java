package com.github.rylxes.softdelete.config;

import com.github.rylxes.softdelete.SoftDeletableEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that enables the Hibernate soft-delete filter before
 * any repository method executes. This ensures that standard JPA
 * queries (findAll, findById, etc.) automatically exclude
 * soft-deleted rows.
 * <p>
 * The filter is enabled at the Hibernate {@link Session} level,
 * so it stays active for the duration of the current transaction /
 * session, then is re-enabled on the next call.
 */
@Aspect
@Component
public class SoftDeleteFilterAspect {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Enable the soft-delete filter before any Spring Data repository method.
     */
    @Before("execution(* org.springframework.data.repository.Repository+.*(..))")
    public void enableSoftDeleteFilter() {
        try {
            Session session = entityManager.unwrap(Session.class);
            if (session.getEnabledFilter(SoftDeletableEntity.FILTER_NAME) == null) {
                session.enableFilter(SoftDeletableEntity.FILTER_NAME);
            }
        } catch (Exception e) {
            // Silently ignore — the filter may not apply to this entity or
            // the session may not be actively bound (e.g. outside a transaction).
        }
    }
}
