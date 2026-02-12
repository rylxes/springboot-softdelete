package com.github.rylxes.softdelete.config;

import com.github.rylxes.softdelete.SoftDeletableEntity;
import com.github.rylxes.softdelete.SoftDeleteRepositoryFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration that:
 * <ol>
 * <li>Registers configuration properties for soft-delete customisation.</li>
 * <li>Component-scans the config package so the {@code SoftDeleteFilterAspect}
 * is picked up automatically.</li>
 * </ol>
 * <p>
 * <b>Consumers</b> must add
 * {@code @EnableJpaRepositories(repositoryFactoryBeanClass = SoftDeleteRepositoryFactoryBean.class)}
 * on their own {@code @Configuration} or {@code @SpringBootApplication} class
 * so
 * that Spring Data JPA knows to use the soft-delete repository implementation
 * for
 * any repository extending {@code SoftDeleteRepository}.
 */
@AutoConfiguration
@ConditionalOnClass(SoftDeletableEntity.class)
@EnableConfigurationProperties(SoftDeleteProperties.class)
@ComponentScan(basePackageClasses = SoftDeleteAutoConfiguration.class)
public class SoftDeleteAutoConfiguration {
}
