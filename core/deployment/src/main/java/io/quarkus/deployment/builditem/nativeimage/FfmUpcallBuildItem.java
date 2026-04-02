package io.quarkus.deployment.builditem.nativeimage;

/**
 * Used to register an upcall signature for FFI/FFM runtime access.
 */
public final class FfmUpcallBuildItem extends FfmCallBuildItem {
    public FfmUpcallBuildItem(FfmType returnType, FfmType... parameterTypes) {
        super(returnType, parameterTypes);
    }
}
