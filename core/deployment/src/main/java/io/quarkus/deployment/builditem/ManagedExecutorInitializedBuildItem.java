package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * <p>
 * This build item can be injected in an extension {@link io.quarkus.deployment.annotations.BuildStep BuildStep} to make sure
 * the Quarkus {@link org.eclipse.microprofile.context.ManagedExecutor ManagedExecutor} instance is available for injection
 * during bytecode recording. The executor can only be used from generated bytecode executed at
 * {@link io.quarkus.deployment.annotations.ExecutionTime#RUNTIME_INIT ExecutionTime.RUNTIME_INIT}.
 * </p>
 * <p>
 * This build item will only be produced if the quarkus-smallrye-context-propagation extension is part of the project
 * dependencies. Therefore, it must be injected as an {@link java.util.Optional Optional} build item.
 * </p>
 * <p>
 * Example code:
 * 
 * <pre>
 * public class MyProcessor {
 *     {@literal @}BuildStep
 *     {@literal @}Record(ExecutionTime.RUNTIME_INIT)
 *     void buildStep({@code Optional<ManagedExecutorInitializedBuildItem>} managedExecutorInitialized,
 *             BeanContainerBuildItem beanContainer, MyRecorder myRecorder) {
 *         myRecorder.record(managedExecutorInitialized.isPresent(), beanContainer.value());
 *     }
 * }
 * 
 * {@literal @}Recorder
 * public class MyRecorder {
 *    public void record(boolean managedExecutorInitialized, BeanContainer beanContainer) {
 *        if (managedExecutorInitialized) {
 *            ManagedExecutor managedExecutor = beanContainer.instance(ManagedExecutor.class);
 *            // Use it...
 *        }
 *    }
 * }
 * </pre>
 * </p>
 * 
 * @deprecated This build item should not be needed anymore and will be removed at some point after Quarkus 1.7.
 */
@Deprecated
public final class ManagedExecutorInitializedBuildItem extends SimpleBuildItem {
}
