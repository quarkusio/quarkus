package io.quarkus.arc.deployment;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.ActiveResult;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanConfiguratorBase;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.recording.BytecodeRecorderImpl.ReturnedProxy;
import io.quarkus.runtime.RuntimeValue;

/**
 * Makes it possible to register a synthetic bean.
 * <p>
 * Bean instances can be easily produced through a recorder and set via {@link ExtendedBeanConfigurator#supplier(Supplier)},
 * {@link ExtendedBeanConfigurator#runtimeValue(RuntimeValue)}, {@link ExtendedBeanConfigurator#createWith(Function)} and
 * {@link ExtendedBeanConfigurator#runtimeProxy(Object)}.
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
        return configurator.supplier != null || configurator.runtimeValue != null || configurator.fun != null
                || configurator.runtimeProxy != null;
    }

    boolean hasCheckActiveSupplier() {
        return configurator.checkActive != null;
    }

    /**
     * This construct is not thread-safe and should not be reused.
     */
    public static class ExtendedBeanConfigurator extends BeanConfiguratorBase<ExtendedBeanConfigurator, Object> {

        private Object runtimeProxy;
        private Supplier<?> supplier;
        private RuntimeValue<?> runtimeValue;
        private Function<SyntheticCreationalContext<?>, ?> fun;
        private boolean staticInit;

        private Supplier<ActiveResult> checkActive;

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
            if (supplier == null && runtimeValue == null && fun == null && runtimeProxy == null && creatorConsumer == null) {
                throw new IllegalStateException(
                        "Synthetic bean does not provide a creation method, use ExtendedBeanConfigurator#creator(), ExtendedBeanConfigurator#supplier(), ExtendedBeanConfigurator#createWith() or ExtendedBeanConfigurator#runtimeValue()");
            }
            if (checkActive != null && supplier == null && runtimeValue == null && fun == null && runtimeProxy == null) {
                // "check active" procedure is set via recorder proxy,
                // creation function must also be set via recorder proxy
                throw new IllegalStateException(
                        "Synthetic bean has ExtendedBeanConfigurator#checkActive(), but does not have ExtendedBeanConfigurator#supplier() / createWith() / runtimeValue() / runtimeProxy()");
            }
            return new SyntheticBeanBuildItem(this);
        }

        /**
         * The contextual bean instance is supplied by a proxy returned from a recorder method.
         * <p>
         * Use {@link #createWith(Function)} if you want to leverage build-time parameters or synthetic injection points.
         *
         * @param supplier A supplier returned from a recorder method
         * @return self
         * @throws IllegalArgumentException If the supplier argument is not a proxy returned from a recorder method
         */
        public ExtendedBeanConfigurator supplier(Supplier<?> supplier) {
            checkReturnedProxy(supplier);
            checkMultipleCreationMethods();
            this.supplier = Objects.requireNonNull(supplier);
            return this;
        }

        /**
         * The contextual bean instance is a proxy returned from a recorder method.
         * <p>
         * Use {@link #createWith(Function)} if you want to leverage build-time parameters or synthetic injection points.
         *
         * @param runtimeValue A runtime value returned from a recorder method
         * @return self
         * @throws IllegalArgumentException If the runtimeValue argument is not a proxy returned from a recorder method
         */
        public ExtendedBeanConfigurator runtimeValue(RuntimeValue<?> runtimeValue) {
            checkReturnedProxy(runtimeValue);
            checkMultipleCreationMethods();
            this.runtimeValue = Objects.requireNonNull(runtimeValue);
            return this;
        }

        /**
         * The contextual bean instance is created by a proxy returned from a recorder method.
         * <p>
         * This method is useful if you need to use build-time parameters or synthetic injection points during creation of a
         * bean instance.
         *
         * @param function A function returned from a recorder method
         * @return self
         * @throws IllegalArgumentException If the function argument is not a proxy returned from a recorder method
         */
        public <B> ExtendedBeanConfigurator createWith(Function<SyntheticCreationalContext<B>, B> function) {
            checkReturnedProxy(function);
            checkMultipleCreationMethods();
            this.fun = cast(Objects.requireNonNull(function));
            return this;
        }

        /**
         * The contextual bean instance is a proxy returned from a recorder method.
         * <p>
         * Use {@link #createWith(Function)} if you want to leverage build-time parameters or synthetic injection points.
         *
         * @param proxy A proxy returned from a recorder method
         * @return self
         * @throws IllegalArgumentException If the proxy argument is not a proxy returned from a recorder method
         */
        public ExtendedBeanConfigurator runtimeProxy(Object proxy) {
            checkReturnedProxy(proxy);
            checkMultipleCreationMethods();
            this.runtimeProxy = Objects.requireNonNull(proxy);
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

        /**
         * The {@link #checkActive(Consumer)} procedure is a {@code Supplier<ActiveResult>} proxy
         * returned from a recorder method.
         *
         * @param checkActive a {@code Supplier<ActiveResult>} returned from a recorder method
         * @return self
         * @throws IllegalArgumentException if the {@code checkActive} argument is not a proxy returned from a recorder method
         */
        public ExtendedBeanConfigurator checkActive(Supplier<ActiveResult> checkActive) {
            checkReturnedProxy(checkActive);
            this.checkActive = Objects.requireNonNull(checkActive);
            return this;
        }

        DotName getImplClazz() {
            return implClazz;
        }

        Set<Type> getTypes() {
            return types;
        }

        Set<AnnotationInstance> getQualifiers() {
            return qualifiers;
        }

        String getIdentifier() {
            return identifier;
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

        Object getRuntimeProxy() {
            return runtimeProxy;
        }

        Supplier<ActiveResult> getCheckActive() {
            return checkActive;
        }

        private void checkMultipleCreationMethods() {
            if (runtimeProxy == null && runtimeValue == null && supplier == null && fun == null) {
                return;
            }
            throw new IllegalStateException("It is not possible to specify multiple creation methods");
        }

        private void checkReturnedProxy(Object object) {
            if (object instanceof ReturnedProxy) {
                return;
            }
            throw new IllegalArgumentException(
                    "The object is not a proxy returned from a recorder method: " + object.toString());
        }

    }
}
