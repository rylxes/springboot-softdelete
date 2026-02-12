package com.github.rylxes.softdelete;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Minimal Spring Boot application for integration tests.
 * Demonstrates how a consumer configures the factory bean.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.github.rylxes.softdelete", repositoryFactoryBeanClass = SoftDeleteRepositoryFactoryBean.class)
public class TestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }
}
