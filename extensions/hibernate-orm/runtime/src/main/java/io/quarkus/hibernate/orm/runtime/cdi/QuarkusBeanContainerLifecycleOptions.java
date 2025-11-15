package io.quarkus.hibernate.orm.runtime.cdi;

import org.hibernate.resource.beans.container.spi.BeanContainer;

/**
 * An override of lifecycle options covering what can actually be supported in Quarkus.
 * <p>
 * Usually the {@link #DEFAULT} is used (when using {@link QuarkusManagedBeanRegistry}),
 * but in some cases Hibernate ORM calls the bean container directly and bypasses the registry,
 * so we need to override options in that case -- see {@link #of(BeanContainer.LifecycleOptions)}.
 */
final class QuarkusBeanContainerLifecycleOptions implements BeanContainer.LifecycleOptions {
    private static final QuarkusBeanContainerLifecycleOptions WITH_CACHE = new QuarkusBeanContainerLifecycleOptions(true);
    private static final QuarkusBeanContainerLifecycleOptions WITHOUT_CACHE = new QuarkusBeanContainerLifecycleOptions(false);

    // Caching is the default in Hibernate ORM, see ManagedBeanRegistryImpl#canUseCachedReferences.
    public static final QuarkusBeanContainerLifecycleOptions DEFAULT = WITH_CACHE;

    public static QuarkusBeanContainerLifecycleOptions of(BeanContainer.LifecycleOptions options) {
        if (options.canUseCachedReferences()) {
            return WITH_CACHE;
        } else {
            return WITHOUT_CACHE;
        }
    }

    private final boolean cache;

    private QuarkusBeanContainerLifecycleOptions(boolean cache) {
        this.cache = cache;
    }

    @Override
    public boolean useJpaCompliantCreation() {
        // Arc doesn't support all the BeanManager methods required to implement JPA-compliant bean creation.

        // In any case, JPA-compliant bean creation means we completely disregard the scope of beans
        // (e.g. @Dependent, @ApplicationScoped), which doesn't seem wise.
        // What we do instead in Quarkus is:
        // 1. Disable JPA-compliant creation, so we look up CDI beans and fall back to reflection if there is none.
        // 2. Add a default scope to relevant bean types -- see io.quarkus.hibernate.orm.deployment.HibernateOrmCdiProcessor.registerBeans
        // In effect, this gives us scope-compliant creation when classes are annotated with CDI scopes,
        // and spec-compliant creation when they are not.
        return false;
    }

    @Override
    public boolean canUseCachedReferences() {
        return cache;
    }
}
