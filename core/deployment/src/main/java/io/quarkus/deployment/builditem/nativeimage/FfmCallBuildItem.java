package io.quarkus.deployment.builditem.nativeimage;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Used to register an upcall/downcall signature for FFI/FFM runtime access.
 */
public abstract class FfmCallBuildItem extends MultiBuildItem {

    private final String returnType;
    private final List<String> parameterTypes;

    public FfmCallBuildItem(FfmType returnType, FfmType... parameterTypes) {
        this.returnType = requireNonNull(returnType, "returnType cannot be null").getCanonicalName();
        this.parameterTypes = Arrays.stream(parameterTypes).map(FfmType::getCanonicalName).toList();
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final FfmCallBuildItem that = (FfmCallBuildItem) o;
        return returnType.equals(that.returnType) && parameterTypes.equals(that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnType, parameterTypes);
    }
}
