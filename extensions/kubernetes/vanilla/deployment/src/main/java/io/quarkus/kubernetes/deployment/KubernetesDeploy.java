
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOY;

import java.util.Optional;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.VersionInfo;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;

public class KubernetesDeploy {

    public static KubernetesDeploy INSTANCE = new KubernetesDeploy();

    private static final Logger log = Logger.getLogger(KubernetesDeploy.class);
    private static boolean serverFound = false;

    private KubernetesDeploy() {
    }

    /**
     * @return {@code true} iff @{code quarkus.kubernetes.deploy=true} AND the target Kubernetes API server is reachable
     *
     *         It follows that this can only return {@code false} if the if @{code quarkus.kubernetes.deploy=false}
     *
     * @throws RuntimeException if communication to the Kubernetes API server errored
     */
    public boolean check() {
        Result result = doCheck();

        if (result.getException().isPresent()) {
            throw result.getException().get();
        }

        return result.isAllowed();
    }

    /**
     * @return {@code true} iff @{code quarkus.kubernetes.deploy=true} AND the target Kubernetes API server is reachable
     *
     *         Never throws an exception even in the face of a communication error with the API server, just returns
     *         {@code false}
     *         in that case
     */
    public boolean checkSilently() {
        return doCheck().isAllowed();
    }

    private Result doCheck() {
        Config config = ConfigProvider.getConfig();
        if (!config.getOptionalValue(DEPLOY, Boolean.class).orElse(false)) {
            return Result.notConfigured();
        }

        // No need to perform the check multiple times.
        if (serverFound) {
            return Result.enabled();
        }

        KubernetesClient client = KubernetesClientUtils.createClient();
        String masterURL = client.getConfiguration().getMasterUrl();
        try {
            //Let's check if we can connect.
            VersionInfo version = client.getVersion();
            if (version == null) {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because the version of the API Server at '"
                                + masterURL + "' could not be determined. Please ensure that a valid token is being used."));
            }

            log.info("Kubernetes API Server at '" + masterURL + "' successfully contacted.");
            log.debugf("Kubernetes Version: %s.%s", version.getMajor(), version.getMinor());
            serverFound = true;
            return Result.enabled();
        } catch (Exception e) {
            if (e.getCause() instanceof SSLHandshakeException) {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because the API Server (at '"
                                + masterURL
                                + "') certificates are not trusted. The certificates can be configured using the relevant configuration propertiers under the 'quarkus.kubernetes-client' config root, or \"quarkus.kubernetes-client.trust-certs=true\" can be set to explicitly trust the certificates (not recommended)",
                        e));
            } else {
                return Result.exceptional(new RuntimeException(
                        "Although a Kubernetes deployment was requested, it however cannot take place because there was an error during communication with the API Server at '"
                                + masterURL + "'",
                        e));
            }
        } finally {
            client.close();
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
