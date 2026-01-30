package io.quarkus.apicurio.registry.graal;

import com.microsoft.kiota.RequestAdapter;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.apicurio.registry.client.common.RegistryClientOptions;
import io.quarkus.arc.Arc;
import io.vertx.core.Vertx;

@TargetClass(className = "io.apicurio.registry.client.common.DefaultVertxInstance$Holder")
final class Target_io_apicurio_registry_client_DefaultVertxInstance_Holder {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias)
    private static Vertx INSTANCE = null;
}

@TargetClass(className = "io.apicurio.registry.client.common.RegistryClientRequestAdapterFactory")
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

    @Substitute
    private static RequestAdapter createJdkAdapter(RegistryClientOptions options) {
        // JDK HTTP adapter is not supported in native mode because kiota-http-jdk is excluded.
        // Use Vertx adapter instead by setting the adapter type in RegistryClientOptions.
        throw new UnsupportedOperationException(
                "JDK HTTP adapter is not available in native mode. Please use the Vertx adapter by setting " +
                        "the HTTP adapter type to VERTX in your RegistryClientOptions or configuration.");
    }
}
