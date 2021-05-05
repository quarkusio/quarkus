package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Synthetic bean configurator. An alternative to {@link javax.enterprise.inject.spi.configurator.BeanConfigurator}.
 * <p>
 * This construct is not thread-safe and should not be re-used.
 * 
 * @param <T>
 */
public final class BeanConfigurator<T> extends BeanConfiguratorBase<BeanConfigurator<T>, T> {

    private final AtomicBoolean consumed;
    private final Consumer<BeanInfo> beanConsumer;
    private final BeanDeployment beanDeployment;

    /**
     *
     * @param implClassName
     * @param beanDeployment
     * @param beanConsumer
     */
    BeanConfigurator(DotName implClassName, BeanDeployment beanDeployment, Consumer<BeanInfo> beanConsumer) {
        super(implClassName);
        this.consumed = new AtomicBoolean(false);
        this.beanDeployment = beanDeployment;
        this.beanConsumer = beanConsumer;
    }

    @Override
    protected BeanConfigurator<T> self() {
        return this;
    }

    /**
     * Finish the configurator. The configurator should not be modified after this method is called.
     */
    public void done() {
        if (consumed.compareAndSet(false, true)) {
            ClassInfo implClass = getClassByName(beanDeployment.getBeanArchiveIndex(), Objects.requireNonNull(implClazz));
            if (implClass == null) {
                throw new IllegalStateException("Unable to find the bean class in the index: " + implClazz);
            }
            beanConsumer.accept(new BeanInfo.Builder()
                    .implClazz(implClass)
                    .providerType(providerType)
                    .beanDeployment(beanDeployment)
                    .scope(scope)
                    .types(types)
                    .qualifiers(qualifiers)
                    .alternativePriority(alternativePriority)
                    .name(name)
                    .creator(creatorConsumer)
                    .destroyer(destroyerConsumer)
                    .params(params)
                    .defaultBean(defaultBean)
                    .removable(removable)
                    .forceApplicationClass(forceApplicationClass)
                    .build());
        }
    }

}
