package io.quarkus.arc.deployment;

import java.util.Set;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BeanConfiguratorBase;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;

/**
 * Makes it possible to register a synthetic bean whose instance can be easily produced through a recorder.
 * 
 * @see BeanConfigurator
 * @see ExtendedBeanConfigurator#setRuntimeInit()
 */
public final class SyntheticBeanBuildItem extends MultiBuildItem {

    public static ExtendedBeanConfigurator configure(Class<?> implClazz) {
        return configure(DotName.createSimple(implClazz.getName()));
    }

    public static ExtendedBeanConfigurator configure(DotName implClazz) {
        return new ExtendedBeanConfigurator(implClazz).addType(implClazz);
    }

    private final ExtendedBeanConfigurator configurator;

    SyntheticBeanBuildItem(ExtendedBeanConfigurator configurator) {
        this.configurator = configurator;
    }

    public ExtendedBeanConfigurator configurator() {
        return configurator;
    }

    public boolean isStaticInit() {
        return configurator.staticInit;
    }

    /**
     * This construct is not thread-safe and should not be reused.
     */
    public static class ExtendedBeanConfigurator extends BeanConfiguratorBase<ExtendedBeanConfigurator, Object> {

        Supplier<?> supplier;
        RuntimeValue<?> runtimeValue;
        boolean staticInit;

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
            if (supplier == null && runtimeValue == null) {
                throw new IllegalStateException("Either a supplier or a runtime value must be set");
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
         * By default, synthetic beans are initialized during {@link ExecutionTime#STATIC_INIT}. It is possible to mark a
         * synthetic bean to be initialized during {@link ExecutionTime#RUNTIME_INIT}. However, in such case a client that
         * attempts to obtain such bean during {@link ExecutionTime#STATIC_INIT} or before runtime-init synthetic beans are
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

        public DotName getImplClazz() {
            return implClazz;
        }

        public Set<AnnotationInstance> getQualifiers() {
            return qualifiers;
        }

        @Override
        protected ExtendedBeanConfigurator self() {
            return this;
        }

    }
}
