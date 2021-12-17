package io.quarkus.jdbc.oracle.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.jdbc.oracle.runtime.OracleInitRecorder;

public final class ExtendedCharactersSupport {

    @Record(STATIC_INIT)
    @BuildStep(onlyIf = NativeBuild.class)
    public void preinitializeCharacterSets(NativeConfig config, OracleInitRecorder recorder) {
        recorder.setupCharSets(config.addAllCharsets);
    }

}
