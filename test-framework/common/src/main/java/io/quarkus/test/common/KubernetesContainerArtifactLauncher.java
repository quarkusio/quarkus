package io.quarkus.test.common;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.OptionalInt;

public interface KubernetesContainerArtifactLauncher
        extends ArtifactLauncher<KubernetesContainerArtifactLauncher.KubernetesInitContext> {

    interface KubernetesInitContext extends InitContext {

        /**
         * Path to the generated deployment manifest, e.g. {@code target/kubernetes/kubernetes.yml}
         * or {@code target/kubernetes/openshift.yml}.
         */
        Path manifestPath();

        /**
         * Either {@code kubernetes} or {@code openshift}.
         */
        String deploymentTarget();

        /**
         * The local image built for this run, as recorded in {@code metadata.container-image}.
         */
        String containerImage();

        /**
         * The registry the local image is retagged and pushed to before being deployed. Deliberately
         * independent of {@code quarkus.container-image.registry}.
         */
        String testRegistry();

        /**
         * The tag to push the retagged image under. Empty means a random per-run tag is generated, so
         * concurrent runs don't collide; set explicitly to reuse (and overwrite) the same tag across runs
         * instead of accumulating a new uniquely-tagged image in the registry every time.
         */
        Optional<String> testTag();

        /**
         * Whether {@link #testRegistry()} is insecure (plain HTTP, or HTTPS with an untrusted certificate). Only
         * applies when pushing with Podman.
         */
        boolean insecureRegistry();

        /**
         * Namespace to deploy to. Empty means the current kubeconfig context's namespace, or {@code default} if
         * the context doesn't specify one.
         */
        Optional<String> namespace();

        /**
         * Either {@code external} or {@code port-forward}. Whether an OpenShift {@code Route} or a Kubernetes
         * {@code Ingress} is actually used for {@code external} follows from {@link #deploymentTarget()}.
         */
        String exposure();

        /**
         * The port to reach the Route/Ingress host on, for {@code external} exposure. Empty means 443 if the
         * Route/Ingress uses TLS, 80 otherwise.
         */
        OptionalInt externalPort();

        /**
         * Whether the deployed resources are deleted after the test run.
         */
        boolean deleteAfterTest();

        /**
         * How long to wait for the deployment to become ready.
         */
        Duration waitTimeout();
    }
}
