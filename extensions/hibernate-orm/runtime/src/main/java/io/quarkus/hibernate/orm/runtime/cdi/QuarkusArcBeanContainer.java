package io.quarkus.hibernate.orm.runtime.cdi;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.hibernate.resource.beans.container.spi.AbstractCdiBeanContainer;
import org.hibernate.resource.beans.container.spi.BeanLifecycleStrategy;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.container.spi.ContainedBeanImplementor;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

/**
 * A {@link org.hibernate.resource.beans.container.spi.BeanContainer} that works with other build-time elements in Quarkus to
 * achieve a behavior that is as unsurprising as possible, while complying with the Jakarta Persistence spec where possible.
 * The effective behavior is as follows:
 * <ol>
 * <li>When a class is annotated with a scope, such as <code>@ApplicationScoped</code> or <code>@Dependent</code>, we will
 * comply with that (with some gotchas, see last item).
 * This is not in line with the behavior from the Jakarta Persitence spec, which would ignore explicit scopes.
 * <li>When a class is NOT annotated with any scope, it will behave as a <code>@Dependent</code> CDI bean,
 * at least for attribute converters and entity listeners (with some gotchas, see last item).
 * This is in line with what the Jakarta Persistence spec requires.
 * <li>Regardless of the above, when a class is annotated with @Vetoed, it will never be instantiated through CDI, but rather
 * through Hibernate ORM's reflection instantiation.
 * This may or may not be in line with the behavior described in the Jakarta Persistence spec -- I did not check -- but
 * seems sensible and intuitive enough.
 * <li>Hibernate ORM's internal behavior means components may be cached at the persistence unit level,
 * so even for <code>@Dependent</code> components, there would be at most one instance per persistence unit.
 * TODO: this is what we've always done and what we assume in tests, but is this what we want?
 * <p>
 * Note this behavior is only possible because we give attribute converters and entity listeners the dependent scope by default:
 * see {@code io.quarkus.hibernate.orm.deployment.HibernateOrmCdiProcessor#registerBeans}.
 */
@Singleton
public class QuarkusArcBeanContainer extends AbstractCdiBeanContainer {

    @Inject
    BeanManager beanManager;

    @Override
    public BeanManager getUsableBeanManager() {
        return beanManager;
    }

    @Override
    public <B> ContainedBean<B> getBean(Class<B> beanType, LifecycleOptions lifecycleOptions,
            BeanInstanceProducer fallbackProducer) {
        // Overriding lifecycle options; see QuarkusBeanContainerLifecycleOptions.
        return super.getBean(beanType, QuarkusBeanContainerLifecycleOptions.of(lifecycleOptions), fallbackProducer);
    }

    @Override
    public <B> ContainedBean<B> getBean(String beanName, Class<B> beanType, LifecycleOptions lifecycleOptions,
            BeanInstanceProducer fallbackProducer) {
        // Overriding lifecycle options; see QuarkusBeanContainerLifecycleOptions.
        return super.getBean(beanName, beanType, QuarkusBeanContainerLifecycleOptions.of(lifecycleOptions), fallbackProducer);
    }

    @Override
    protected <B> ContainedBeanImplementor<B> createBean(Class<B> beanType,
            BeanLifecycleStrategy lifecycleStrategy, BeanInstanceProducer fallbackProducer) {
        ContainedBeanImplementor<B> bean = lifecycleStrategy.createBean(beanType, fallbackProducer, this);
        bean.initialize();
        return bean;
    }

    @Override
    protected <B> ContainedBeanImplementor<B> createBean(String name, Class<B> beanType,
            BeanLifecycleStrategy lifecycleStrategy, BeanInstanceProducer fallbackProducer) {
        ContainedBeanImplementor<B> bean = lifecycleStrategy.createBean(name, beanType,
                fallbackProducer, this);
        bean.initialize();
        return bean;
    }

    /**
     * This will happen after Hibernate ORM is stopped;
     * see io.quarkus.hibernate.orm.runtime.JPAConfig#destroy(java.lang.Object).
     */
    @PreDestroy
    public void destroy() {
        stop();
    }
}
