package io.quarkus.core.deployment.action.impl;

import java.lang.constant.MethodTypeDesc;
import java.util.List;

import io.smallrye.classfile.ClassModel;
import io.smallrye.classfile.CodeModel;

/**
 * The result of extracting a service action lambda for deferred class generation.
 * <p>
 * Instances carry all data needed to generate the deploy and action methods
 * for a service into a consolidated class. Two variants exist:
 * <ul>
 * <li>{@link ActionService} — a full transliterated lambda body</li>
 * <li>{@link AliasService} — a trivial forwarder from a recorder proxy key to a service key</li>
 * </ul>
 */
public sealed interface TransliteratedAction {

    /**
     * Get the service key used to store the result in the startup context.
     *
     * @return the service key string
     */
    String serviceKey();

    /**
     * Get whether this action targets the static-init phase.
     *
     * @return {@code true} for static-init, {@code false} for runtime
     */
    boolean staticInit();

    /**
     * Get the ID of the build step that produced this action.
     *
     * @return the step ID (never {@code null})
     */
    String stepId();

    /**
     * A full action service whose lambda body has been extracted and prepared
     * for code generation.
     *
     * @param serviceKey the service key for storing the result
     * @param dependencies the declared dependencies, in order
     * @param beforeKeys service keys that should depend on this service (reverse ordering)
     * @param afterBuildItemClasses build item class names whose producing steps must complete first
     * @param async {@code true} if this is an async action
     * @param staticInit {@code true} if this is a static-init service
     * @param actionMethodType the method descriptor after removing captured parameters
     * @param originalCode the parsed lambda body code model
     * @param captureSlotCount the number of leading local variable slots consumed by captures
     * @param slotToCaptureIndex mapping from slot index to capture argument index ({@code -1} for non-start slots)
     * @param capturedEmitters the resolved emitters for captured arguments
     * @param enclosingClassInternal the internal name of the lambda's enclosing class
     * @param enclosingClassModel the parsed class model of the enclosing class (for inner method lookup)
     * @param stepId the producing build step's ID
     */
    record ActionService(
            String serviceKey,
            List<Dependency> dependencies,
            List<String> beforeKeys,
            List<String> afterBuildItemClasses,
            boolean async,
            boolean staticInit,
            MethodTypeDesc actionMethodType,
            CodeModel originalCode,
            int captureSlotCount,
            int[] slotToCaptureIndex,
            CaptureEmitter[] capturedEmitters,
            String enclosingClassInternal,
            ClassModel enclosingClassModel,
            String stepId) implements TransliteratedAction {
    }

    /**
     * An alias service that copies a recorder-produced value from its recorder
     * proxy key to a service key in the startup context.
     *
     * @param serviceKey the service key to store the value under
     * @param staticInit {@code true} if this is a static-init alias
     * @param recorderProxyKey the key under which the recorder stored the value
     * @param stepId the producing build step's ID
     */
    record AliasService(
            String serviceKey,
            boolean staticInit,
            String recorderProxyKey,
            String stepId) implements TransliteratedAction {
    }

    /**
     * A wrapper action that reads a service value from the startup context,
     * wraps it in a {@link io.quarkus.runtime.RuntimeValue}, and stores
     * the wrapper under a separate key.
     * <p>
     * This enables legacy {@code @Record}-based build steps to consume
     * service values via {@code RuntimeValue<T>} during incremental migration.
     *
     * @param sourceServiceKey the service key to read the bare value from
     * @param staticInit {@code true} if this targets the static-init phase
     * @param rvKey the key under which the {@code RuntimeValue} wrapper is stored
     * @param stepId the producing build step's ID
     */
    record RuntimeValueWrapper(
            String sourceServiceKey,
            boolean staticInit,
            String rvKey,
            String stepId) implements TransliteratedAction {

        @Override
        public String serviceKey() {
            return rvKey;
        }
    }
}
