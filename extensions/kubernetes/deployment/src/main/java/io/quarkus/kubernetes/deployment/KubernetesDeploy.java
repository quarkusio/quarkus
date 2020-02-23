
package io.quarkus.kubernetes.deployment;

import static io.quarkus.kubernetes.deployment.Constants.DEPLOY;

import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.fabric8.kubernetes.api.model.RootPaths;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.kubernetes.client.runtime.KubernetesClientUtils;

public class KubernetesDeploy implements BooleanSupplier {

    private final Logger LOGGER = Logger.getLogger(KubernetesDeploy.class);
    private static boolean serverFound = false;

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
        final KubernetesClient client = KubernetesClientUtils.createClient();
        try {
            //Let's check id we can connect.
            RootPaths paths = client.rootPaths();
            LOGGER.info("Found kubernetes server.");
            serverFound = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
