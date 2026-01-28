package io.quarkus.apicurio.registry.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;

@TargetClass(className = "io.apicurio.registry.client.DefaultVertxInstance$Holder")
final class Target_io_apicurio_registry_client_DefaultVertxInstance_Holder {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Vertx INSTANCE = null;
}

@TargetClass(className = "io.apicurio.registry.client.RegistryClientRequestAdapterFactory")
final class Target_RegistryClientRequestAdapterFactory {

    @Substitute
    static Vertx getVertxFromCDI(String CDIClassName, String InstanceClassName) {
        try {
            return Arc.container().instance(Vertx.class).get();
        } catch (Throwable t) {
            // Log and ignore
            return null;
        }
    }
}
