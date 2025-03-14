package io.quarkus.hibernate.orm.runtime.cdi;

import org.hibernate.resource.beans.container.spi.BeanContainer;

final class QuarkusBeanContainerLifecycleOptions implements BeanContainer.LifecycleOptions {
    public static final QuarkusBeanContainerLifecycleOptions INSTANCE = new QuarkusBeanContainerLifecycleOptions();

    private QuarkusBeanContainerLifecycleOptions() {
    }

    @Override
    public boolean useJpaCompliantCreation() {
        // Arc doesn't support all the BeanManager methods required to implement JPA-compliant bean creation.
        // Anyway, JPA-compliant bean creation means we completely disregard the scope of beans
        // (e.g. @Dependent, @ApplicationScoped), which doesn't seem wise.
        // So we're probably better off this way.
        return false;
    }

    @Override
    public boolean canUseCachedReferences() {
        // Let Arc do the caching based on scopes
        return false;
    }
}
