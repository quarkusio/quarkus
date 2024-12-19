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
        // We can only support these lifecycle options. See QuarkusBeanContainerLifecycleOptions.
        // Usually that's what we get passed (when using QuarkusManagedBeanRegistry),
        // but in some cases Hibernate ORM calls the bean cotnainer directly and bypasses the registry,
        // so we need to override options in that case.
        return super.getBean(beanType, QuarkusBeanContainerLifecycleOptions.INSTANCE, fallbackProducer);
    }

    @Override
    public <B> ContainedBean<B> getBean(String beanName, Class<B> beanType, LifecycleOptions lifecycleOptions,
            BeanInstanceProducer fallbackProducer) {
        // Overriding lifecycle options; see comments in the other getBean() method.
        return super.getBean(beanName, beanType, QuarkusBeanContainerLifecycleOptions.INSTANCE, fallbackProducer);
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
