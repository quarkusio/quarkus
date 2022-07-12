package io.quarkus.hibernate.orm.runtime.cdi;

import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.internal.FallbackBeanInstanceProducer;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;
import org.hibernate.resource.beans.spi.ManagedBean;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import io.quarkus.arc.Arc;

/**
 * A replacement for ManagedBeanRegistryImpl that:
 * <ul>
 * <li>forces the use of QuarkusManagedBeanRegistry,
 * which works with Arc and respects configured scopes when instantiating CDI beans.</li>
 * <li>is not stoppable and leaves the release of beans to {@link QuarkusArcBeanContainer},
 * so that the bean container and its beans can be reused between static init and runtime init,
 * even though we stop Hibernate ORM services after the end of static init.</li>
 * </ul>
 *
 * @see QuarkusArcBeanContainer
 */
public class QuarkusManagedBeanRegistry implements ManagedBeanRegistry {

    private final QuarkusArcBeanContainer beanContainer;

    public QuarkusManagedBeanRegistry() {
        this.beanContainer = Arc.container().instance(QuarkusArcBeanContainer.class).get();
    }

    @Override
    public <T> ManagedBean<T> getBean(Class<T> beanClass) {
        return getBean(beanClass, FallbackBeanInstanceProducer.INSTANCE);
    }

    @Override
    public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract) {
        return getBean(beanName, beanContract, FallbackBeanInstanceProducer.INSTANCE);
    }

    @Override
    public BeanContainer getBeanContainer() {
        return beanContainer;
    }

    @Override
    public <T> ManagedBean<T> getBean(Class<T> beanClass, BeanInstanceProducer fallbackBeanInstanceProducer) {
        return new ContainedBeanManagedBeanAdapter<>(beanClass,
                beanContainer.getBean(beanClass, QuarkusBeanContainerLifecycleOptions.INSTANCE,
                        fallbackBeanInstanceProducer));
    }

    @Override
    public <T> ManagedBean<T> getBean(String beanName, Class<T> beanContract,
            BeanInstanceProducer fallbackBeanInstanceProducer) {
        return new ContainedBeanManagedBeanAdapter<>(beanContract,
                beanContainer.getBean(beanName, beanContract, QuarkusBeanContainerLifecycleOptions.INSTANCE,
                        fallbackBeanInstanceProducer));
    }

    private static class ContainedBeanManagedBeanAdapter<B> implements ManagedBean<B> {
        private final Class<B> beanClass;
        private final ContainedBean<B> containedBean;

        private ContainedBeanManagedBeanAdapter(Class<B> beanClass, ContainedBean<B> containedBean) {
            this.beanClass = beanClass;
            this.containedBean = containedBean;
        }

        @Override
        public Class<B> getBeanClass() {
            return beanClass;
        }

        @Override
        public B getBeanInstance() {
            return containedBean.getBeanInstance();
        }
    }

    private static final class QuarkusBeanContainerLifecycleOptions implements BeanContainer.LifecycleOptions {
        private static final QuarkusBeanContainerLifecycleOptions INSTANCE = new QuarkusBeanContainerLifecycleOptions();

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

}
