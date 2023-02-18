package io.quarkus.arc.deployment;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanConfiguratorBase;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.runtime.RuntimeValue;

/**
 * Makes it possible to register a synthetic bean.
 * <p>
 * Bean instances can be easily produced through a recorder and set via {@link ExtendedBeanConfigurator#supplier(Supplier)},
 * {@link ExtendedBeanConfigurator#runtimeValue(RuntimeValue)} and {@link ExtendedBeanConfigurator#createWith(Function)}.
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
        return configurator.supplier != null || configurator.runtimeValue != null || configurator.fun != null;
    }

    /**
     * This construct is not thread-safe and should not be reused.
     */
    public static class ExtendedBeanConfigurator extends BeanConfiguratorBase<ExtendedBeanConfigurator, Object> {

        private Supplier<?> supplier;
        private RuntimeValue<?> runtimeValue;
        private Function<SyntheticCreationalContext<?>, ?> fun;
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
            if (supplier == null && runtimeValue == null && fun == null && creatorConsumer == null) {
                throw new IllegalStateException(
                        "Synthetic bean does not provide a creation method, use ExtendedBeanConfigurator#creator(), ExtendedBeanConfigurator#supplier(), ExtendedBeanConfigurator#createWith()  or ExtendedBeanConfigurator#runtimeValue()");
            }
            return new SyntheticBeanBuildItem(this);
        }

        /**
         * Use {@link #createWith(Function)} if you want to leverage build-time parameters or synthetic injection points.
         *
         * @param supplier A supplier returned from a recorder
         * @return self
         */
        public ExtendedBeanConfigurator supplier(Supplier<?> supplier) {
            if (runtimeValue != null || fun != null) {
                throw multipleCreationMethods();
            }
            this.supplier = Objects.requireNonNull(supplier);
            return this;
        }

        /**
         * Use {@link #createWith(Function)} if you want to leverage build-time parameters or synthetic injection points.
         *
         * @param runtimeValue A runtime value returned from a recorder
         * @return self
         */
        public ExtendedBeanConfigurator runtimeValue(RuntimeValue<?> runtimeValue) {
            if (supplier != null || fun != null) {
                throw multipleCreationMethods();
            }
            this.runtimeValue = Objects.requireNonNull(runtimeValue);
            return this;
        }

        /**
         * This method is useful if you need to use build-time parameters or synthetic injection points during creation of a
         * bean instance.
         *
         * @param fun A function returned from a recorder
         * @return self
         */
        public <B> ExtendedBeanConfigurator createWith(Function<SyntheticCreationalContext<B>, B> fun) {
            if (supplier != null || runtimeValue != null) {
                throw multipleCreationMethods();
            }
            this.fun = cast(Objects.requireNonNull(fun));
            return this;
        }

        /**
         * A synthetic bean whose instance is produced through a recorder is initialized during
         * {@link ExecutionTime#STATIC_INIT} by default.
         * <p>
         * It is possible to change this behavior and initialize the bean during the {@link ExecutionTime#RUNTIME_INIT}.
         * However, in such case a client that attempts to obtain such bean during {@link ExecutionTime#STATIC_INIT} or before
         * runtime-init synthetic beans are initialized will receive an exception.
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

        Supplier<?> getSupplier() {
            return supplier;
        }

        RuntimeValue<?> getRuntimeValue() {
            return runtimeValue;
        }

        Function<SyntheticCreationalContext<?>, ?> getFunction() {
            return fun;
        }

        private IllegalStateException multipleCreationMethods() {
            return new IllegalStateException("It is not possible to specify multiple creation methods");
        }

    }
}
