package com.github.rylxes.softdelete;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override the soft-delete column name for a specific entity.
 * <p>
 * By default, the column is {@code deleted_at}. Place this annotation
 * on your entity class to use a different column name:
 * 
 * <pre>
 * {@literal @}Entity
 * {@literal @}SoftDeleteColumn("removed_at")
 * public class Invoice extends SoftDeletableEntity { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SoftDeleteColumn {

    /**
     * The database column name to use for the soft-delete timestamp.
     */
    String value();
}
