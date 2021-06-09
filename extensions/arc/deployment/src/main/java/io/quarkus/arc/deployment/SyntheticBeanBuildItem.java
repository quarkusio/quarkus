package io.quarkus.arc.deployment;

import java.util.Set;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanConfiguratorBase;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;

/**
 * Makes it possible to register a synthetic bean.
 * <p>
 * Bean instances can be easily produced through a recorder and set via {@link ExtendedBeanConfigurator#supplier(Supplier)} and
 * {@link ExtendedBeanConfigurator#runtimeValue(RuntimeValue)}.
 * 
 * @see ExtendedBeanConfigurator
 * @see BeanRegistrar
 */
public final class SyntheticBeanBuildItem extends MultiBuildItem {

    /**
     * 
     * @param implClazz
     * @return a new configurator instance
     * @see ExtendedBeanConfigurator#done()
     */
    public static ExtendedBeanConfigurator configure(Class<?> implClazz) {
        return configure(DotName.createSimple(implClazz.getName()));
    }

    /**
     * 
     * @param implClazz
     * @return a new configurator instance
     * @see ExtendedBeanConfigurator#done()
     */
    public static ExtendedBeanConfigurator configure(DotName implClazz) {
        return new ExtendedBeanConfigurator(implClazz).addType(implClazz);
    }

    private final ExtendedBeanConfigurator configurator;

    SyntheticBeanBuildItem(ExtendedBeanConfigurator configurator) {
        this.configurator = configurator;
    }

    ExtendedBeanConfigurator configurator() {
        return configurator;
    }

    boolean isStaticInit() {
        return configurator.staticInit;
    }

    boolean hasRecorderInstance() {
        return configurator.supplier != null || configurator.runtimeValue != null;
    }

    /**
     * This construct is not thread-safe and should not be reused.
     */
    public static class ExtendedBeanConfigurator extends BeanConfiguratorBase<ExtendedBeanConfigurator, Object> {

        private Supplier<?> supplier;
        private RuntimeValue<?> runtimeValue;
        private boolean staticInit;

        ExtendedBeanConfigurator(DotName implClazz) {
            super(implClazz);
            this.staticInit = true;
        }

        /**
         * Finish the configurator.
         * 
         * @return a new build item
         */
        public SyntheticBeanBuildItem done() {
            if (supplier != null && runtimeValue != null) {
                throw new IllegalStateException("It is not possible to specify both - a supplier and a runtime value");
            }
            if (creatorConsumer == null && supplier == null && runtimeValue == null) {
                throw new IllegalStateException(
                        "Synthetic bean does not provide a creation method, use ExtendedBeanConfigurator#creator(), ExtendedBeanConfigurator#supplier() or ExtendedBeanConfigurator#runtimeValue()");
            }
            return new SyntheticBeanBuildItem(this);
        }

        public ExtendedBeanConfigurator supplier(Supplier<?> supplier) {
            this.supplier = supplier;
            return this;
        }

        public ExtendedBeanConfigurator runtimeValue(RuntimeValue<?> runtimeValue) {
            this.runtimeValue = runtimeValue;
            return this;
        }

        /**
         * A synthetic bean whose instance is produced through a recorder is initialized during
         * {@link ExecutionTime#STATIC_INIT} by default.
         * <p>
         * It is possible to change this behavior and initialize the bean during the {@link ExecutionTime#RUNTIME_INIT}.
         * However, in such case a client that attempts to obtain such bean during {@link ExecutionTime#STATIC_INIT} or before
         * runtime-init synthetic beans are
         * initialized will receive an exception.
         * <p>
         * {@link ExecutionTime#RUNTIME_INIT} build steps that access a runtime-init synthetic bean should consume the
         * {@link SyntheticBeansRuntimeInitBuildItem}.
         * 
         * @return self
         * @see SyntheticBeansRuntimeInitBuildItem
         */
        public ExtendedBeanConfigurator setRuntimeInit() {
            this.staticInit = false;
            return this;
        }

        DotName getImplClazz() {
            return implClazz;
        }

        Set<AnnotationInstance> getQualifiers() {
            return qualifiers;
        }

        @Override
        protected ExtendedBeanConfigurator self() {
            return this;
        }

        Supplier<?> getSupplier() {
            return supplier;
        }

        RuntimeValue<?> getRuntimeValue() {
            return runtimeValue;
        }
    }
}
