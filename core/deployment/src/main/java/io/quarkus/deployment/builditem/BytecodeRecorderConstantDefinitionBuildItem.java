package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.recording.BytecodeRecorderImpl;

/**
 * The definition of a constant
 * that can be injected into recorders via their {@code @Inject}-annotated constructor.
 *
 * Compared to simply passing the value to a recorder proxy,
 * this build item allows for injecting values into recorders
 * without introducing new dependencies from build steps
 * that use the recorder to build steps that create the constant value.
 * This can be useful in complex dependency graphs.
 */
public final class BytecodeRecorderConstantDefinitionBuildItem extends MultiBuildItem {

    private final Holder<?> holder;

    public <T> BytecodeRecorderConstantDefinitionBuildItem(Class<T> type, T value) {
        this.holder = new Holder<>(type, value);
    }

    public void register(BytecodeRecorderImpl recorder) {
        holder.register(recorder);
    }

    // Necessary because generics are not allowed on BuildItems.
    private static class Holder<T> {
        private final Class<T> type;
        private final T value;

        public Holder(Class<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        public void register(BytecodeRecorderImpl recorder) {
            recorder.registerConstant(type, value);
        }
    }
}
