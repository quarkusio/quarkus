package io.quarkus.gradle.application.internal.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.services.BuildServiceParameters;
import org.junit.jupiter.api.Test;

class ImageReferenceClaimServiceTest {

    private final ImageReferenceClaimService service = new ImageReferenceClaimService() {
        @Override
        public BuildServiceParameters.None getParameters() {
            return null;
        }
    };

    @Test
    void acceptsRepeatedClaimsFromOneLogicalOwner() {
        assertThatCode(() -> {
            service.claim(owner("app", ImageReferenceOwner.Flavor.NORMAL),
                    List.of("quay.io/acme/app:1", "quay.io/acme/app:latest"));
            service.claim(owner("app", ImageReferenceOwner.Flavor.NORMAL),
                    List.of("quay.io/acme/app:1", "quay.io/acme/app:latest"));
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsPrimaryAndAdditionalCollisionsAcrossOwners() {
        service.claim(owner("first", ImageReferenceOwner.Flavor.NORMAL),
                List.of("quay.io/acme/first:1", "quay.io/acme/shared:latest"));

        assertThatThrownBy(() -> service.claim(
                owner("second", ImageReferenceOwner.Flavor.NORMAL),
                List.of("quay.io/acme/shared:latest")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("quay.io/acme/shared:latest")
                .hasMessageContaining("named build 'first'")
                .hasMessageContaining("named build 'second'")
                .hasMessageContaining("Configure distinct image references/tags");
    }

    @Test
    void collisionDiagnosticDoesNotDependOnClaimOrder() {
        String firstThenSecond = collisionMessage("first", "second");
        String secondThenFirst = collisionMessage("second", "first");

        assertThat(firstThenSecond).isEqualTo(secondThenFirst);
    }

    @Test
    void treatsStartupOptimizedAsADistinctOwner() {
        service.claim(owner("app", ImageReferenceOwner.Flavor.NORMAL), List.of("app:1"));

        assertThatThrownBy(() -> service.claim(
                owner("app", ImageReferenceOwner.Flavor.STARTUP_OPTIMIZED), List.of("app:1")))
                .isInstanceOf(GradleException.class)
                .hasMessageContaining("normal image")
                .hasMessageContaining("startup-optimized image");
    }

    private static ImageReferenceOwner owner(String build, ImageReferenceOwner.Flavor flavor) {
        return new ImageReferenceOwner(":", build, flavor);
    }

    private static String collisionMessage(String firstBuild, String secondBuild) {
        ImageReferenceClaimService claims = new ImageReferenceClaimService() {
            @Override
            public BuildServiceParameters.None getParameters() {
                return null;
            }
        };
        claims.claim(owner(firstBuild, ImageReferenceOwner.Flavor.NORMAL), List.of("app:1"));
        try {
            claims.claim(owner(secondBuild, ImageReferenceOwner.Flavor.NORMAL), List.of("app:1"));
            throw new AssertionError("Expected a container image reference collision");
        } catch (GradleException expected) {
            return expected.getMessage();
        }
    }
}
