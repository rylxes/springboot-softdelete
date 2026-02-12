package com.github.rylxes.softdelete.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the soft-delete library.
 * <p>
 * Set the global default column name in your {@code application.properties}:
 * 
 * <pre>
 * softdelete.column-name=removed_at
 * </pre>
 * 
 * Per-entity overrides via
 * {@link com.github.rylxes.softdelete.SoftDeleteColumn @SoftDeleteColumn}
 * take precedence over this global setting.
 */
@ConfigurationProperties(prefix = "softdelete")
public class SoftDeleteProperties {

    /**
     * The database column name used for the soft-delete timestamp.
     * Default: {@code deleted_at}.
     */
    private String columnName = "deleted_at";

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }
}
