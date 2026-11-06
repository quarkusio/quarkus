package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jdbc.oracle.runtime.OracleInitRecorder;

public final class ExtendedCharactersSupport {

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void preinitializeCharacterSets(NativeConfig config, ServiceRegistrar serviceRegistrar) {
        boolean addAllCharsets = config.addAllCharsets();
        serviceRegistrar
                .forService("io.quarkus.jdbc.oracle.preinitialize-charsets")
                .atPhase(Phase.STATIC_INIT)
                .onStart(ctx -> OracleInitRecorder.setupCharSets(addAllCharsets));
    }

}
