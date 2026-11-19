package io.quarkus.security.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.quarkus.bootstrap.model.ApplicationModelBuilder;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM;
import io.quarkus.deployment.pkg.steps.NativeImageBuildRunnerError;
import io.quarkus.maven.dependency.ResolvedDependencyBuilder;
import io.smallrye.config.SmallRyeConfigBuilder;

class BouncyCastleMlKemReflectionTest {

    @Test
    void reuseExistingDoesNotInvokeBuilder() {
        var runner = new NativeImageBuildRunnerError("Builder must not be invoked when reusing an executable") {
            @Override
            public void setup(boolean processInheritIODisabled) {
                throw new AssertionError("Builder must not be set up when reusing an executable");
            }

            @Override
            public GraalVM.Version getGraalVMVersion() {
                throw new AssertionError("Builder version must not be queried when reusing an executable");
            }
        };
        var reflection = new ArrayList<ReflectiveClassBuildItem>();

        new SecurityProcessor().registerBouncyCastleMlKemReflection(applicationWithTransitiveBouncyCastle(),
                nativeConfig(true), Optional.of(new NativeImageRunnerBuildItem(runner)),
                List.of(new BouncyCastleProviderBuildItem()), List.of(), reflection::add);

        assertThat(reflection).hasSize(1);
        assertThat(reflection.get(0).getClassNames()).hasSize(13)
                .contains("org.bouncycastle.jcajce.provider.asymmetric.mlkem.MLKEMSpi$MLKEM768");
    }

    @Test
    void transitiveBouncyCastleWithoutConfiguredProviderDoesNotRegisterMlKem() {
        var reflection = new ArrayList<ReflectiveClassBuildItem>();

        new SecurityProcessor().registerBouncyCastleMlKemReflection(applicationWithTransitiveBouncyCastle(),
                nativeConfig(false), Optional.empty(), List.of(), List.of(), reflection::add);

        assertThat(reflection).isEmpty();
    }

    private static NativeConfig nativeConfig(boolean reuseExisting) {
        return new SmallRyeConfigBuilder()
                .withMapping(NativeConfig.class)
                .withDefaultValue("quarkus.native.reuse-existing", Boolean.toString(reuseExisting))
                .build().getConfigMapping(NativeConfig.class);
    }

    private static CurateOutcomeBuildItem applicationWithTransitiveBouncyCastle() {
        return new CurateOutcomeBuildItem(new ApplicationModelBuilder()
                .setAppArtifact(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.acme").setArtifactId("app").setVersion("1.0"))
                .addDependency(ResolvedDependencyBuilder.newInstance()
                        .setGroupId("org.bouncycastle").setArtifactId("bcprov-jdk18on").setVersion("1.83")
                        .setRuntimeCp().setDeploymentCp())
                .build());
    }
}
