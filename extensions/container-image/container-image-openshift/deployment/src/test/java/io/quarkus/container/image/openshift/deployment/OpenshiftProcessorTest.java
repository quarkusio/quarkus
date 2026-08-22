package io.quarkus.container.image.openshift.deployment;

import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.artifactImageReference;
import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.concatUnixPaths;
import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.containerArtifactResult;
import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.observedImageReference;
import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.selectObservedImageReference;
import static io.quarkus.container.image.openshift.deployment.OpenshiftProcessor.waitForOpenshiftBuild;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import io.fabric8.openshift.api.model.BuildBuilder;

public class OpenshiftProcessorTest {
    @Test
    public void concatUnixPathsTest() {
        assertEquals("foo/bar", concatUnixPaths("foo", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/bar/"));

        assertEquals("foo/bar", concatUnixPaths("foo", "/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "bar"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "/bar"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo/", "/", "/bar/"));
        assertEquals("foo/bar", concatUnixPaths("foo", "/", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "/bar"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/", "/bar/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/", "/bar/"));

        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo/", "/bar/", "/"));
        assertEquals("/foo/bar", concatUnixPaths("/foo", "/bar/", "/"));
    }

    @Test
    public void containerArtifactResultsExposeTheDeployableImageForJarAndNativeBuilds() {
        String image = "registry.example/team/application@sha256:0123456789";

        var jarResult = containerArtifactResult("jar-container", image);
        var nativeResult = containerArtifactResult("native-container", image);

        assertEquals("jar-container", jarResult.getType());
        assertEquals(Map.of("container-image", image), jarResult.getMetadata());
        assertEquals("native-container", nativeResult.getType());
        assertEquals(Map.of("container-image", image), nativeResult.getMetadata());
    }

    @Test
    public void completedBuildExposesProviderObservedOutputImageReference() {
        var build = new BuildBuilder()
                .withNewStatus()
                .withOutputDockerImageReference("image-registry.openshift-image-registry.svc:5000/team/application@sha256:abc")
                .endStatus()
                .build();

        assertEquals(
                Optional.of("image-registry.openshift-image-registry.svc:5000/team/application@sha256:abc"),
                observedImageReference(build));
        assertEquals(Optional.empty(), observedImageReference(new BuildBuilder().build()));
        assertEquals(Optional.empty(),
                observedImageReference(new BuildBuilder().withNewStatus().withOutputDockerImageReference(" ").endStatus()
                        .build()));
    }

    @Test
    public void completedBuildResultFlowsToArtifactWithExplicitFallback() {
        String observed = "registry.example/team/application@sha256:abc";
        String configured = "registry.example/team/application:latest";
        var completed = new BuildBuilder()
                .withNewStatus()
                .withPhase("Complete")
                .withOutputDockerImageReference(observed)
                .endStatus()
                .build();

        var returnedBuild = waitForOpenshiftBuild(completed, null, null);
        String effectiveObserved = artifactImageReference(observedImageReference(returnedBuild), configured);
        String effectiveFallback = artifactImageReference(Optional.empty(), configured);

        assertEquals(Map.of("container-image", observed),
                containerArtifactResult("jar-container", effectiveObserved).getMetadata());
        assertEquals(Map.of("container-image", configured),
                containerArtifactResult("native-container", effectiveFallback).getMetadata());
    }

    @Test
    public void firstProviderObservedReferenceWinsAcrossMultipleBuildConfigs() {
        Optional<String> first = Optional.of("registry.example/team/first@sha256:abc");
        Optional<String> second = Optional.of("registry.example/team/second@sha256:def");

        assertEquals(first, selectObservedImageReference(Optional.empty(), first));
        assertEquals(first, selectObservedImageReference(first, Optional.empty()));
        assertEquals(first, selectObservedImageReference(first, second));
    }
}
