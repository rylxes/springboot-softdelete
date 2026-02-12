package com.github.rylxes.softdelete;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

/**
 * JPA repository with Laravel-style soft-delete operations.
 * <p>
 * Extend this interface instead of {@link JpaRepository} to gain
 * soft-delete, restore, force-delete, and "trashed" query methods:
 * 
 * <pre>
 * public interface PostRepository extends SoftDeleteRepository&lt;Post, Long&gt; {
 * }
 * </pre>
 *
 * @param <T>  entity type (must implement {@link SoftDeletable})
 * @param <ID> primary key type
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T extends SoftDeletable, ID> extends JpaRepository<T, ID> {

    // ── Soft Delete ────────────────────────────────────────────────

    /**
     * Soft-delete the entity by setting its {@code deletedAt} timestamp.
     */
    void softDelete(T entity);

    /**
     * Soft-delete the entity with the given id.
     */
    void softDeleteById(ID id);

    // ── Restore ────────────────────────────────────────────────────

    /**
     * Restore a soft-deleted entity by clearing its {@code deletedAt} timestamp.
     */
    void restore(T entity);

    /**
     * Restore a soft-deleted entity by its id.
     */
    void restoreById(ID id);

    // ── Force Delete ───────────────────────────────────────────────

    /**
     * Permanently remove the entity from the database.
     */
    void forceDelete(T entity);

    /**
     * Permanently remove the entity with the given id.
     */
    void forceDeleteById(ID id);

    // ── Query Scopes ───────────────────────────────────────────────

    /**
     * Return <b>all</b> entities, including soft-deleted ones.
     * Equivalent to Laravel's {@code withTrashed()}.
     */
    List<T> findAllWithTrashed();

    /**
     * Find an entity by id, including soft-deleted entities.
     * Equivalent to Laravel's {@code withTrashed()->find(id)}.
     */
    Optional<T> findByIdWithTrashed(ID id);

    /**
     * Return <b>only</b> soft-deleted entities.
     * Equivalent to Laravel's {@code onlyTrashed()}.
     */
    List<T> findAllTrashed();

    /**
     * Count all entities including soft-deleted ones.
     */
    long countWithTrashed();

    /**
     * Count only soft-deleted entities.
     */
    long countTrashed();
}
