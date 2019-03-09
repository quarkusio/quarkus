package io.quarkus.deployment.builditem;

import java.util.function.Consumer;

import org.jboss.builder.item.SimpleBuildItem;

import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.ResultHandle;

public final class MainAfterStartupBuildItem extends SimpleBuildItem {

    private final Consumer<Input> bytecodeCreator;

    public MainAfterStartupBuildItem(Consumer<Input> bytecodeCreator) {
        this.bytecodeCreator = bytecodeCreator;
    }

    public Consumer<Input> getBytecodeCreator() {
        return bytecodeCreator;
    }

    public static class Input {
        private final MethodCreator doStartMethod;
        private final ResultHandle mainArgs;

        public Input(MethodCreator doStartMethod, ResultHandle mainArgs) {
            this.doStartMethod = doStartMethod;
            this.mainArgs = mainArgs;
        }

        public MethodCreator getDoStartMethod() {
            return doStartMethod;
        }

        public ResultHandle getMainArgs() {
            return mainArgs;
        }
    }
}
