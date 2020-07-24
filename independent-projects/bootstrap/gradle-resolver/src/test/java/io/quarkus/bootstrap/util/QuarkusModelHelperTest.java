package io.quarkus.bootstrap.util;

import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.bootstrap.model.AppDependency;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class QuarkusModelHelperTest {

    @Test
    public void shouldKeepExtensionDependencyVersion() {
        Map<AppArtifactKey, AppDependency> userDependencies = new HashMap<>();

        final AppArtifact appArtifact = new AppArtifact("org.acme", "common", "0.0.1-SNAPSHOT");
        AppDependency extensionDependency = new AppDependency(appArtifact, "runtime", false);

        final AppDependency dependency = QuarkusModelHelper.alignVersion(extensionDependency, userDependencies);

        Assertions.assertEquals(extensionDependency, dependency);
    }

    @Test
    public void shouldUseUserDependencyVersion() {
        Map<AppArtifactKey, AppDependency> userDependencies = new HashMap<>();
        final AppArtifact userAppArtifact = new AppArtifact("org.acme", "common", "1.0.0-SNAPSHOT");
        final AppDependency userDependency = new AppDependency(userAppArtifact, "runtime", false);
        userDependencies.put(new AppArtifactKey("org.acme", "common"), userDependency);

        final AppArtifact appArtifact = new AppArtifact("org.acme", "common", "0.0.1-SNAPSHOT");
        AppDependency extensionDependency = new AppDependency(appArtifact, "runtime", false);

        final AppDependency dependency = QuarkusModelHelper.alignVersion(extensionDependency, userDependencies);

        Assertions.assertEquals(userDependency, dependency);
    }
}
