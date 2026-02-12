package com.github.rylxes.softdelete;

import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link SoftDeleteRepository}.
 * <p>
 * Overrides standard JPA query methods to enable the Hibernate
 * soft-delete filter, so soft-deleted rows are automatically excluded.
 * Temporarily disables the filter for "withTrashed" and "onlyTrashed" queries.
 */
public class SoftDeleteRepositoryImpl<T extends SoftDeletable, ID>
        extends SimpleJpaRepository<T, ID>
        implements SoftDeleteRepository<T, ID> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public SoftDeleteRepositoryImpl(JpaEntityInformation<T, ?> entityInformation,
            EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    // ── Filter helpers ─────────────────────────────────────────

    private Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    private void enableFilter() {
        Session session = getSession();
        if (session.getEnabledFilter(SoftDeletableEntity.FILTER_NAME) == null) {
            session.enableFilter(SoftDeletableEntity.FILTER_NAME);
        }
    }

    private void disableFilter() {
        Session session = getSession();
        if (session.getEnabledFilter(SoftDeletableEntity.FILTER_NAME) != null) {
            session.disableFilter(SoftDeletableEntity.FILTER_NAME);
        }
    }

    // ── Override standard query methods to auto-filter ──────────

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll() {
        enableFilter();
        return super.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAll(Sort sort) {
        enableFilter();
        return super.findAll(sort);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<T> findAll(Pageable pageable) {
        enableFilter();
        return super.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllById(Iterable<ID> ids) {
        enableFilter();
        return super.findAllById(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findById(ID id) {
        enableFilter();
        // Use JPQL instead of EntityManager.find() because Hibernate filters
        // only apply to queries, not direct entity lookups.
        String jpql = "SELECT e FROM " + entityInformation.getEntityName()
                + " e WHERE e." + entityInformation.getIdAttribute().getName() + " = :id";
        List<T> results = entityManager.createQuery(jpql, entityInformation.getJavaType())
                .setParameter("id", id)
                .getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        enableFilter();
        return super.count();
    }

    @Override
    @Transactional(readOnly = true)
    public <S extends T> List<S> findAll(Example<S> example) {
        enableFilter();
        return super.findAll(example);
    }

    @Override
    @Transactional(readOnly = true)
    public <S extends T> List<S> findAll(Example<S> example, Sort sort) {
        enableFilter();
        return super.findAll(example, sort);
    }

    @Override
    @Transactional(readOnly = true)
    public <S extends T> Page<S> findAll(Example<S> example, Pageable pageable) {
        enableFilter();
        return super.findAll(example, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public <S extends T> long count(Example<S> example) {
        enableFilter();
        return super.count(example);
    }

    @Override
    @Transactional(readOnly = true)
    public <S extends T> boolean exists(Example<S> example) {
        enableFilter();
        return super.exists(example);
    }

    // ── Override delete to soft-delete by default ───────────────

    @Override
    @Transactional
    public void delete(T entity) {
        softDelete(entity);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        softDeleteById(id);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {
        entities.forEach(this::softDelete);
    }

    @Override
    @Transactional
    public void deleteAll() {
        findAll().forEach(this::softDelete);
    }

    // ── Soft Delete ────────────────────────────────────────────

    @Override
    @Transactional
    public void softDelete(T entity) {
        entity.setDeletedAt(Instant.now());
        entityManager.merge(entity);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void softDeleteById(ID id) {
        disableFilter();
        try {
            findById(id).ifPresent(this::softDelete);
        } finally {
            enableFilter();
        }
    }

    // ── Restore ────────────────────────────────────────────────

    @Override
    @Transactional
    public void restore(T entity) {
        entity.setDeletedAt(null);
        entityManager.merge(entity);
        entityManager.flush();
    }

    @Override
    @Transactional
    public void restoreById(ID id) {
        findByIdWithTrashed(id).ifPresent(this::restore);
    }

    // ── Force Delete ───────────────────────────────────────────

    @Override
    @Transactional
    public void forceDelete(T entity) {
        disableFilter();
        try {
            T managed = entityManager.contains(entity)
                    ? entity
                    : entityManager.merge(entity);
            entityManager.remove(managed);
            entityManager.flush();
        } finally {
            enableFilter();
        }
    }

    @Override
    @Transactional
    public void forceDeleteById(ID id) {
        disableFilter();
        try {
            super.findById(id).ifPresent(e -> {
                entityManager.remove(entityManager.contains(e) ? e : entityManager.merge(e));
                entityManager.flush();
            });
        } finally {
            enableFilter();
        }
    }

    // ── Query Scopes ───────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllWithTrashed() {
        disableFilter();
        try {
            return super.findAll();
        } finally {
            enableFilter();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<T> findByIdWithTrashed(ID id) {
        disableFilter();
        try {
            return super.findById(id);
        } finally {
            enableFilter();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<T> findAllTrashed() {
        disableFilter();
        try {
            String jpql = "SELECT e FROM " + entityInformation.getEntityName()
                    + " e WHERE e.deletedAt IS NOT NULL";
            return entityManager.createQuery(jpql, entityInformation.getJavaType())
                    .getResultList();
        } finally {
            enableFilter();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countWithTrashed() {
        disableFilter();
        try {
            return super.count();
        } finally {
            enableFilter();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countTrashed() {
        disableFilter();
        try {
            String jpql = "SELECT COUNT(e) FROM " + entityInformation.getEntityName()
                    + " e WHERE e.deletedAt IS NOT NULL";
            return entityManager.createQuery(jpql, Long.class)
                    .getSingleResult();
        } finally {
            enableFilter();
        }
    }
}
