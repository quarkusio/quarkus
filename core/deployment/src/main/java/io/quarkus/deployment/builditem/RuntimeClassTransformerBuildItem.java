package io.quarkus.deployment.builditem;

import io.quarkus.bootstrap.app.ClassTransformer;
import io.quarkus.builder.item.SimpleBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * Carries the bytecode transformation function produced during augmentation,
 * for use by the runtime hot-reload infrastructure (instrumentation-based class redefinition).
 */
public final class RuntimeClassTransformerBuildItem extends SimpleBuildItem {

    private final ClassTransformer transformer;

    public RuntimeClassTransformerBuildItem(ClassTransformer transformer) {
        this.transformer = Assert.checkNotNullParam("transformer", transformer);
    }

    public ClassTransformer getTransformer() {
        return transformer;
    }
}
