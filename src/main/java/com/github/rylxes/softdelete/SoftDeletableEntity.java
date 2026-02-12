package com.github.rylxes.softdelete;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;

import java.time.Instant;

/**
 * Base mapped superclass that adds soft-delete support to any JPA entity.
 * <p>
 * Extend this class to opt into soft deletion — analogous to using
 * Laravel's {@code SoftDeletes} trait:
 * 
 * <pre>
 * {@literal @}Entity
 * public class Post extends SoftDeletableEntity {
 *     private String title;
 *     // ...
 * }
 * </pre>
 * <p>
 * The default column name is {@code deleted_at}. Override it per-entity
 * with {@link SoftDeleteColumn @SoftDeleteColumn("custom_col")} or
 * globally via {@code softdelete.column-name} in your properties.
 * <p>
 * When the Hibernate filter is enabled (done automatically by the
 * auto-configuration), all standard queries will exclude rows where
 * the soft-delete column is not null.
 */
@MappedSuperclass
@FilterDef(name = SoftDeletableEntity.FILTER_NAME, defaultCondition = "deleted_at IS NULL")
@Filter(name = SoftDeletableEntity.FILTER_NAME)
public abstract class SoftDeletableEntity implements SoftDeletable {

    public static final String FILTER_NAME = "softDeleteFilter";

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Override
    public Instant getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
