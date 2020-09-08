package io.quarkus.hibernate.orm.panache.deployment;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.objectweb.asm.ClassVisitor;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.visitors.PanacheRepositoryClassVisitor;

public class PanacheJpaRepositoryEnhancer extends PanacheRepositoryEnhancer {

    private static final DotName PANACHE_REPOSITORY_BINARY_NAME = DotName.createSimple(PanacheRepository.class.getName());
    private static final DotName PANACHE_REPOSITORY_BASE_BINARY_NAME = DotName
            .createSimple(PanacheRepositoryBase.class.getName());

    public PanacheJpaRepositoryEnhancer(IndexView index) {
        super(index, PanacheHibernateResourceProcessor.DOTNAME_PANACHE_REPOSITORY_BASE);
    }

    @Override
    public ClassVisitor apply(String className, ClassVisitor outputClassVisitor) {
        return new PanacheJpaRepositoryClassVisitor(className, outputClassVisitor,
                this.indexView);
    }

    static class PanacheJpaRepositoryClassVisitor extends PanacheRepositoryClassVisitor {

        public PanacheJpaRepositoryClassVisitor(String className, ClassVisitor outputClassVisitor, IndexView indexView) {
            super(className, outputClassVisitor, indexView);
        }

        @Override
        protected DotName getPanacheRepositoryDotName() {
            return PANACHE_REPOSITORY_BINARY_NAME;
        }

        @Override
        protected DotName getPanacheRepositoryBaseDotName() {
            return PANACHE_REPOSITORY_BASE_BINARY_NAME;
        }

        @Override
        protected String getPanacheOperationsInternalName() {
            return PanacheJpaEntityEnhancer.JPA_OPERATIONS_BINARY_NAME;
        }
    }
}
