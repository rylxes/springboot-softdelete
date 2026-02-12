package com.github.rylxes.softdelete;

import java.time.Instant;

/**
 * Contract for any entity that supports soft deletion.
 * <p>
 * Implement this interface directly if you don't want to extend
 * {@link SoftDeletableEntity}. Otherwise, simply extend the base class.
 */
public interface SoftDeletable {

    Instant getDeletedAt();

    void setDeletedAt(Instant deletedAt);

    /**
     * Returns {@code true} if this entity has been soft-deleted.
     */
    default boolean isDeleted() {
        return getDeletedAt() != null;
    }
}
