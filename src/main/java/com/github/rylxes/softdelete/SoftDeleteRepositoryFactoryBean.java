package com.github.rylxes.softdelete;

import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import java.io.Serializable;

/**
 * Custom factory bean that tells Spring Data JPA to use
 * {@link SoftDeleteRepositoryImpl} as the base class for any
 * repository that extends {@link SoftDeleteRepository}.
 */
public class SoftDeleteRepositoryFactoryBean<R extends JpaRepository<T, ID>, T, ID extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, ID> {

    public SoftDeleteRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new SoftDeleteRepositoryFactory(entityManager);
    }

    private static class SoftDeleteRepositoryFactory extends JpaRepositoryFactory {

        private final EntityManager entityManager;

        public SoftDeleteRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
            this.entityManager = entityManager;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected JpaRepositoryImplementation<?, ?> getTargetRepository(
                RepositoryInformation information, EntityManager em) {

            // Only use our custom impl for repos that extend SoftDeleteRepository
            if (SoftDeleteRepository.class.isAssignableFrom(information.getRepositoryInterface())) {
                JpaEntityInformation<?, ?> entityInformation = getEntityInformation(information.getDomainType());
                return new SoftDeleteRepositoryImpl(entityInformation, em);
            }

            return super.getTargetRepository(information, em);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            if (SoftDeleteRepository.class.isAssignableFrom(metadata.getRepositoryInterface())) {
                return SoftDeleteRepositoryImpl.class;
            }
            return super.getRepositoryBaseClass(metadata);
        }
    }
}
