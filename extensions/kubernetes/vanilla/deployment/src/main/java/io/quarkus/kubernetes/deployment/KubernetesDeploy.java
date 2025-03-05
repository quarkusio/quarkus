
package io.quarkus.kubernetes.deployment;

import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.kubernetes.client.spi.KubernetesClientBuildItem;

public class KubernetesDeploy {

    public static KubernetesDeploy INSTANCE = new KubernetesDeploy();

    private static final Logger log = Logger.getLogger(KubernetesDeploy.class);
    private static boolean serverFound = false;

    private KubernetesDeploy() {
    }

    /**
     * @return {@code true} if {@code quarkus.kubernetes.deploy=true} AND the target Kubernetes API server is reachable,
     *         {@code false} otherwise
     *
     * @throws RuntimeException if there was an error while communicating with the Kubernetes API server
     */
    public boolean check(KubernetesClientBuildItem clientBuilder) {
        Result result = doCheck(clientBuilder);

        if (result.getException().isPresent()) {
            throw result.getException().get();
        }

        return result.isAllowed();
    }

    /**
     * @return {@code true} if {@code quarkus.kubernetes.deploy=true} AND the target Kubernetes API server is reachable
     *         {@code false} otherwise or if there was an error while communicating with the Kubernetes API server
     */
    public boolean checkSilently(KubernetesClientBuildItem clientBuilder) {
        return doCheck(clientBuilder).isAllowed();
    }

    private Result doCheck(KubernetesClientBuildItem clientBuilder) {
        if (!KubernetesConfigUtil.isDeploymentEnabled()) {
            return Result.notConfigured();
        }

        // No need to perform the check multiple times.
        if (serverFound) {
            return Result.enabled();
        }

        String masterURL = clientBuilder.getConfig().getMasterUrl();
        try (KubernetesClient client = clientBuilder.buildClient()) {
            //Let's check if we can connect.
            VersionInfo version = client.getVersion();
            if (version == null) {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because the version of the API Server at '"
                                + masterURL + "' could not be determined. Please ensure that a valid token is being used."));
            }

            log.debugf("Kubernetes API Server at '" + masterURL + "' successfully contacted.");
            log.debugf("Kubernetes Version: %s.%s", version.getMajor(), version.getMinor());
            serverFound = true;
            return Result.enabled();
        } catch (Exception e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because the API Server (at '"
                                + masterURL
                                + "') certificates are not trusted. The certificates can be configured using the relevant configuration properties under the 'quarkus.kubernetes-client' config root, or \"quarkus.kubernetes-client.trust-certs=true\" can be set to explicitly trust the certificates (not recommended)",
                        e));
            } else {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because there was an error during communication with the API Server at '"
                                + masterURL + "'",
                        e));
            }
        }
    }

    private static class Result {
        private final boolean allowed;
        private final Optional<RuntimeException> exception;

        private Result(boolean allowed, Optional<RuntimeException> exception) {
            this.allowed = allowed;
            this.exception = exception;
        }

        static Result notConfigured() {
            return new Result(false, Optional.empty());
        }

        static Result enabled() {
            return new Result(true, Optional.empty());
        }

        static Result exceptional(RuntimeException e) {
            return new Result(false, Optional.of(e));
        }

        public boolean isAllowed() {
            return allowed;
        }

        public Optional<RuntimeException> getException() {
            return exception;
        }
    }
}
