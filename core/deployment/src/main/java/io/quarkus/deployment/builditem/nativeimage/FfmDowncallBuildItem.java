package io.quarkus.deployment.builditem.nativeimage;

/**
 * Used to register a downcall signature for FFI/FFM runtime access.
 */
public final class FfmDowncallBuildItem extends FfmCallBuildItem {
    public FfmDowncallBuildItem(FfmType returnType, FfmType... parameterTypes) {
        super(returnType, parameterTypes);
    }
}
