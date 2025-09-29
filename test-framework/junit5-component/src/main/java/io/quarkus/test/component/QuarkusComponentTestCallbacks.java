package io.quarkus.test.component;

import java.util.Set;

import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.IndexView;

import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.processor.BytecodeTransformer;

/**
 * This service provider can be used to contribute additional logic to {@link QuarkusComponentTestExtension}.
 * <p>
 * The implementations should be stateless. Callbacks are invoked in this order:
 * <ol>
 * <li>{@link #beforeIndex(BeforeIndexContext)}</li>
 * <li>{@link #beforeBuild(BeforeBuildContext)}</li>
 * <li>{@link #beforeStart(BeforeStartContext)}</li>
 * <li>{@link #afterStart(AfterStartContext)}</li>
 * <li>{@link #afterStop(AfterStopContext)}</li>
 * </ol>
 *
 * There are no other guarantees regarding instantiation, lifecycle and thread-safety.
 */
public interface QuarkusComponentTestCallbacks {

    /**
     * Called before the bean archive index is built.
     * <p>
     * The bean archive index is built before the container is built.
     *
     * @param beforeIndexContext
     */
    default void beforeIndex(BeforeIndexContext beforeIndexContext) {
        // No-op
    }

    /**
     * Called before the container is built.
     * <p>
     * The container is built before the test class is loaded.
     *
     * @param beforeBuildContext
     */
    default void beforeBuild(BeforeBuildContext beforeBuildContext) {
        // No-op
    }

    /**
     * Called before the container is started.
     * <p>
     * If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD} is used (default) then the container is started during
     * the {@code before each} test phase. If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS} is used
     * then the container is started during the {@code before all} test phase.
     *
     * @param beforeStartContext
     */
    default void beforeStart(BeforeStartContext beforeStartContext) {
        // No-op
    }

    /**
     * Called after the container is started.
     * <p>
     * If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD} is used (default) then the container is started during
     * the {@code before each} test phase. If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS} is used
     * then the container is started during the {@code before all} test phase.
     *
     * @param afterStartContext
     */
    default void afterStart(AfterStartContext afterStartContext) {
        // No-op
    }

    /**
     * Called after the container is stopped.
     * <p>
     * If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_METHOD} is used (default) then the container is stopped during
     * the {@code after each} test phase. If {@link org.junit.jupiter.api.TestInstance.Lifecycle#PER_CLASS} is used then the
     * container is during the {@code after all} test phase.
     *
     * @param afterStopContext
     */
    default void afterStop(AfterStopContext afterStopContext) {
        // No-op
    }

    interface BeforeIndexContext extends ComponentTestContext {

        /**
         * @return the immutable set of original component classes
         */
        Set<Class<?>> getComponentClasses();

        void addComponentClass(Class<?> componentClass);

    }

    interface BeforeBuildContext extends ComponentTestContext {

        IndexView getImmutableBeanArchiveIndex();

        IndexView getComputingBeanArchiveIndex();

        void addAnnotationTransformation(AnnotationTransformation transformation);

        void addBeanRegistrar(BeanRegistrar beanRegistrar);

        void addBytecodeTransformer(BytecodeTransformer bytecodeTransformer);

    }

    interface BeforeStartContext extends ComponentTestContext {

        /**
         * Set the value of a configuration property.
         * <p>
         * Overrides values set by other means, including {@link io.quarkus.test.component.TestConfigProperty}.
         *
         * @param key
         * @param value
         */
        void setConfigProperty(String key, String value);

    }

    interface AfterStartContext extends ComponentTestContext {
    }

    interface AfterStopContext extends ComponentTestContext {
    }

    interface ComponentTestContext {

        Class<?> getTestClass();

    }

}
