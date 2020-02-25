
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOY;

import java.util.function.BooleanSupplier;

import javax.net.ssl.SSLHandshakeException;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;

public class KubernetesDeploy implements BooleanSupplier {

    private final Logger LOGGER = Logger.getLogger(KubernetesDeploy.class);
    private static boolean serverFound = false;
    private static boolean alreadyWarned = false;

    @Override
    public boolean getAsBoolean() {
        Config config = ConfigProvider.getConfig();
        if (!config.getOptionalValue(DEPLOY, Boolean.class).orElse(false)) {
            return false;
        }

        // No need to perform the check multiple times.
        if (serverFound) {
            return true;
        }
        try (final KubernetesClient client = KubernetesClientUtils.createClient()) {
            //Let's check id we can connect.
            RootPaths paths = client.rootPaths();
            LOGGER.info("Found kubernetes server.");
            serverFound = true;
            return true;
        } catch (Exception e) {
            if (!alreadyWarned) {
                if (e.getCause() instanceof SSLHandshakeException) {
                    String message = "Although a Kubernetes deployment was requested, it will however not take place because the API Server certificates are not trusted. The certificates can be configured using the relevant configuration propertiers under the 'quarkus.kubernetes-client' config root, or \"quarkus.kubernetes-client.trust-certs=true\" can be set to explicitly trust the certificates (not recommended)";
                    LOGGER.warn(message);
                } else {
                    LOGGER.error(
                            "Although a Kubernetes deployment was requested, it will however not take place because there was an error during communication with the API Server: "
                                    + e.getMessage());
                }
                alreadyWarned = true;
            }
            return false;
        }
    }
}
