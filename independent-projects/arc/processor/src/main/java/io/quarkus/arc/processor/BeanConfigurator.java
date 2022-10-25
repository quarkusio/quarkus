package io.quarkus.arc.processor;

import static io.quarkus.arc.processor.IndexClassLookupUtils.getClassByName;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Synthetic bean configurator. An alternative to {@link jakarta.enterprise.inject.spi.configurator.BeanConfigurator}.
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

    /**
     * Finish the configurator. The configurator should not be modified after this method is called.
     */
    public void done() {
        if (consumed.compareAndSet(false, true)) {
            ClassInfo implClass = getClassByName(beanDeployment.getBeanArchiveIndex(), Objects.requireNonNull(implClazz));
            if (implClass == null) {
                throw new IllegalStateException("Unable to find the bean class in the index: " + implClazz);
            }

            ScopeInfo scope = this.scope;
            if (scope == null) {
                scope = Beans.initStereotypeScope(stereotypes, implClass, beanDeployment);
            }
            if (scope == null) {
                scope = BuiltinScope.DEPENDENT.getInfo();
            }

            String name = this.name;
            if (name == null) {
                name = Beans.initStereotypeName(stereotypes, implClass);
            }

            Boolean alternative = this.alternative;
            if (alternative == null) {
                alternative = Beans.initStereotypeAlternative(stereotypes);
            }

            Integer priority = this.priority;
            if (priority == null) {
                priority = Beans.initStereotypeAlternativePriority(stereotypes);
            }

            beanConsumer.accept(new BeanInfo.Builder()
                    .implClazz(implClass)
                    .providerType(providerType)
                    .beanDeployment(beanDeployment)
                    .scope(scope)
                    .types(types)
                    .qualifiers(qualifiers)
                    .alternative(alternative)
                    .priority(priority)
                    .stereotypes(stereotypes)
                    .name(name)
                    .creator(creatorConsumer)
                    .destroyer(destroyerConsumer)
                    .params(params)
                    .defaultBean(defaultBean)
                    .removable(removable)
                    .forceApplicationClass(forceApplicationClass)
                    .targetPackageName(targetPackageName)
                    .build());
        }
    }

}
